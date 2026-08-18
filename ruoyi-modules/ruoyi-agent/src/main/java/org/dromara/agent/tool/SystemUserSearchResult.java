package org.dromara.agent.tool;

import java.util.Date;
import java.util.List;

/**
 * Safe, structured payload returned by {@code system_user_search}.
 */
public record SystemUserSearchResult(
    long total,
    List<UserSummary> users
) {

    public record UserSummary(
        Long userId,
        String userName,
        String nickName,
        String deptName,
        String status,
        Date createTime
    ) {
    }

}
