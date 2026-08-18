package org.dromara.agent.service;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.tool.AgentToolParameters;
import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemUserAccessProfileResult;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysDeptService;
import org.dromara.system.service.ISysPermissionService;
import org.dromara.system.service.ISysPostService;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregates existing RuoYi services into a safe user access profile.
 */
@Service
@RequiredArgsConstructor
public class SystemUserAccessProfileService {

    private static final int MAX_PERMISSIONS = 50;

    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final ISysPostService postService;
    private final ISysRoleService roleService;
    private final ISysPermissionService permissionService;

    public AgentToolResult<SystemUserAccessProfileResult> getProfile(Long userId, String userName) {
        String normalizedUserName = AgentToolParameters.trimToNull(userName);
        if ((userId == null || userId <= 0) && normalizedUserName == null) {
            return AgentToolResult.failure(
                "INVALID_ARGUMENT",
                "Provide a positive userId or an exact userName"
            );
        }
        if (userId != null && userId <= 0) {
            return AgentToolResult.failure("INVALID_ARGUMENT", "userId must be a positive number");
        }

        SysUserVo user = userId == null
            ? userService.selectUserByUserName(normalizedUserName)
            : userService.selectUserById(userId);
        if (user == null) {
            return AgentToolResult.failure("NOT_FOUND", "The requested user does not exist");
        }
        if (normalizedUserName != null && !normalizedUserName.equalsIgnoreCase(user.getUserName())) {
            return AgentToolResult.failure(
                "INVALID_ARGUMENT",
                "userId and userName identify different users"
            );
        }

        // Reuse the same data-scope guard as the system user controllers.
        userService.checkUserDataScope(user.getUserId());

        SysDeptVo department = user.getDeptId() == null
            ? null
            : deptService.selectDeptById(user.getDeptId());
        List<SysPostVo> posts = safeList(postService.selectPostsByUserId(user.getUserId()));
        List<SysRoleVo> roles = safeList(roleService.selectRolesByUserId(user.getUserId()));
        Set<String> effectivePermissions = permissionService.getMenuPermission(user.getUserId());
        List<String> allPermissions = effectivePermissions == null
            ? List.of()
            : effectivePermissions.stream()
                .filter(permission -> permission != null && !permission.isBlank())
                .sorted()
                .toList();
        List<String> returnedPermissions = allPermissions.stream()
            .limit(MAX_PERMISSIONS)
            .toList();

        SystemUserAccessProfileResult profile = new SystemUserAccessProfileResult(
            user.getUserId(),
            user.getUserName(),
            user.getNickName(),
            normalizeStatus(user.getStatus()),
            toDepartmentSummary(department),
            posts.stream()
                .sorted(Comparator.comparing(SysPostVo::getPostId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toPostSummary)
                .toList(),
            roles.stream()
                .sorted(Comparator.comparing(SysRoleVo::getRoleId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toRoleSummary)
                .toList(),
            allPermissions.size(),
            returnedPermissions
        );

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("permissionsTruncated", allPermissions.size() > MAX_PERMISSIONS);
        metadata.put("permissionLimit", MAX_PERMISSIONS);
        return AgentToolResult.success("User access profile loaded", profile, metadata);
    }

    private SystemUserAccessProfileResult.DepartmentSummary toDepartmentSummary(SysDeptVo department) {
        if (department == null) {
            return null;
        }
        return new SystemUserAccessProfileResult.DepartmentSummary(
            department.getDeptId(),
            department.getDeptName(),
            department.getParentId(),
            department.getParentName()
        );
    }

    private SystemUserAccessProfileResult.PostSummary toPostSummary(SysPostVo post) {
        return new SystemUserAccessProfileResult.PostSummary(
            post.getPostId(),
            post.getPostCode(),
            post.getPostName(),
            normalizeStatus(post.getStatus())
        );
    }

    private SystemUserAccessProfileResult.RoleSummary toRoleSummary(SysRoleVo role) {
        return new SystemUserAccessProfileResult.RoleSummary(
            role.getRoleId(),
            role.getRoleName(),
            role.getRoleKey(),
            normalizeStatus(role.getStatus()),
            role.getDataScope()
        );
    }

    private String normalizeStatus(String status) {
        return SystemConstants.NORMAL.equals(status) ? "NORMAL" : "DISABLED";
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

}
