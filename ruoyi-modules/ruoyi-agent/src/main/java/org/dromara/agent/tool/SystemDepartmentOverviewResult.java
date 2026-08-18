package org.dromara.agent.tool;

import java.util.List;

/**
 * Safe, structured payload returned by {@code system_department_overview}.
 */
public record SystemDepartmentOverviewResult(
    Long deptId,
    String deptName,
    String status,
    DepartmentReference parent,
    LeaderSummary leader,
    int childDepartmentCount,
    List<ChildDepartmentSummary> childDepartments,
    long normalUserCount,
    long disabledUserCount,
    long postCount,
    boolean includesChildrenInMetrics
) {

    public record DepartmentReference(
        Long deptId,
        String deptName
    ) {
    }

    public record LeaderSummary(
        Long userId,
        String userName,
        String nickName
    ) {
    }

    public record ChildDepartmentSummary(
        Long deptId,
        String deptName,
        String status
    ) {
    }

}
