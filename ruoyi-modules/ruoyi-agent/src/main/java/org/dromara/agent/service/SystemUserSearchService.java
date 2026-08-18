package org.dromara.agent.service;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.tool.AgentToolParameters;
import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemUserSearchResult;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.domain.bo.SysRoleBo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.service.ISysDeptService;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines existing RuoYi services for the system user search Agent tool.
 */
@Service
@RequiredArgsConstructor
public class SystemUserSearchService {

    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final ISysRoleService roleService;
    private final SysUserRoleMapper userRoleMapper;

    public AgentToolResult<SystemUserSearchResult> search(
        String keyword,
        String deptName,
        String roleName,
        String status,
        Integer limit
    ) {
        String normalizedKeyword = AgentToolParameters.trimToNull(keyword);
        String normalizedDeptName = AgentToolParameters.trimToNull(deptName);
        String normalizedRoleName = AgentToolParameters.trimToNull(roleName);
        StatusFilter statusFilter = StatusFilter.parse(status);
        if (statusFilter == null) {
            return AgentToolResult.failure(
                "INVALID_ARGUMENT",
                "status must be NORMAL, DISABLED, or ALL"
            );
        }
        int safeLimit = AgentToolParameters.normalizeLimit(limit);

        TargetResolution<SysDeptVo> department = resolveDepartment(normalizedDeptName);
        if (department.failure() != null) {
            return department.failure();
        }
        if (department.notFound()) {
            return emptyResult(
                normalizedKeyword, normalizedDeptName, null, normalizedRoleName, null, statusFilter, safeLimit
            );
        }

        TargetResolution<SysRoleVo> role = resolveRole(normalizedRoleName);
        if (role.failure() != null) {
            return role.failure();
        }
        if (role.notFound()) {
            return emptyResult(
                normalizedKeyword, normalizedDeptName, department.target(), normalizedRoleName, null,
                statusFilter, safeLimit
            );
        }

        SysUserBo query = new SysUserBo();
        query.setKeyword(normalizedKeyword);
        query.setStatus(statusFilter.databaseValue());
        if (department.target() != null) {
            query.setDeptId(department.target().getDeptId());
        }
        if (role.target() != null) {
            List<Long> roleUserIds = userRoleMapper.selectUserIdsByRoleId(role.target().getRoleId());
            if (roleUserIds.isEmpty()) {
                return emptyResult(
                    normalizedKeyword, normalizedDeptName, department.target(), normalizedRoleName, role.target(),
                    statusFilter, safeLimit
                );
            }
            query.setUserIds(String.join(",", roleUserIds.stream().map(String::valueOf).toList()));
        }

        TableDataInfo<SysUserVo> page = userService.selectPageUserList(query, new PageQuery(safeLimit, 1));
        List<SysUserVo> rows = page.getRows() == null ? List.of() : page.getRows();
        Map<Long, String> departmentNames = loadDepartmentNames(rows);
        List<SystemUserSearchResult.UserSummary> users = rows.stream()
            .map(user -> toUserSummary(user, departmentNames))
            .toList();

        SystemUserSearchResult data = new SystemUserSearchResult(page.getTotal(), users);
        return AgentToolResult.success(
            users.isEmpty() ? "No users matched the supplied filters" : "User search completed",
            data,
            metadata(
                normalizedKeyword, normalizedDeptName, department.target(), normalizedRoleName, role.target(),
                statusFilter, safeLimit, page.getTotal()
            )
        );
    }

    private TargetResolution<SysDeptVo> resolveDepartment(String deptName) {
        if (deptName == null) {
            return TargetResolution.resolved(null);
        }
        SysDeptBo query = new SysDeptBo();
        query.setDeptName(deptName);
        List<SysDeptVo> candidates = deptService.selectDeptList(query);
        return resolveByName(
            deptName,
            candidates,
            SysDeptVo::getDeptName,
            candidate -> Map.of("deptId", candidate.getDeptId(), "deptName", candidate.getDeptName())
        );
    }

    private TargetResolution<SysRoleVo> resolveRole(String roleName) {
        if (roleName == null) {
            return TargetResolution.resolved(null);
        }
        SysRoleBo query = new SysRoleBo();
        query.setRoleName(roleName);
        List<SysRoleVo> candidates = roleService.selectRoleList(query);
        return resolveByName(
            roleName,
            candidates,
            SysRoleVo::getRoleName,
            candidate -> Map.of("roleId", candidate.getRoleId(), "roleName", candidate.getRoleName())
        );
    }

    private <T> TargetResolution<T> resolveByName(
        String requestedName,
        List<T> candidates,
        java.util.function.Function<T, String> nameGetter,
        java.util.function.Function<T, Map<String, Object>> candidateMapper
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return TargetResolution.missing();
        }
        List<T> exactMatches = candidates.stream()
            .filter(candidate -> requestedName.equalsIgnoreCase(nameGetter.apply(candidate)))
            .toList();
        List<T> resolvedCandidates = exactMatches.isEmpty() ? candidates : exactMatches;
        if (resolvedCandidates.size() == 1) {
            return TargetResolution.resolved(resolvedCandidates.get(0));
        }
        List<Map<String, Object>> choices = resolvedCandidates.stream()
            .limit(AgentToolParameters.MAX_LIST_LIMIT)
            .map(candidateMapper)
            .toList();
        AgentToolResult<SystemUserSearchResult> failure = AgentToolResult.failure(
            "AMBIGUOUS_TARGET",
            "Multiple targets matched; ask the user to choose one",
            null,
            Map.of("candidates", choices)
        );
        return TargetResolution.failed(failure);
    }

    private AgentToolResult<SystemUserSearchResult> emptyResult(
        String keyword,
        String requestedDeptName,
        SysDeptVo department,
        String requestedRoleName,
        SysRoleVo role,
        StatusFilter status,
        int limit
    ) {
        return AgentToolResult.success(
            "No users matched the supplied filters",
            new SystemUserSearchResult(0, List.of()),
            metadata(keyword, requestedDeptName, department, requestedRoleName, role, status, limit, 0)
        );
    }

    private Map<String, Object> metadata(
        String keyword,
        String requestedDeptName,
        SysDeptVo department,
        String requestedRoleName,
        SysRoleVo role,
        StatusFilter status,
        int limit,
        long total
    ) {
        Map<String, Object> filters = new LinkedHashMap<>();
        if (keyword != null) {
            filters.put("keyword", keyword);
        }
        if (department != null) {
            filters.put("deptId", department.getDeptId());
            filters.put("deptName", department.getDeptName());
        } else if (requestedDeptName != null) {
            filters.put("deptName", requestedDeptName);
        }
        if (role != null) {
            filters.put("roleId", role.getRoleId());
            filters.put("roleName", role.getRoleName());
        } else if (requestedRoleName != null) {
            filters.put("roleName", requestedRoleName);
        }
        filters.put("status", status.name());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("filters", filters);
        metadata.put("limit", limit);
        metadata.put("truncated", total > limit);
        return metadata;
    }

    private Map<Long, String> loadDepartmentNames(List<SysUserVo> users) {
        List<Long> deptIds = users.stream()
            .map(SysUserVo::getDeptId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        return deptService.selectDeptByIds(deptIds).stream()
            .collect(java.util.stream.Collectors.toMap(
                SysDeptVo::getDeptId,
                SysDeptVo::getDeptName,
                (first, ignored) -> first,
                LinkedHashMap::new
            ));
    }

    private SystemUserSearchResult.UserSummary toUserSummary(
        SysUserVo user,
        Map<Long, String> departmentNames
    ) {
        return new SystemUserSearchResult.UserSummary(
            user.getUserId(),
            user.getUserName(),
            user.getNickName(),
            departmentNames.getOrDefault(user.getDeptId(), user.getDeptName()),
            SystemConstants.NORMAL.equals(user.getStatus()) ? "NORMAL" : "DISABLED",
            user.getCreateTime()
        );
    }

    private enum StatusFilter {
        NORMAL(SystemConstants.NORMAL),
        DISABLED(SystemConstants.DISABLE),
        ALL(null);

        private final String databaseValue;

        StatusFilter(String databaseValue) {
            this.databaseValue = databaseValue;
        }

        private String databaseValue() {
            return databaseValue;
        }

        private static StatusFilter parse(String value) {
            String normalized = AgentToolParameters.trimToNull(value);
            if (normalized == null || "0".equals(normalized) || "NORMAL".equalsIgnoreCase(normalized)) {
                return NORMAL;
            }
            if ("1".equals(normalized) || "DISABLED".equalsIgnoreCase(normalized)) {
                return DISABLED;
            }
            if ("ALL".equalsIgnoreCase(normalized)) {
                return ALL;
            }
            return null;
        }
    }

    private record TargetResolution<T>(
        T target,
        AgentToolResult<SystemUserSearchResult> failure,
        boolean notFound
    ) {

        private static <T> TargetResolution<T> resolved(T target) {
            return new TargetResolution<>(target, null, false);
        }

        private static <T> TargetResolution<T> missing() {
            return new TargetResolution<>(null, null, true);
        }

        private static <T> TargetResolution<T> failed(AgentToolResult<SystemUserSearchResult> failure) {
            return new TargetResolution<>(null, failure, false);
        }
    }

}
