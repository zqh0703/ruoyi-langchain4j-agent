package org.dromara.agent.tool;

import java.util.List;

/**
 * Safe, structured payload returned by {@code system_role_overview}.
 */
public record SystemRoleOverviewResult(
    Long roleId,
    String roleName,
    String roleKey,
    String status,
    String dataScope,
    String dataScopeDescription,
    long memberCount,
    List<MemberSummary> members,
    int permissionCount,
    List<String> menuPermissions
) {

    public record MemberSummary(
        Long userId,
        String userName,
        String nickName,
        String deptName,
        String status
    ) {
    }

}
