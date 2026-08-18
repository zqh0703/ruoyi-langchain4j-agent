package org.dromara.agent.action.command;

import java.util.List;

public record SystemUserCreateCommand(
    String userName,
    String nickName,
    Long deptId,
    String deptName,
    List<Long> roleIds,
    List<String> roleNames,
    List<Long> postIds,
    List<String> postNames
) {
}
