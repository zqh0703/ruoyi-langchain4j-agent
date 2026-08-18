package org.dromara.agent.action.handler;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.action.AgentActionException;
import org.dromara.agent.action.AgentActionExecutionResult;
import org.dromara.agent.action.AgentActionHandler;
import org.dromara.agent.action.AgentActionPreview;
import org.dromara.agent.action.AgentActionRiskLevel;
import org.dromara.agent.action.AgentActionTargetResolver;
import org.dromara.agent.action.TemporaryPasswordGenerator;
import org.dromara.agent.action.command.SystemUserCreateCommand;
import cn.hutool.crypto.digest.BCrypt;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.service.ISysTenantService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemUserCreateActionHandler implements AgentActionHandler<SystemUserCreateCommand> {

    public static final String TOOL_CODE = "system_user_create";

    private final AgentActionTargetResolver targetResolver;
    private final ISysUserService userService;
    private final ISysTenantService tenantService;
    private final TemporaryPasswordGenerator passwordGenerator;

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    @Override
    public AgentActionRiskLevel riskLevel() {
        return AgentActionRiskLevel.MEDIUM;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:user:add", "system:dept:query", "system:role:query", "system:post:list");
    }

    @Override
    public Class<SystemUserCreateCommand> commandType() {
        return SystemUserCreateCommand.class;
    }

    @Override
    public AgentActionPreview preview(SystemUserCreateCommand command) {
        validateNames(command);
        Resolved resolved = resolve(command);
        SysUserBo candidate = new SysUserBo();
        candidate.setUserName(command.userName().trim());
        if (!userService.checkUserNameUnique(candidate)) {
            throw new AgentActionException("ALREADY_EXISTS", "Username " + command.userName().trim() + " already exists");
        }
        if (TenantHelper.isEnable() && !tenantService.checkAccountBalance(TenantHelper.getTenantId())) {
            throw new AgentActionException("TENANT_QUOTA_EXCEEDED", "The current tenant has no remaining user quota");
        }

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("userName", command.userName().trim());
        preview.put("nickName", command.nickName().trim());
        preview.put("department", target(resolved.department().getDeptId(), resolved.department().getDeptName()));
        preview.put("roles", resolved.roles().stream().map(role -> target(role.getRoleId(), role.getRoleName())).toList());
        preview.put("posts", resolved.posts().stream().map(post -> target(post.getPostId(), post.getPostName())).toList());
        preview.put("status", "NORMAL");
        preview.put("passwordPolicy", "A random temporary password will be shown once after confirmation");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("userNameAvailable", true);
        expected.put("deptId", String.valueOf(resolved.department().getDeptId()));
        expected.put("roleIds", resolved.roles().stream().map(SysRoleVo::getRoleId).sorted().map(String::valueOf).toList());
        expected.put("postIds", resolved.posts().stream().map(SysPostVo::getPostId).sorted().map(String::valueOf).toList());
        String summary = "Create normal user " + command.userName().trim() + " in department "
            + resolved.department().getDeptName();
        return AgentActionPreview.required(summary, preview, expected);
    }

    @Override
    public AgentActionExecutionResult execute(SystemUserCreateCommand command, AgentActionPreview prepared) {
        AgentActionPreview current = preview(command);
        if (!current.expectedState().equals(prepared.expectedState())) {
            throw new AgentActionException("STALE_STATE", "User creation targets changed after the preview was created");
        }
        Resolved resolved = resolve(command);
        String temporaryPassword = passwordGenerator.generate();
        SysUserBo user = new SysUserBo();
        user.setUserName(command.userName().trim());
        user.setNickName(command.nickName().trim());
        user.setDeptId(resolved.department().getDeptId());
        user.setRoleIds(resolved.roles().stream().map(SysRoleVo::getRoleId).toArray(Long[]::new));
        user.setPostIds(resolved.posts().stream().map(SysPostVo::getPostId).toArray(Long[]::new));
        user.setStatus("0");
        user.setPassword(BCrypt.hashpw(temporaryPassword));
        if (userService.insertUser(user) != 1) {
            throw new AgentActionException("EXECUTION_FAILED", "The user could not be created");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getUserId());
        result.put("userName", user.getUserName());
        result.put("nickName", user.getNickName());
        result.put("deptId", user.getDeptId());
        result.put("status", "NORMAL");
        return AgentActionExecutionResult.withSecret(result, "TEMPORARY_PASSWORD", temporaryPassword);
    }

    private Resolved resolve(SystemUserCreateCommand command) {
        SysDeptVo department = targetResolver.department(command.deptId(), command.deptName());
        if (!"0".equals(department.getStatus())) {
            throw new AgentActionException("INVALID_ARGUMENT", "The selected department is disabled");
        }
        List<SysRoleVo> roles = targetResolver.roles(command.roleIds(), command.roleNames());
        for (SysRoleVo role : roles) {
            if (role.isSuperAdmin()) {
                throw new AgentActionException("FORBIDDEN_TARGET", "The super administrator role cannot be assigned");
            }
            if (!"0".equals(role.getStatus())) {
                throw new AgentActionException("INVALID_ARGUMENT", "Role " + role.getRoleName() + " is disabled");
            }
        }
        List<SysPostVo> posts = targetResolver.posts(command.postIds(), command.postNames(), department.getDeptId());
        return new Resolved(department, roles, posts);
    }

    private void validateNames(SystemUserCreateCommand command) {
        if (command.userName() == null || command.userName().trim().length() < 2
            || command.userName().trim().length() > 30) {
            throw new AgentActionException("INVALID_ARGUMENT", "Username must contain 2 to 30 characters");
        }
        if (command.nickName() == null || command.nickName().isBlank() || command.nickName().trim().length() > 30) {
            throw new AgentActionException("INVALID_ARGUMENT", "Nickname is required and cannot exceed 30 characters");
        }
    }

    private Map<String, Object> target(Long id, String name) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", id);
        value.put("name", name);
        return value;
    }

    private record Resolved(SysDeptVo department, List<SysRoleVo> roles, List<SysPostVo> posts) {
        private Resolved {
            roles = new ArrayList<>(roles);
            posts = new ArrayList<>(posts);
        }
    }
}
