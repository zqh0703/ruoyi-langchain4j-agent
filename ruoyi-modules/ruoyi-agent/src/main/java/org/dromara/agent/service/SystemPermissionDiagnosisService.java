package org.dromara.agent.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.tool.AgentExecutionContext;
import org.dromara.agent.tool.AgentToolParameters;
import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemPermissionDiagnosisResult;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.system.domain.SysRoleMenu;
import org.dromara.system.domain.bo.SysMenuBo;
import org.dromara.system.domain.vo.SysMenuVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysRoleMenuMapper;
import org.dromara.system.service.ISysMenuService;
import org.dromara.system.service.ISysPermissionService;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Explains whether and why a user can access one menu permission.
 */
@Service
@RequiredArgsConstructor
public class SystemPermissionDiagnosisService {

    private final ISysUserService userService;
    private final ISysRoleService roleService;
    private final ISysMenuService menuService;
    private final ISysPermissionService permissionService;
    private final SysRoleMenuMapper roleMenuMapper;
    private final AgentExecutionContext executionContext;

    public AgentToolResult<SystemPermissionDiagnosisResult> diagnose(
        Long userId,
        String userName,
        String permissionCode,
        String menuName
    ) {
        String normalizedUserName = AgentToolParameters.trimToNull(userName);
        String normalizedPermission = AgentToolParameters.trimToNull(permissionCode);
        String normalizedMenuName = AgentToolParameters.trimToNull(menuName);
        if ((userId == null || userId <= 0) && normalizedUserName == null) {
            return AgentToolResult.failure(
                "INVALID_ARGUMENT",
                "Provide a positive userId or an exact userName"
            );
        }
        if (userId != null && userId <= 0) {
            return AgentToolResult.failure("INVALID_ARGUMENT", "userId must be a positive number");
        }
        if (normalizedPermission == null && normalizedMenuName == null) {
            return AgentToolResult.failure(
                "INVALID_ARGUMENT",
                "Provide an exact permissionCode or menuName"
            );
        }

        UserResolution userResolution = resolveUser(userId, normalizedUserName);
        if (userResolution.failure() != null) {
            return userResolution.failure();
        }
        SysUserVo user = userResolution.user();
        userService.checkUserDataScope(user.getUserId());

        MenuResolution menuResolution = resolveMenu(normalizedPermission, normalizedMenuName);
        if (menuResolution.failure() != null) {
            return menuResolution.failure();
        }
        SysMenuVo menu = menuResolution.menu();
        String targetPermission = AgentToolParameters.trimToNull(menu.getPerms());

        List<SysRoleVo> assignedRoles = safeList(roleService.selectRolesByUserId(user.getUserId()));
        Set<Long> assignedRoleIds = assignedRoles.stream()
            .map(SysRoleVo::getRoleId)
            .collect(java.util.stream.Collectors.toSet());
        Set<Long> menuRoleIds = loadMenuRoleIds(menu.getMenuId());

        List<SysRoleVo> sourceRoleEntities = assignedRoles.stream()
            .filter(role -> SystemConstants.NORMAL.equals(role.getStatus()))
            .filter(role -> grantsTarget(role, menuRoleIds, targetPermission))
            .toList();

        Set<String> effectivePermissions = permissionService.getMenuPermission(user.getUserId());
        boolean wildcard = effectivePermissions != null && effectivePermissions.contains("*:*:*");
        boolean permissionGranted = targetPermission == null
            || wildcard
            || effectivePermissions != null && effectivePermissions.contains(targetPermission);
        boolean menuGranted = menuService.selectMenuList(new SysMenuBo(), user.getUserId()).stream()
            .anyMatch(candidate -> menu.getMenuId().equals(candidate.getMenuId()));
        boolean userEnabled = SystemConstants.NORMAL.equals(user.getStatus());
        boolean menuEnabled = SystemConstants.NORMAL.equals(menu.getStatus());
        boolean authorized = userEnabled && menuEnabled && (targetPermission == null ? menuGranted : permissionGranted);

        List<String> blockingReasons = new ArrayList<>();
        if (!userEnabled) {
            blockingReasons.add("USER_DISABLED");
        }
        if (!menuEnabled) {
            blockingReasons.add("MENU_DISABLED");
        }
        if (targetPermission == null && !menuGranted) {
            blockingReasons.add("MENU_NOT_GRANTED");
        }
        if (targetPermission != null && !permissionGranted) {
            blockingReasons.add("PERMISSION_NOT_GRANTED");
        }
        boolean disabledAssignedSource = assignedRoles.stream()
            .filter(role -> !SystemConstants.NORMAL.equals(role.getStatus()))
            .anyMatch(role -> menuRoleIds.contains(role.getRoleId()));
        if (!authorized && disabledAssignedSource) {
            blockingReasons.add("ONLY_DISABLED_ROLE_GRANTS_TARGET");
        }
        if (!authorized && sourceRoleEntities.isEmpty() && !wildcard) {
            blockingReasons.add("NO_ACTIVE_ROLE_SOURCE");
        }

        List<SystemPermissionDiagnosisResult.RoleSummary> candidateRoles = loadCandidateRoles(menuRoleIds).stream()
            .filter(role -> !assignedRoleIds.contains(role.getRoleId()))
            .limit(AgentToolParameters.MAX_LIST_LIMIT)
            .map(this::toRoleSummary)
            .toList();

        SystemPermissionDiagnosisResult data = new SystemPermissionDiagnosisResult(
            new SystemPermissionDiagnosisResult.UserSummary(
                user.getUserId(), user.getUserName(), user.getNickName(), normalizeStatus(user.getStatus())
            ),
            new SystemPermissionDiagnosisResult.MenuSummary(
                menu.getMenuId(), menu.getMenuName(), menu.getMenuType(),
                normalizeStatus(menu.getStatus()), menu.getVisible()
            ),
            targetPermission,
            authorized,
            sourceRoleEntities.stream().map(this::toRoleSummary).toList(),
            blockingReasons,
            candidateRoles
        );

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("wildcardPermission", wildcard);
        metadata.put("candidateRolesTruncated", loadCandidateRoles(menuRoleIds).size() > AgentToolParameters.MAX_LIST_LIMIT);
        return AgentToolResult.success("Permission diagnosis completed", data, metadata);
    }

    private UserResolution resolveUser(Long userId, String userName) {
        SysUserVo user = userId == null
            ? userService.selectUserByUserName(userName)
            : userService.selectUserById(userId);
        if (user == null) {
            return UserResolution.failed(AgentToolResult.failure(
                "NOT_FOUND",
                "The requested user does not exist"
            ));
        }
        if (userName != null && !userName.equalsIgnoreCase(user.getUserName())) {
            return UserResolution.failed(AgentToolResult.failure(
                "INVALID_ARGUMENT",
                "userId and userName identify different users"
            ));
        }
        return UserResolution.resolved(user);
    }

    private MenuResolution resolveMenu(String permissionCode, String menuName) {
        List<SysMenuVo> candidates = safeList(
            menuService.selectMenuList(new SysMenuBo(), executionContext.currentUserId())
        ).stream()
            .filter(menu -> permissionCode == null || permissionCode.equals(menu.getPerms()))
            .filter(menu -> menuName == null || menuName.equalsIgnoreCase(menu.getMenuName()))
            .toList();
        if (candidates.isEmpty()) {
            return MenuResolution.failed(AgentToolResult.failure(
                "NOT_FOUND",
                "The requested menu permission does not exist or is not visible to the operator"
            ));
        }
        if (candidates.size() > 1) {
            List<Map<String, Object>> choices = candidates.stream()
                .limit(AgentToolParameters.MAX_LIST_LIMIT)
                .map(this::toMenuCandidate)
                .toList();
            return MenuResolution.failed(AgentToolResult.failure(
                "AMBIGUOUS_TARGET",
                "Multiple menu permissions matched; ask the user to choose one",
                null,
                Map.of("candidates", choices)
            ));
        }
        return MenuResolution.resolved(candidates.get(0));
    }

    private boolean grantsTarget(SysRoleVo role, Set<Long> menuRoleIds, String permissionCode) {
        if (permissionCode == null) {
            return menuRoleIds.contains(role.getRoleId());
        }
        Set<String> permissions = menuService.selectMenuPermsByRoleId(role.getRoleId());
        return permissions != null && permissions.contains(permissionCode);
    }

    private Set<Long> loadMenuRoleIds(Long menuId) {
        return roleMenuMapper.selectList(
            Wrappers.lambdaQuery(SysRoleMenu.class).eq(SysRoleMenu::getMenuId, menuId)
        ).stream().map(SysRoleMenu::getRoleId).collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    private List<SysRoleVo> loadCandidateRoles(Set<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return safeList(roleService.selectRoleByIds(roleIds.stream().sorted().toList()));
    }

    private SystemPermissionDiagnosisResult.RoleSummary toRoleSummary(SysRoleVo role) {
        return new SystemPermissionDiagnosisResult.RoleSummary(
            role.getRoleId(), role.getRoleName(), role.getRoleKey(), normalizeStatus(role.getStatus())
        );
    }

    private Map<String, Object> toMenuCandidate(SysMenuVo menu) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("menuId", menu.getMenuId());
        candidate.put("menuName", menu.getMenuName());
        candidate.put("permissionCode", menu.getPerms());
        return candidate;
    }

    private String normalizeStatus(String status) {
        return SystemConstants.NORMAL.equals(status) ? "NORMAL" : "DISABLED";
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record UserResolution(
        SysUserVo user,
        AgentToolResult<SystemPermissionDiagnosisResult> failure
    ) {
        private static UserResolution resolved(SysUserVo user) {
            return new UserResolution(user, null);
        }

        private static UserResolution failed(AgentToolResult<SystemPermissionDiagnosisResult> failure) {
            return new UserResolution(null, failure);
        }
    }

    private record MenuResolution(
        SysMenuVo menu,
        AgentToolResult<SystemPermissionDiagnosisResult> failure
    ) {
        private static MenuResolution resolved(SysMenuVo menu) {
            return new MenuResolution(menu, null);
        }

        private static MenuResolution failed(AgentToolResult<SystemPermissionDiagnosisResult> failure) {
            return new MenuResolution(null, failure);
        }
    }

}
