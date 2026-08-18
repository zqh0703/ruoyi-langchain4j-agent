package org.dromara.agent.tool;

import java.util.List;

/**
 * Safe role candidates returned when the Agent needs to discover assignable roles.
 */
public record SystemRoleSearchResult(
    long total,
    List<RoleSummary> roles
) {

    public record RoleSummary(
        Long roleId,
        String roleName,
        String roleKey,
        String status,
        String dataScope
    ) {
    }
}
