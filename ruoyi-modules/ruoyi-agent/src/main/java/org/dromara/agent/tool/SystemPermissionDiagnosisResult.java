package org.dromara.agent.tool;

import java.util.List;

/**
 * Safe, structured payload returned by {@code system_permission_diagnosis}.
 */
public record SystemPermissionDiagnosisResult(
    UserSummary user,
    MenuSummary menu,
    String permissionCode,
    boolean authorized,
    List<RoleSummary> sourceRoles,
    List<String> blockingReasons,
    List<RoleSummary> candidateRoles
) {

    public record UserSummary(
        Long userId,
        String userName,
        String nickName,
        String status
    ) {
    }

    public record MenuSummary(
        Long menuId,
        String menuName,
        String menuType,
        String status,
        String visible
    ) {
    }

    public record RoleSummary(
        Long roleId,
        String roleName,
        String roleKey,
        String status
    ) {
    }

}
