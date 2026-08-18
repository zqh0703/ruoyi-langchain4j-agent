package org.dromara.agent.action.handler;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.action.AgentActionException;
import org.dromara.agent.action.AgentActionExecutionResult;
import org.dromara.agent.action.AgentActionHandler;
import org.dromara.agent.action.AgentActionPreview;
import org.dromara.agent.action.AgentActionRiskLevel;
import org.dromara.agent.action.AgentActionTargetResolver;
import org.dromara.agent.action.command.SystemUserRoleAssignCommand;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SystemUserRoleAssignActionHandler implements AgentActionHandler<SystemUserRoleAssignCommand> {

    public static final String TOOL_CODE = "system_user_role_assign";

    private final AgentActionTargetResolver targetResolver;
    private final ISysUserService userService;
    private final ISysRoleService roleService;

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
        return List.of("system:user:edit", "system:role:query");
    }

    @Override
    public Class<SystemUserRoleAssignCommand> commandType() {
        return SystemUserRoleAssignCommand.class;
    }

    @Override
    public AgentActionPreview preview(SystemUserRoleAssignCommand command) {
        SysUserVo user = targetResolver.user(command.userId(), command.userName());
        protectTarget(user);
        String operation = normalizeOperation(command.operation());
        List<SysRoleVo> requestedRoles = targetResolver.roles(command.roleIds(), command.roleNames());
        validateRoles(requestedRoles, operation);

        List<Long> currentIds = sorted(roleService.selectRoleListByUserId(user.getUserId()));
        Set<Long> finalIds = new LinkedHashSet<>(currentIds);
        if ("ADD".equals(operation)) {
            requestedRoles.forEach(role -> finalIds.add(role.getRoleId()));
        } else {
            requestedRoles.forEach(role -> finalIds.remove(role.getRoleId()));
        }
        List<Long> normalizedFinalIds = sorted(finalIds);
        if (normalizedFinalIds.equals(currentIds)) {
            return AgentActionPreview.noChange(
                "The requested role assignment does not change user " + user.getUserName(),
                Map.of("userId", user.getUserId(), "userName", user.getUserName(), "operation", operation)
            );
        }
        if (normalizedFinalIds.isEmpty()) {
            throw new AgentActionException("INVALID_ARGUMENT", "A user must retain at least one role");
        }

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("userId", user.getUserId());
        preview.put("userName", user.getUserName());
        preview.put("operation", operation);
        preview.put("roles", requestedRoles.stream().map(this::roleTarget).toList());
        preview.put("currentRoleIds", currentIds);
        preview.put("resultRoleIds", normalizedFinalIds);
        Map<String, Object> expected = Map.of(
            "userId", String.valueOf(user.getUserId()),
            "currentRoleIds", currentIds.stream().map(String::valueOf).toList()
        );
        String verb = "ADD".equals(operation) ? "Add" : "Remove";
        return AgentActionPreview.required(
            verb + " " + requestedRoles.size() + " role(s) "
                + ("ADD".equals(operation) ? "to " : "from ") + "user " + user.getUserName(),
            preview, expected
        );
    }

    @Override
    public AgentActionExecutionResult execute(SystemUserRoleAssignCommand command, AgentActionPreview prepared) {
        SysUserVo user = targetResolver.user(command.userId(), command.userName());
        protectTarget(user);
        List<Long> currentIds = sorted(roleService.selectRoleListByUserId(user.getUserId()));
        List<String> expectedIds = currentIds.stream().map(String::valueOf).toList();
        if (!Objects.equals(prepared.expectedState().get("userId"), String.valueOf(user.getUserId()))
            || !Objects.equals(prepared.expectedState().get("currentRoleIds"), expectedIds)) {
            throw new AgentActionException("STALE_STATE", "The user's roles changed after the preview was created");
        }
        AgentActionPreview currentPreview = preview(command);
        @SuppressWarnings("unchecked")
        List<Number> finalNumbers = (List<Number>) currentPreview.preview().get("resultRoleIds");
        Long[] finalIds = finalNumbers.stream().map(Number::longValue).toArray(Long[]::new);
        userService.insertUserAuth(user.getUserId(), finalIds);
        return AgentActionExecutionResult.success(Map.of(
            "userId", user.getUserId(),
            "userName", user.getUserName(),
            "operation", normalizeOperation(command.operation()),
            "roleIds", List.of(finalIds)
        ));
    }

    private void protectTarget(SysUserVo user) {
        if (SystemConstants.SUPER_ADMIN_ID.equals(user.getUserId())) {
            throw new AgentActionException("FORBIDDEN_TARGET", "The super administrator account cannot be changed");
        }
        if (Objects.equals(LoginHelper.getUserId(), user.getUserId())) {
            throw new AgentActionException("FORBIDDEN_TARGET", "You cannot change your own roles through Agent");
        }
        userService.checkUserAllowed(user.getUserId());
    }

    private void validateRoles(List<SysRoleVo> roles, String operation) {
        for (SysRoleVo role : roles) {
            if (role.isSuperAdmin() || SystemConstants.SUPER_ADMIN_ID.equals(role.getRoleId())) {
                throw new AgentActionException("FORBIDDEN_TARGET", "The super administrator role cannot be changed");
            }
            if ("ADD".equals(operation) && !"0".equals(role.getStatus())) {
                throw new AgentActionException("INVALID_ARGUMENT", "Disabled role " + role.getRoleName() + " cannot be assigned");
            }
        }
    }

    private String normalizeOperation(String operation) {
        if (operation == null) {
            throw new AgentActionException("INVALID_ARGUMENT", "operation must be ADD or REMOVE");
        }
        String normalized = operation.trim().toUpperCase();
        if (!"ADD".equals(normalized) && !"REMOVE".equals(normalized)) {
            throw new AgentActionException("INVALID_ARGUMENT", "operation must be ADD or REMOVE");
        }
        return normalized;
    }

    private List<Long> sorted(Iterable<Long> values) {
        List<Long> result = new ArrayList<>();
        if (values != null) {
            values.forEach(value -> {
                if (value != null) {
                    result.add(value);
                }
            });
        }
        result.sort(Long::compareTo);
        return result;
    }

    private Map<String, Object> roleTarget(SysRoleVo role) {
        return Map.of("id", role.getRoleId(), "name", role.getRoleName(), "key", role.getRoleKey());
    }
}
