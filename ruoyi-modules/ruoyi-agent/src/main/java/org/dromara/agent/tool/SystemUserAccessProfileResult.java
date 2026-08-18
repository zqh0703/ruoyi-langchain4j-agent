package org.dromara.agent.tool;

import java.util.List;

/**
 * Safe, structured payload returned by {@code system_user_access_profile}.
 */
public record SystemUserAccessProfileResult(
    Long userId,
    String userName,
    String nickName,
    String accountStatus,
    DepartmentSummary department,
    List<PostSummary> posts,
    List<RoleSummary> roles,
    int permissionCount,
    List<String> permissions
) {

    public record DepartmentSummary(
        Long deptId,
        String deptName,
        Long parentId,
        String parentName
    ) {
    }

    public record PostSummary(
        Long postId,
        String postCode,
        String postName,
        String status
    ) {
    }

    public record RoleSummary(
        Long roleId,
        String roleName,
        String roleKey,
        String status,
        String dataScope
    ) {
    }

}
