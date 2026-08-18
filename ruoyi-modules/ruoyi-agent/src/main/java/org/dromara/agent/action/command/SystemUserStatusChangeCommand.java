package org.dromara.agent.action.command;

public record SystemUserStatusChangeCommand(
    Long userId,
    String userName,
    String targetStatus,
    String reason
) {
}
