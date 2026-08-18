package org.dromara.agent.tool;

import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Component;

/**
 * Isolates the Agent services from static authentication context access.
 */
@Component
public class AgentExecutionContext {

    public Long currentUserId() {
        return LoginHelper.getUserId();
    }

    public String currentTenantId() {
        return LoginHelper.getTenantId();
    }

}
