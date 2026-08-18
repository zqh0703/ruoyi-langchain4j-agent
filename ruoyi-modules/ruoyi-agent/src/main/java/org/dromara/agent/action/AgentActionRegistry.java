package org.dromara.agent.action;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves write action handlers by their stable tool code.
 */
@Component
public class AgentActionRegistry {

    private final Map<String, AgentActionHandler<?>> handlers;

    public AgentActionRegistry(List<AgentActionHandler<?>> handlerList) {
        Map<String, AgentActionHandler<?>> index = new LinkedHashMap<>();
        for (AgentActionHandler<?> handler : handlerList) {
            if (index.putIfAbsent(handler.toolCode(), handler) != null) {
                throw new IllegalStateException("Duplicate Agent action handler: " + handler.toolCode());
            }
        }
        handlers = Map.copyOf(index);
    }

    public AgentActionHandler<?> require(String toolCode) {
        AgentActionHandler<?> handler = handlers.get(toolCode);
        if (handler == null) {
            throw new AgentActionException("HANDLER_NOT_FOUND", "No action handler is registered for " + toolCode);
        }
        return handler;
    }
}
