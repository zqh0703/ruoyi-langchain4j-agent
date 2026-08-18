package org.dromara.agent.tool;

import java.util.List;

/**
 * Exposes one LangChain4j tool to the Agent tool registry.
 */
public interface AgentToolProvider {

    String toolCode();

    default List<String> requiredPermissions() {
        return List.of();
    }

    default Object toolInstance() {
        return this;
    }

}
