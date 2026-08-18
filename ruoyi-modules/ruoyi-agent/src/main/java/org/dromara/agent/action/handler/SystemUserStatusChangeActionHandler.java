package org.dromara.agent.action.handler;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.action.AgentActionException;
import org.dromara.agent.action.AgentActionExecutionResult;
import org.dromara.agent.action.AgentActionHandler;
import org.dromara.agent.action.AgentActionPreview;
import org.dromara.agent.action.AgentActionRiskLevel;
import org.dromara.agent.action.AgentActionTargetResolver;
import org.dromara.agent.action.command.SystemUserStatusChangeCommand;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SystemUserStatusChangeActionHandler implements AgentActionHandler<SystemUserStatusChangeCommand> {

    public static final String TOOL_CODE = "system_user_status_change";

    private final AgentActionTargetResolver targetResolver;
    private final ISysUserService userService;

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    @Override
    public AgentActionRiskLevel riskLevel() {
        return AgentActionRiskLevel.HIGH;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:user:edit");
    }

    @Override
    public Class<SystemUserStatusChangeCommand> commandType() {
        return SystemUserStatusChangeCommand.class;
    }

    @Override
    public AgentActionPreview preview(SystemUserStatusChangeCommand command) {
        SysUserVo user = targetResolver.user(command.userId(), command.userName());
        protectTarget(user);
        String targetStatus = normalizeStatus(command.targetStatus());
        if (command.reason() == null || command.reason().isBlank() || command.reason().trim().length() > 200) {
            throw new AgentActionException("INVALID_ARGUMENT", "A reason of at most 200 characters is required");
        }
        if (targetStatus.equals(user.getStatus())) {
            return AgentActionPreview.noChange(
                "User " + user.getUserName() + " is already " + label(targetStatus),
                Map.of("userId", user.getUserId(), "userName", user.getUserName(), "status", label(targetStatus))
            );
        }
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("userId", user.getUserId());
        preview.put("userName", user.getUserName());
        preview.put("nickName", user.getNickName());
        preview.put("currentStatus", label(user.getStatus()));
        preview.put("targetStatus", label(targetStatus));
        preview.put("reason", command.reason().trim());
        Map<String, Object> expected = Map.of("userId", String.valueOf(user.getUserId()), "currentStatus", user.getStatus());
        return AgentActionPreview.required(
            "Change user " + user.getUserName() + " from " + label(user.getStatus()) + " to " + label(targetStatus),
            preview, expected
        );
    }

    @Override
    public AgentActionExecutionResult execute(SystemUserStatusChangeCommand command, AgentActionPreview prepared) {
        SysUserVo user = targetResolver.user(command.userId(), command.userName());
        protectTarget(user);
        if (!Objects.equals(prepared.expectedState().get("userId"), String.valueOf(user.getUserId()))
            || !Objects.equals(prepared.expectedState().get("currentStatus"), user.getStatus())) {
            throw new AgentActionException("STALE_STATE", "The user status changed after the preview was created");
        }
        String targetStatus = normalizeStatus(command.targetStatus());
        if (userService.updateUserStatus(user.getUserId(), targetStatus) != 1) {
            throw new AgentActionException("EXECUTION_FAILED", "The user status could not be updated");
        }
        return AgentActionExecutionResult.success(Map.of(
            "userId", user.getUserId(), "userName", user.getUserName(), "status", label(targetStatus)
        ));
    }

    private void protectTarget(SysUserVo user) {
        if (SystemConstants.SUPER_ADMIN_ID.equals(user.getUserId())) {
            throw new AgentActionException("FORBIDDEN_TARGET", "The super administrator account cannot be changed");
        }
        if (Objects.equals(LoginHelper.getUserId(), user.getUserId())) {
            throw new AgentActionException("FORBIDDEN_TARGET", "You cannot change your own account status");
        }
        userService.checkUserAllowed(user.getUserId());
    }

    private String normalizeStatus(String value) {
        if (value == null) {
            throw new AgentActionException("INVALID_ARGUMENT", "targetStatus must be NORMAL or DISABLED");
        }
        return switch (value.trim().toUpperCase()) {
            case "NORMAL", "0" -> "0";
            case "DISABLED", "1" -> "1";
            default -> throw new AgentActionException("INVALID_ARGUMENT", "targetStatus must be NORMAL or DISABLED");
        };
    }

    private String label(String status) {
        return "0".equals(status) ? "NORMAL" : "DISABLED";
    }
}
