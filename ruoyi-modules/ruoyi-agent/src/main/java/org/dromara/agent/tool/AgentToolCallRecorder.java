package org.dromara.agent.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.agent.domain.AgentMessage;
import org.dromara.agent.mapper.AgentMessageMapper;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Persists synchronous tool executions as role=tool conversation messages.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentToolCallRecorder {

    private final AgentMessageMapper agentMessageMapper;
    private final ThreadLocal<ExecutionContext> contextHolder = new ThreadLocal<>();

    public Scope open(Long sessionId, Long agentId, Long runLogId) {
        ExecutionContext previous = contextHolder.get();
        contextHolder.set(new ExecutionContext(sessionId, agentId, runLogId));
        return () -> {
            if (previous == null) {
                contextHolder.remove();
            } else {
                contextHolder.set(previous);
            }
        };
    }

    public String record(String toolName, Map<String, Object> arguments, Supplier<String> execution) {
        long startedAt = System.currentTimeMillis();
        try {
            String result = execution.get();
            persist(toolName, arguments, result, System.currentTimeMillis() - startedAt, null);
            return result;
        } catch (RuntimeException error) {
            persist(toolName, arguments, null, System.currentTimeMillis() - startedAt, error);
            throw error;
        }
    }

    private void persist(String toolName, Map<String, Object> arguments, String result,
                         long durationMs, RuntimeException error) {
        ExecutionContext context = contextHolder.get();
        if (context == null) {
            log.debug("Skipping tool trace outside an Agent conversation: {}", toolName);
            return;
        }

        try {
            AgentMessage message = new AgentMessage();
            message.setSessionId(context.sessionId());
            message.setAgentId(context.agentId());
            message.setRunLogId(context.runLogId());
            message.setRole("tool");
            message.setContent(error == null
                ? "Tool completed in " + durationMs + " ms."
                : "Tool failed in " + durationMs + " ms: " + safeMessage(error));
            message.setToolName(toolName);
            message.setToolArgs(JsonUtils.toJsonString(arguments));
            message.setToolResult(error == null ? result : safeMessage(error));
            message.setToolStatus(error == null ? "SUCCESS" : "FAILED");
            message.setToolDurationMs(durationMs);
            message.setPromptTokens(0);
            message.setCompletionTokens(0);
            message.setSeq(Math.toIntExact(agentMessageMapper.selectCount(
                Wrappers.lambdaQuery(AgentMessage.class)
                    .eq(AgentMessage::getSessionId, context.sessionId())
            ) + 1));
            message.setTenantId(LoginHelper.getTenantId());
            message.setCreateBy(LoginHelper.getUserId());
            message.setCreateDept(LoginHelper.getDeptId());
            if (agentMessageMapper.insert(message) <= 0) {
                log.error("Failed to persist Agent tool trace: {}", toolName);
            }
        } catch (RuntimeException persistenceError) {
            log.error("Failed to persist Agent tool trace: {}", toolName, persistenceError);
        }
    }

    private String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }

    private record ExecutionContext(Long sessionId, Long agentId, Long runLogId) {
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {

        @Override
        void close();

    }

}
