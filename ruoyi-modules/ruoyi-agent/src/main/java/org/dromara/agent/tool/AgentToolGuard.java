package org.dromara.agent.tool;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Applies the same Sa-Token permissions used by the existing controllers.
 */
@Component
public class AgentToolGuard {

    public boolean isAllowed(AgentToolProvider provider) {
        return provider.requiredPermissions().stream().allMatch(StpUtil::hasPermission);
    }

    public void check(AgentToolProvider provider) {
        check(provider.requiredPermissions());
    }

    public void check(List<String> permissions) {
        for (String permission : permissions) {
            StpUtil.checkPermission(permission);
        }
    }

}
