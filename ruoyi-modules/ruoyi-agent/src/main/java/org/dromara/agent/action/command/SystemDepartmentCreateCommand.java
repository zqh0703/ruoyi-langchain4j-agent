package org.dromara.agent.action.command;

public record SystemDepartmentCreateCommand(
    Long parentId,
    String parentName,
    String deptName,
    Integer orderNum,
    Long leaderUserId,
    String leaderUserName
) {
}
