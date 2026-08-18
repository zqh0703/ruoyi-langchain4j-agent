package org.dromara.agent.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.action.AgentActionException;
import org.dromara.agent.action.AgentActionHandler;
import org.dromara.agent.action.AgentActionPreparation;
import org.dromara.agent.action.AgentActionPreview;
import org.dromara.agent.action.AgentActionRegistry;
import org.dromara.agent.action.AgentActionStatus;
import org.dromara.agent.action.AgentActionViewAssembler;
import org.dromara.agent.action.AgentInvocationContext;
import org.dromara.agent.domain.AgentActionRequest;
import org.dromara.agent.domain.vo.AgentActionExecutionVo;
import org.dromara.agent.domain.vo.AgentActionVo;
import org.dromara.agent.mapper.AgentActionRequestMapper;
import org.dromara.agent.tool.AgentToolGuard;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Prepares, confirms, cancels, and queries two-phase Agent actions.
 */
@Service
@RequiredArgsConstructor
public class AgentActionService {

    private static final Duration CONFIRMATION_TTL = Duration.ofMinutes(10);
    private static final Duration EXECUTION_TIMEOUT = Duration.ofMinutes(5);

    private final AgentActionRequestMapper actionMapper;
    private final AgentActionRegistry actionRegistry;
    private final AgentInvocationContext invocationContext;
    private final AgentToolGuard toolGuard;
    private final AgentActionViewAssembler viewAssembler;
    private final AgentActionExecutionService executionService;

    public AgentActionPreparation prepare(String toolCode, Object command) {
        AgentInvocationContext.Context context = invocationContext.require();
        AgentActionHandler<?> handler = actionRegistry.require(toolCode);
        toolGuard.check(handler.requiredPermissions());
        AgentActionPreview preview = handler.previewObject(command);
        if (!preview.actionRequired()) {
            return new AgentActionPreparation("NO_CHANGE", preview.summary(), null, preview.preview());
        }

        String argumentsJson = JsonUtils.toJsonString(command);
        String requestKey = sha256(context.runLogId() + "|" + toolCode + "|" + argumentsJson);
        AgentActionRequest existing = actionMapper.selectOne(
            Wrappers.lambdaQuery(AgentActionRequest.class)
                .eq(AgentActionRequest::getRunLogId, context.runLogId())
                .eq(AgentActionRequest::getRequestKey, requestKey)
                .eq(AgentActionRequest::getCreateBy, LoginHelper.getUserId())
                .last("limit 1")
        );
        if (existing != null) {
            refreshTerminalState(existing);
            return new AgentActionPreparation(
                "ACTION_CONFIRMATION_REQUIRED", "This action requires explicit user confirmation.",
                viewAssembler.toVo(actionMapper.selectById(existing.getId())), preview.preview()
            );
        }

        Date now = new Date();
        AgentActionRequest action = new AgentActionRequest();
        action.setSessionId(context.sessionId());
        action.setAgentId(context.agentId());
        action.setRunLogId(context.runLogId());
        action.setToolCode(toolCode);
        action.setRiskLevel(handler.riskLevel().name());
        action.setStatus(AgentActionStatus.PENDING_CONFIRMATION.name());
        action.setRequestKey(requestKey);
        action.setArgumentsJson(argumentsJson);
        action.setPreviewJson(JsonUtils.toJsonString(preview));
        action.setSummary(limit(preview.summary(), 1000));
        action.setExpiresAt(new Date(now.getTime() + CONFIRMATION_TTL.toMillis()));
        action.setVersion(0);
        action.setTenantId(LoginHelper.getTenantId());
        action.setCreateBy(LoginHelper.getUserId());
        action.setCreateDept(LoginHelper.getDeptId());
        action.setCreateTime(now);
        if (actionMapper.insert(action) != 1) {
            throw new AgentActionException("PREPARE_FAILED", "Failed to persist the action proposal");
        }
        return new AgentActionPreparation(
            "ACTION_CONFIRMATION_REQUIRED", "This action requires explicit user confirmation.",
            viewAssembler.toVo(action), preview.preview()
        );
    }

    public void linkToolMessage(Long actionId, Long toolMessageId) {
        if (actionId == null || toolMessageId == null) {
            return;
        }
        actionMapper.update(null, Wrappers.lambdaUpdate(AgentActionRequest.class)
            .set(AgentActionRequest::getToolMessageId, toolMessageId)
            .eq(AgentActionRequest::getId, actionId)
            .eq(AgentActionRequest::getCreateBy, LoginHelper.getUserId()));
    }

    public List<AgentActionVo> listBySession(Long sessionId) {
        refreshStaleStates();
        return actionMapper.selectList(Wrappers.lambdaQuery(AgentActionRequest.class)
                .eq(AgentActionRequest::getSessionId, sessionId)
                .eq(AgentActionRequest::getCreateBy, LoginHelper.getUserId())
                .orderByAsc(AgentActionRequest::getCreateTime))
            .stream().map(viewAssembler::toVo).toList();
    }

    public AgentActionVo get(Long id) {
        AgentActionRequest action = requireOwned(id);
        refreshTerminalState(action);
        return viewAssembler.toVo(actionMapper.selectById(id));
    }

    public AgentActionExecutionVo confirm(Long id, Integer version) {
        refreshStaleStates();
        AgentActionRequest action = requireOwned(id);
        requireVersion(action, version);
        if (!AgentActionStatus.PENDING_CONFIRMATION.name().equals(action.getStatus())) {
            throw new AgentActionException("INVALID_STATE", "Only pending actions can be confirmed");
        }
        if (!action.getExpiresAt().after(new Date())) {
            expire(action);
            throw new AgentActionException("ACTION_EXPIRED", "The action confirmation has expired");
        }
        AgentActionHandler<?> handler = actionRegistry.require(action.getToolCode());
        toolGuard.check(handler.requiredPermissions());

        Date now = new Date();
        int claimed = actionMapper.update(null, Wrappers.lambdaUpdate(AgentActionRequest.class)
            .set(AgentActionRequest::getStatus, AgentActionStatus.EXECUTING.name())
            .set(AgentActionRequest::getConfirmedBy, LoginHelper.getUserId())
            .set(AgentActionRequest::getConfirmedTime, now)
            .set(AgentActionRequest::getStartedTime, now)
            .set(AgentActionRequest::getVersion, version + 1)
            .eq(AgentActionRequest::getId, id)
            .eq(AgentActionRequest::getCreateBy, LoginHelper.getUserId())
            .eq(AgentActionRequest::getStatus, AgentActionStatus.PENDING_CONFIRMATION.name())
            .eq(AgentActionRequest::getVersion, version));
        if (claimed != 1) {
            throw new AgentActionException("VERSION_CONFLICT", "The action was already changed or confirmed");
        }

        try {
            return executionService.execute(id);
        } catch (RuntimeException error) {
            markFailed(id, error);
            if (error instanceof AgentActionException actionError) {
                throw actionError;
            }
            throw new ServiceException("Action execution failed: " + safeMessage(error));
        }
    }

    public AgentActionVo cancel(Long id, Integer version) {
        refreshStaleStates();
        AgentActionRequest action = requireOwned(id);
        requireVersion(action, version);
        if (!AgentActionStatus.PENDING_CONFIRMATION.name().equals(action.getStatus())) {
            throw new AgentActionException("INVALID_STATE", "Only pending actions can be cancelled");
        }
        Date now = new Date();
        int changed = actionMapper.update(null, Wrappers.lambdaUpdate(AgentActionRequest.class)
            .set(AgentActionRequest::getStatus, AgentActionStatus.CANCELLED.name())
            .set(AgentActionRequest::getFinishedTime, now)
            .set(AgentActionRequest::getVersion, version + 1)
            .eq(AgentActionRequest::getId, id)
            .eq(AgentActionRequest::getCreateBy, LoginHelper.getUserId())
            .eq(AgentActionRequest::getStatus, AgentActionStatus.PENDING_CONFIRMATION.name())
            .eq(AgentActionRequest::getVersion, version));
        if (changed != 1) {
            throw new AgentActionException("VERSION_CONFLICT", "The action was already changed");
        }
        return viewAssembler.toVo(actionMapper.selectById(id));
    }

    public List<String> recentSuccessfulSummaries(Long sessionId) {
        return actionMapper.selectList(Wrappers.lambdaQuery(AgentActionRequest.class)
                .select(AgentActionRequest::getSummary)
                .eq(AgentActionRequest::getSessionId, sessionId)
                .eq(AgentActionRequest::getCreateBy, LoginHelper.getUserId())
                .eq(AgentActionRequest::getStatus, AgentActionStatus.SUCCESS.name())
                .orderByDesc(AgentActionRequest::getFinishedTime)
                .last("limit 10"))
            .stream().map(AgentActionRequest::getSummary).filter(Objects::nonNull).toList();
    }

    private AgentActionRequest requireOwned(Long id) {
        AgentActionRequest action = actionMapper.selectById(id);
        if (action == null || !Objects.equals(action.getCreateBy(), LoginHelper.getUserId())) {
            throw new AgentActionException("NOT_FOUND", "Action does not exist or is not owned by the current user");
        }
        return action;
    }

    private void requireVersion(AgentActionRequest action, Integer version) {
        if (!Objects.equals(action.getVersion(), version)) {
            throw new AgentActionException("VERSION_CONFLICT", "The action version is stale; refresh and try again");
        }
    }

    private void refreshStaleStates() {
        Date now = new Date();
        actionMapper.update(null, Wrappers.lambdaUpdate(AgentActionRequest.class)
            .set(AgentActionRequest::getStatus, AgentActionStatus.EXPIRED.name())
            .set(AgentActionRequest::getFinishedTime, now)
            .setSql("version = version + 1")
            .eq(AgentActionRequest::getCreateBy, LoginHelper.getUserId())
            .eq(AgentActionRequest::getStatus, AgentActionStatus.PENDING_CONFIRMATION.name())
            .le(AgentActionRequest::getExpiresAt, now));
        Date interruptedBefore = new Date(now.getTime() - EXECUTION_TIMEOUT.toMillis());
        actionMapper.update(null, Wrappers.lambdaUpdate(AgentActionRequest.class)
            .set(AgentActionRequest::getStatus, AgentActionStatus.FAILED.name())
            .set(AgentActionRequest::getErrorCode, "EXECUTION_INTERRUPTED")
            .set(AgentActionRequest::getErrorMessage, "Execution did not complete within five minutes")
            .set(AgentActionRequest::getFinishedTime, now)
            .setSql("version = version + 1")
            .eq(AgentActionRequest::getCreateBy, LoginHelper.getUserId())
            .eq(AgentActionRequest::getStatus, AgentActionStatus.EXECUTING.name())
            .le(AgentActionRequest::getStartedTime, interruptedBefore));
    }

    private void refreshTerminalState(AgentActionRequest action) {
        Date now = new Date();
        if (AgentActionStatus.PENDING_CONFIRMATION.name().equals(action.getStatus())
            && !action.getExpiresAt().after(now)) {
            expire(action);
        } else if (AgentActionStatus.EXECUTING.name().equals(action.getStatus())
            && action.getStartedTime() != null
            && action.getStartedTime().getTime() <= now.getTime() - EXECUTION_TIMEOUT.toMillis()) {
            markFailed(action.getId(), new AgentActionException(
                "EXECUTION_INTERRUPTED", "Execution did not complete within five minutes"
            ));
        }
    }

    private void expire(AgentActionRequest action) {
        actionMapper.update(null, Wrappers.lambdaUpdate(AgentActionRequest.class)
            .set(AgentActionRequest::getStatus, AgentActionStatus.EXPIRED.name())
            .set(AgentActionRequest::getFinishedTime, new Date())
            .set(AgentActionRequest::getVersion, action.getVersion() + 1)
            .eq(AgentActionRequest::getId, action.getId())
            .eq(AgentActionRequest::getStatus, AgentActionStatus.PENDING_CONFIRMATION.name())
            .eq(AgentActionRequest::getVersion, action.getVersion()));
    }

    private void markFailed(Long id, RuntimeException error) {
        AgentActionRequest action = actionMapper.selectById(id);
        if (action == null) {
            return;
        }
        Date now = new Date();
        String code = error instanceof AgentActionException actionError ? actionError.code() : "EXECUTION_FAILED";
        actionMapper.update(null, Wrappers.lambdaUpdate(AgentActionRequest.class)
            .set(AgentActionRequest::getStatus, AgentActionStatus.FAILED.name())
            .set(AgentActionRequest::getErrorCode, code)
            .set(AgentActionRequest::getErrorMessage, limit(safeMessage(error), 2000))
            .set(AgentActionRequest::getFinishedTime, now)
            .set(AgentActionRequest::getDurationMs,
                action.getStartedTime() == null ? 0L : Math.max(0L, now.getTime() - action.getStartedTime().getTime()))
            .set(AgentActionRequest::getVersion, action.getVersion() + 1)
            .eq(AgentActionRequest::getId, id)
            .eq(AgentActionRequest::getStatus, AgentActionStatus.EXECUTING.name())
            .eq(AgentActionRequest::getVersion, action.getVersion()));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private String safeMessage(RuntimeException error) {
        return error.getMessage() == null || error.getMessage().isBlank()
            ? error.getClass().getSimpleName() : error.getMessage();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
