package org.dromara.agent.service;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.tool.AgentToolParameters;
import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemRoleOverviewResult;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysRoleBo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysMenuService;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregates role, member, and menu services into a role overview.
 */
@Service
@RequiredArgsConstructor
public class SystemRoleOverviewService {

    private static final int MAX_PERMISSIONS = 50;

    private final ISysRoleService roleService;
    private final ISysUserService userService;
    private final ISysMenuService menuService;

    public AgentToolResult<SystemRoleOverviewResult> getOverview(
        Long roleId,
        String roleName,
        Boolean includeMembers,
        Integer memberLimit
    ) {
        String normalizedRoleName = AgentToolParameters.trimToNull(roleName);
        if ((roleId == null || roleId <= 0) && normalizedRoleName == null) {
            return AgentToolResult.failure(
                "INVALID_ARGUMENT",
                "Provide a positive roleId or an exact roleName"
            );
        }
        if (roleId != null && roleId <= 0) {
            return AgentToolResult.failure("INVALID_ARGUMENT", "roleId must be a positive number");
        }

        TargetResolution resolution = resolveRole(roleId, normalizedRoleName);
        if (resolution.failure() != null) {
            return resolution.failure();
        }
        SysRoleVo role = resolution.role();
        roleService.checkRoleDataScope(role.getRoleId());

        boolean returnMembers = Boolean.TRUE.equals(includeMembers);
        int safeMemberLimit = AgentToolParameters.normalizeLimit(memberLimit);
        long memberCount = roleService.countUserRoleByRoleId(role.getRoleId());
        List<SystemRoleOverviewResult.MemberSummary> members = returnMembers
            ? loadMembers(role.getRoleId(), safeMemberLimit)
            : List.of();

        Set<String> permissionSet = menuService.selectMenuPermsByRoleId(role.getRoleId());
        List<String> allPermissions = permissionSet == null
            ? List.of()
            : permissionSet.stream()
                .filter(permission -> permission != null && !permission.isBlank())
                .sorted()
                .toList();

        SystemRoleOverviewResult data = new SystemRoleOverviewResult(
            role.getRoleId(),
            role.getRoleName(),
            role.getRoleKey(),
            normalizeStatus(role.getStatus()),
            role.getDataScope(),
            describeDataScope(role.getDataScope()),
            memberCount,
            members,
            allPermissions.size(),
            allPermissions.stream().limit(MAX_PERMISSIONS).toList()
        );

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("membersIncluded", returnMembers);
        metadata.put("membersTruncated", returnMembers && memberCount > members.size());
        metadata.put("memberLimit", safeMemberLimit);
        metadata.put("permissionsTruncated", allPermissions.size() > MAX_PERMISSIONS);
        metadata.put("permissionLimit", MAX_PERMISSIONS);
        return AgentToolResult.success("Role overview loaded", data, metadata);
    }

    private TargetResolution resolveRole(Long roleId, String roleName) {
        if (roleId != null) {
            SysRoleVo role = roleService.selectRoleById(roleId);
            if (role == null) {
                return TargetResolution.failed(AgentToolResult.failure(
                    "NOT_FOUND",
                    "The requested role does not exist"
                ));
            }
            if (roleName != null && !roleName.equalsIgnoreCase(role.getRoleName())) {
                return TargetResolution.failed(AgentToolResult.failure(
                    "INVALID_ARGUMENT",
                    "roleId and roleName identify different roles"
                ));
            }
            return TargetResolution.resolved(role);
        }

        SysRoleBo query = new SysRoleBo();
        query.setRoleName(roleName);
        List<SysRoleVo> candidates = safeList(roleService.selectRoleList(query)).stream()
            .filter(candidate -> roleName.equalsIgnoreCase(candidate.getRoleName()))
            .toList();
        if (candidates.isEmpty()) {
            return TargetResolution.failed(AgentToolResult.failure(
                "NOT_FOUND",
                "The requested role does not exist"
            ));
        }
        if (candidates.size() > 1) {
            List<Map<String, Object>> choices = candidates.stream()
                .limit(AgentToolParameters.MAX_LIST_LIMIT)
                .map(this::toCandidate)
                .toList();
            return TargetResolution.failed(AgentToolResult.failure(
                "AMBIGUOUS_TARGET",
                "Multiple roles have this name; ask the user to choose a role ID",
                null,
                Map.of("candidates", choices)
            ));
        }
        return TargetResolution.resolved(candidates.get(0));
    }

    private List<SystemRoleOverviewResult.MemberSummary> loadMembers(Long roleId, int limit) {
        SysUserBo query = new SysUserBo();
        query.setRoleId(roleId);
        TableDataInfo<SysUserVo> page = userService.selectAllocatedList(query, new PageQuery(limit, 1));
        List<SysUserVo> users = page.getRows() == null ? List.of() : page.getRows();
        return users.stream()
            .map(user -> new SystemRoleOverviewResult.MemberSummary(
                user.getUserId(),
                user.getUserName(),
                user.getNickName(),
                user.getDeptName(),
                normalizeStatus(user.getStatus())
            ))
            .toList();
    }

    private Map<String, Object> toCandidate(SysRoleVo role) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("roleId", role.getRoleId());
        candidate.put("roleName", role.getRoleName());
        candidate.put("roleKey", role.getRoleKey());
        return candidate;
    }

    private String normalizeStatus(String status) {
        return SystemConstants.NORMAL.equals(status) ? "NORMAL" : "DISABLED";
    }

    private String describeDataScope(String dataScope) {
        return switch (dataScope == null ? "" : dataScope) {
            case "1" -> "ALL_DATA";
            case "2" -> "CUSTOM_DEPARTMENTS";
            case "3" -> "CURRENT_DEPARTMENT";
            case "4" -> "CURRENT_DEPARTMENT_AND_CHILDREN";
            case "5" -> "SELF_ONLY";
            case "6" -> "DEPARTMENT_AND_CHILDREN_OR_SELF";
            default -> "UNKNOWN";
        };
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record TargetResolution(
        SysRoleVo role,
        AgentToolResult<SystemRoleOverviewResult> failure
    ) {

        private static TargetResolution resolved(SysRoleVo role) {
            return new TargetResolution(role, null);
        }

        private static TargetResolution failed(AgentToolResult<SystemRoleOverviewResult> failure) {
            return new TargetResolution(null, failure);
        }
    }

}
