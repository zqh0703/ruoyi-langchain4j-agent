package org.dromara.agent.service;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.tool.AgentToolParameters;
import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemDepartmentOverviewResult;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.domain.bo.SysPostBo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysDeptService;
import org.dromara.system.service.ISysPostService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates department, user, and post services into a department overview.
 */
@Service
@RequiredArgsConstructor
public class SystemDepartmentOverviewService {

    private final ISysDeptService deptService;
    private final ISysUserService userService;
    private final ISysPostService postService;

    public AgentToolResult<SystemDepartmentOverviewResult> getOverview(
        Long deptId,
        String deptName,
        Boolean includeChildren
    ) {
        String normalizedDeptName = AgentToolParameters.trimToNull(deptName);
        if ((deptId == null || deptId <= 0) && normalizedDeptName == null) {
            return AgentToolResult.failure(
                "INVALID_ARGUMENT",
                "Provide a positive deptId or an exact deptName"
            );
        }
        if (deptId != null && deptId <= 0) {
            return AgentToolResult.failure("INVALID_ARGUMENT", "deptId must be a positive number");
        }

        TargetResolution resolution = resolveDepartment(deptId, normalizedDeptName);
        if (resolution.failure() != null) {
            return resolution.failure();
        }
        SysDeptVo department = resolution.department();
        deptService.checkDeptDataScope(department.getDeptId());

        boolean includeChildMetrics = Boolean.TRUE.equals(includeChildren);
        List<SysDeptVo> directChildren = loadDirectChildren(department.getDeptId());
        List<SystemDepartmentOverviewResult.ChildDepartmentSummary> returnedChildren = directChildren.stream()
            .limit(AgentToolParameters.MAX_LIST_LIMIT)
            .map(this::toChildSummary)
            .toList();

        SystemDepartmentOverviewResult data = new SystemDepartmentOverviewResult(
            department.getDeptId(),
            department.getDeptName(),
            normalizeStatus(department.getStatus()),
            toParentReference(department),
            loadLeader(department),
            directChildren.size(),
            returnedChildren,
            countUsers(department.getDeptId(), SystemConstants.NORMAL, includeChildMetrics),
            countUsers(department.getDeptId(), SystemConstants.DISABLE, includeChildMetrics),
            countPosts(department.getDeptId(), includeChildMetrics),
            includeChildMetrics
        );

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("childrenTruncated", directChildren.size() > AgentToolParameters.MAX_LIST_LIMIT);
        metadata.put("childDepartmentLimit", AgentToolParameters.MAX_LIST_LIMIT);
        metadata.put("metricScope", includeChildMetrics ? "DEPARTMENT_AND_DESCENDANTS" : "DEPARTMENT_ONLY");
        return AgentToolResult.success("Department overview loaded", data, metadata);
    }

    private TargetResolution resolveDepartment(Long deptId, String deptName) {
        if (deptId != null) {
            SysDeptVo department = deptService.selectDeptById(deptId);
            if (department == null) {
                return TargetResolution.failed(AgentToolResult.failure(
                    "NOT_FOUND",
                    "The requested department does not exist"
                ));
            }
            if (deptName != null && !deptName.equalsIgnoreCase(department.getDeptName())) {
                return TargetResolution.failed(AgentToolResult.failure(
                    "INVALID_ARGUMENT",
                    "deptId and deptName identify different departments"
                ));
            }
            return TargetResolution.resolved(department);
        }

        SysDeptBo query = new SysDeptBo();
        query.setDeptName(deptName);
        List<SysDeptVo> candidates = safeList(deptService.selectDeptList(query)).stream()
            .filter(candidate -> deptName.equalsIgnoreCase(candidate.getDeptName()))
            .toList();
        if (candidates.isEmpty()) {
            return TargetResolution.failed(AgentToolResult.failure(
                "NOT_FOUND",
                "The requested department does not exist"
            ));
        }
        if (candidates.size() > 1) {
            List<Map<String, Object>> choices = candidates.stream()
                .limit(AgentToolParameters.MAX_LIST_LIMIT)
                .map(this::toCandidate)
                .toList();
            return TargetResolution.failed(AgentToolResult.failure(
                "AMBIGUOUS_TARGET",
                "Multiple departments have this name; ask the user to choose a department ID",
                null,
                Map.of("candidates", choices)
            ));
        }
        return TargetResolution.resolved(candidates.get(0));
    }

    private List<SysDeptVo> loadDirectChildren(Long deptId) {
        SysDeptBo query = new SysDeptBo();
        query.setParentId(deptId);
        return safeList(deptService.selectDeptList(query)).stream()
            .sorted(Comparator.comparing(SysDeptVo::getOrderNum, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysDeptVo::getDeptId, Comparator.nullsLast(Long::compareTo)))
            .toList();
    }

    private SystemDepartmentOverviewResult.LeaderSummary loadLeader(SysDeptVo department) {
        if (department.getLeader() == null) {
            return null;
        }
        SysUserBo query = new SysUserBo();
        query.setUserId(department.getLeader());
        TableDataInfo<SysUserVo> page = userService.selectPageUserList(query, new PageQuery(1, 1));
        List<SysUserVo> users = page.getRows() == null ? List.of() : page.getRows();
        if (users.isEmpty()) {
            return new SystemDepartmentOverviewResult.LeaderSummary(
                department.getLeader(),
                null,
                department.getLeaderName()
            );
        }
        SysUserVo leader = users.get(0);
        return new SystemDepartmentOverviewResult.LeaderSummary(
            leader.getUserId(),
            leader.getUserName(),
            leader.getNickName()
        );
    }

    private long countUsers(Long deptId, String status, boolean includeChildren) {
        SysUserBo query = new SysUserBo();
        query.setStatus(status);
        if (includeChildren) {
            query.setDeptId(deptId);
        } else {
            query.setExactDeptId(deptId);
        }
        return userService.selectPageUserList(query, new PageQuery(1, 1)).getTotal();
    }

    private long countPosts(Long deptId, boolean includeChildren) {
        SysPostBo query = new SysPostBo();
        if (includeChildren) {
            query.setBelongDeptId(deptId);
        } else {
            query.setDeptId(deptId);
        }
        return postService.selectPagePostList(query, new PageQuery(1, 1)).getTotal();
    }

    private SystemDepartmentOverviewResult.DepartmentReference toParentReference(SysDeptVo department) {
        if (department.getParentId() == null || department.getParentId() == 0) {
            return null;
        }
        return new SystemDepartmentOverviewResult.DepartmentReference(
            department.getParentId(),
            department.getParentName()
        );
    }

    private SystemDepartmentOverviewResult.ChildDepartmentSummary toChildSummary(SysDeptVo department) {
        return new SystemDepartmentOverviewResult.ChildDepartmentSummary(
            department.getDeptId(),
            department.getDeptName(),
            normalizeStatus(department.getStatus())
        );
    }

    private Map<String, Object> toCandidate(SysDeptVo department) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("deptId", department.getDeptId());
        candidate.put("deptName", department.getDeptName());
        candidate.put("parentId", department.getParentId());
        candidate.put("parentName", department.getParentName());
        return candidate;
    }

    private String normalizeStatus(String status) {
        return SystemConstants.NORMAL.equals(status) ? "NORMAL" : "DISABLED";
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record TargetResolution(
        SysDeptVo department,
        AgentToolResult<SystemDepartmentOverviewResult> failure
    ) {

        private static TargetResolution resolved(SysDeptVo department) {
            return new TargetResolution(department, null);
        }

        private static TargetResolution failed(AgentToolResult<SystemDepartmentOverviewResult> failure) {
            return new TargetResolution(null, failure);
        }
    }

}
