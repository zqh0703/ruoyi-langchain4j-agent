package org.dromara.agent.tool;

/**
 * Exposes one LangChain4j tool to the Agent tool registry.
 */
public interface AgentToolProvider {

    String toolCode();

    default Object toolInstance() {
        return this;
    }

}
