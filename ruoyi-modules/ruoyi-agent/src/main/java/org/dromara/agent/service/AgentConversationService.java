package org.dromara.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.domain.AgentConfig;
import org.dromara.agent.domain.AgentMessage;
import org.dromara.agent.domain.AgentRunLog;
import org.dromara.agent.domain.AgentSession;
import org.dromara.agent.domain.bo.AgentChatSendBo;
import org.dromara.agent.domain.bo.AgentRunLogQueryBo;
import org.dromara.agent.domain.bo.AgentSessionCreateBo;
import org.dromara.agent.domain.vo.AgentChatReplyVo;
import org.dromara.agent.domain.vo.AgentMessageVo;
import org.dromara.agent.domain.vo.AgentRunLogVo;
import org.dromara.agent.domain.vo.AgentRunTraceVo;
import org.dromara.agent.domain.vo.AgentSessionVo;
import org.dromara.agent.domain.vo.AgentToolVo;
import org.dromara.agent.mapper.AgentConfigMapper;
import org.dromara.agent.mapper.AgentMessageMapper;
import org.dromara.agent.mapper.AgentRunLogMapper;
import org.dromara.agent.mapper.AgentSessionMapper;
import org.dromara.agent.provider.DeepSeekChatModelFactory;
import org.dromara.agent.tool.AgentToolRegistry;
import org.dromara.agent.tool.AgentToolCallRecorder;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates persisted conversations with LangChain4j Agent execution.
 */
@Service
@RequiredArgsConstructor
public class AgentConversationService {

    private static final int MEMORY_MAX_MESSAGES = 20;
    private static final String DEFAULT_SYSTEM_PROMPT = "你是后台管理系统中的智能助手。回答应准确、简洁；需要系统统计数据时，可以调用已提供的工具。";
    private static final String DEFAULT_ROLE_PROMPT = """
        Help the user understand, develop, debug, and present the RuoYi-Vue-Plus Agent module.
        Explain unfamiliar LangChain4j concepts step by step and connect them to the current project.
        """;

    private final AgentConfigMapper agentConfigMapper;
    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final AgentRunLogMapper agentRunLogMapper;
    private final DeepSeekChatModelFactory deepSeekChatModelFactory;
    private final AgentToolRegistry agentToolRegistry;
    private final AgentToolCallRecorder agentToolCallRecorder;
    private final AgentActionService agentActionService;

    /**
     * Creates a conversation session for the current user.
     */
    public AgentSessionVo createSession(AgentSessionCreateBo bo) {
        AgentConfig agentConfig = requireEnabledAgent(bo.getAgentId());
        AgentSession session = new AgentSession();
        session.setAgentId(agentConfig.getId());
        session.setTitle(StringUtils.isNotBlank(bo.getTitle()) ? bo.getTitle() : agentConfig.getAgentName() + "的新会话");
        session.setStatus("0");
        session.setLastMessageTime(new Date());
        fillCreator(session);
        if (agentSessionMapper.insert(session) <= 0) {
            throw new ServiceException("创建Agent会话失败");
        }
        return agentSessionMapper.selectVoById(session.getId());
    }

    /**
     * Lists sessions visible to the current user.
     */
    public TableDataInfo<AgentSessionVo> querySessionPage(PageQuery pageQuery) {
        LambdaQueryWrapper<AgentSession> lqw = Wrappers.lambdaQuery();
        if (!LoginHelper.isSuperAdmin()) {
            lqw.eq(AgentSession::getCreateBy, LoginHelper.getUserId());
        }
        lqw.orderByDesc(AgentSession::getLastMessageTime);
        Page<AgentSessionVo> result = agentSessionMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * Lists persisted messages for a session.
     */
    public List<AgentMessageVo> queryMessages(Long sessionId) {
        requireOwnedSession(sessionId);
        return agentMessageMapper.selectVoList(
            Wrappers.lambdaQuery(AgentMessage.class)
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByAsc(AgentMessage::getSeq)
        );
    }

    /**
     * Sends one message, invokes LangChain4j, and persists the full business trace.
     */
    public AgentChatReplyVo send(AgentChatSendBo bo) {
        AgentSession session = requireOwnedSession(bo.getSessionId());
        AgentConfig agentConfig = requireEnabledAgent(session.getAgentId());
        AgentRunLog runLog = createRunLog(session, agentConfig, bo.getMessage());
        long startedAt = System.currentTimeMillis();

        try {
            AgentMessage userMessage = saveMessage(session, "user", bo.getMessage(), runLog.getId());
            AgentAssistant assistant = buildAssistant(agentConfig, userMessage.getId(), session.getId());
            String answer;
            try (AgentToolCallRecorder.Scope ignored = agentToolCallRecorder.open(
                session.getId(), agentConfig.getId(), runLog.getId())) {
                answer = assistant.chat(session.getId(), bo.getMessage());
            }
            AgentMessage assistantMessage = saveMessage(session, "assistant", answer, runLog.getId());
            updateSessionLastMessageTime(session);
            finishRunLog(runLog, answer, startedAt, null);
            return new AgentChatReplyVo(session.getId(), assistantMessage.getId(), runLog.getId(), answer, System.currentTimeMillis() - startedAt);
        } catch (Exception e) {
            finishRunLog(runLog, null, startedAt, e);
            throw e instanceof ServiceException ? (ServiceException) e : new ServiceException("Agent调用失败: " + safeErrorMessage(e));
        }
    }

    /**
     * Queries persisted Agent invocation logs.
     */
    public TableDataInfo<AgentRunLogVo> queryRunLogPage(AgentRunLogQueryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<AgentRunLog> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getAgentId() != null, AgentRunLog::getAgentId, bo.getAgentId());
        lqw.eq(bo.getSessionId() != null, AgentRunLog::getSessionId, bo.getSessionId());
        lqw.eq(StringUtils.isNotBlank(bo.getProvider()), AgentRunLog::getProvider, bo.getProvider());
        lqw.eq(StringUtils.isNotBlank(bo.getModelName()), AgentRunLog::getModelName, bo.getModelName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), AgentRunLog::getStatus, bo.getStatus());
        if (!LoginHelper.isSuperAdmin()) {
            lqw.eq(AgentRunLog::getCreateBy, LoginHelper.getUserId());
        }
        lqw.orderByDesc(AgentRunLog::getCreateTime);
        Page<AgentRunLogVo> result = agentRunLogMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    public AgentRunLogVo queryRunLogById(Long id) {
        AgentRunLog runLog = agentRunLogMapper.selectById(id);
        if (runLog == null || (!LoginHelper.isSuperAdmin() && !Objects.equals(runLog.getCreateBy(), LoginHelper.getUserId()))) {
            throw new ServiceException("执行记录不存在或无权访问");
        }
        return agentRunLogMapper.selectVoById(id);
    }
    public AgentRunTraceVo queryRunTraceById(Long id) {
        AgentRunTraceVo trace = new AgentRunTraceVo();
        trace.setRunLog(queryRunLogById(id));
        trace.setMessages(agentMessageMapper.selectVoList(
            Wrappers.lambdaQuery(AgentMessage.class)
                .eq(AgentMessage::getRunLogId, id)
                .orderByAsc(AgentMessage::getSeq)
        ));
        return trace;
    }


    public List<AgentToolVo> listTools() {
        return agentToolRegistry.listAvailableTools();
    }

    private AgentAssistant buildAssistant(AgentConfig agentConfig, Long currentUserMessageId, Long sessionId) {
        List<Object> enabledTools = "1".equals(agentConfig.getEnableTool())
            ? agentToolRegistry.resolveEnabledTools(agentConfig.getId())
            : List.of();
        var builder = AiServices.builder(AgentAssistant.class)
            .chatModel(deepSeekChatModelFactory.create(agentConfig))
            .chatMemoryProvider(memoryId -> restoreMemory(Long.valueOf(memoryId.toString()), currentUserMessageId))
            .systemMessageProvider(memoryId -> buildSystemPrompt(agentConfig, !enabledTools.isEmpty(), sessionId));
        if (!enabledTools.isEmpty()) {
            builder.tools(enabledTools.toArray());
        }
        return builder.build();
    }

    private String buildSystemPrompt(AgentConfig agentConfig, boolean toolsEnabled, Long sessionId) {
        String rolePrompt = StringUtils.isNotBlank(agentConfig.getSystemPrompt())
            ? agentConfig.getSystemPrompt() : DEFAULT_ROLE_PROMPT;
        String toolInstruction = toolsEnabled
            ? "Business tools are enabled. Read tools return facts immediately. Write tools only create proposals; "
                + "never claim a proposed action succeeded until it appears in confirmed operations. "
                + "Before creating a user or changing roles, use system_role_search when the requested exact role "
                + "is unknown or not found. Present the available roles and ask the user to choose; never substitute "
                + "a privileged role or invent a role ID."
            : "Business tools are disabled. Do not claim that you queried system data or executed a tool.";
        List<String> confirmedActions = agentActionService.recentSuccessfulSummaries(sessionId);
        String confirmedActionContext = confirmedActions.isEmpty()
            ? "- None"
            : confirmedActions.stream().map(summary -> "- " + summary)
                .collect(java.util.stream.Collectors.joining("\n"));
        return """
            You are %s, an AI project assistant.

            Current runtime configuration:
            - Provider: %s
            - Model: %s
            - Business tools: %s

            Response rules:
            - Reply in the user's language; use Chinese by default.
            - Answer directly, accurately, and concisely, while giving step-by-step explanations for technical questions.
            - Use the runtime configuration above when asked about your identity, provider, model, or tool availability.
            - Do not invent project facts, source code, database results, or actions that are not present in the conversation or tool results.
            - Clearly distinguish confirmed facts from suggestions.
            - %s

            Confirmed write operations in this conversation (authoritative, sanitized):
            %s

            Project-specific responsibilities:
            %s
            """.formatted(
            agentConfig.getAgentName(),
            agentConfig.getProvider(),
            agentConfig.getModelName(),
            toolsEnabled ? "enabled" : "disabled",
            toolInstruction,
            confirmedActionContext,
            rolePrompt
        );
    }

    private ChatMemory restoreMemory(Long sessionId, Long excludedMessageId) {
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(MEMORY_MAX_MESSAGES);
        List<AgentMessage> messages = agentMessageMapper.selectList(
            Wrappers.lambdaQuery(AgentMessage.class)
                .eq(AgentMessage::getSessionId, sessionId)
                .ne(excludedMessageId != null, AgentMessage::getId, excludedMessageId)
                .in(AgentMessage::getRole, "user", "assistant")
                .orderByAsc(AgentMessage::getSeq)
        );
        for (AgentMessage message : messages) {
            if ("user".equals(message.getRole())) {
                memory.add(UserMessage.from(message.getContent()));
            } else {
                memory.add(AiMessage.from(message.getContent()));
            }
        }
        return memory;
    }

    private AgentSession requireOwnedSession(Long sessionId) {
        AgentSession session = agentSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new ServiceException("Agent会话不存在");
        }
        if (!LoginHelper.isSuperAdmin() && !Objects.equals(session.getCreateBy(), LoginHelper.getUserId())) {
            throw new ServiceException("无权访问此Agent会话");
        }
        return session;
    }

    private AgentConfig requireEnabledAgent(Long agentId) {
        AgentConfig agentConfig = agentConfigMapper.selectById(agentId);
        if (agentConfig == null || !"0".equals(agentConfig.getStatus())) {
            throw new ServiceException("Agent不存在或已停用");
        }
        return agentConfig;
    }

    private AgentMessage saveMessage(AgentSession session, String role, String content, Long runLogId) {
        AgentMessage message = new AgentMessage();
        message.setSessionId(session.getId());
        message.setAgentId(session.getAgentId());
        message.setRole(role);
        message.setRunLogId(runLogId);
        message.setContent(content);
        message.setPromptTokens(0);
        message.setCompletionTokens(0);
        message.setSeq(Math.toIntExact(agentMessageMapper.selectCount(
            Wrappers.lambdaQuery(AgentMessage.class).eq(AgentMessage::getSessionId, session.getId())
        ) + 1));
        fillCreator(message);
        if (agentMessageMapper.insert(message) <= 0) {
            throw new ServiceException("保存Agent消息失败");
        }
        return message;
    }

    private AgentRunLog createRunLog(AgentSession session, AgentConfig agentConfig, String requestBody) {
        AgentRunLog runLog = new AgentRunLog();
        runLog.setAgentId(agentConfig.getId());
        runLog.setSessionId(session.getId());
        runLog.setProvider(agentConfig.getProvider());
        runLog.setModelName(agentConfig.getModelName());
        runLog.setRequestBody(requestBody);
        runLog.setStatus("1");
        runLog.setDurationMs(0L);
        fillCreator(runLog);
        if (agentRunLogMapper.insert(runLog) <= 0) {
            throw new ServiceException("创建Agent执行记录失败");
        }
        return runLog;
    }

    private void finishRunLog(AgentRunLog runLog, String responseBody, long startedAt, Exception error) {
        runLog.setDurationMs(System.currentTimeMillis() - startedAt);
        runLog.setResponseBody(responseBody);
        runLog.setStatus(error == null ? "0" : "1");
        runLog.setErrorMsg(error == null ? null : safeErrorMessage(error));
        agentRunLogMapper.updateById(runLog);
    }

    private void updateSessionLastMessageTime(AgentSession session) {
        session.setLastMessageTime(new Date());
        agentSessionMapper.updateById(session);
    }

    private void fillCreator(org.dromara.common.tenant.core.TenantEntity entity) {
        entity.setTenantId(LoginHelper.getTenantId());
        entity.setCreateBy(LoginHelper.getUserId());
        entity.setCreateDept(LoginHelper.getDeptId());
    }

    private String safeErrorMessage(Exception error) {
        String message = StringUtils.isNotBlank(error.getMessage()) ? error.getMessage() : error.getClass().getSimpleName();
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }

}
