package org.dromara.agent.action.command;

import java.util.List;

public record SystemUserRoleAssignCommand(
    Long userId,
    String userName,
    String operation,
    List<Long> roleIds,
    List<String> roleNames
) {
}
