package org.dromara.agent.action;

import java.util.Map;

/**
 * Sanitized persisted result plus an optional one-time response secret.
 */
public record AgentActionExecutionResult(
    Map<String, Object> result,
    String secretType,
    String secretValue
) {

    public AgentActionExecutionResult {
        result = result == null ? Map.of() : Map.copyOf(result);
    }

    public static AgentActionExecutionResult success(Map<String, Object> result) {
        return new AgentActionExecutionResult(result, null, null);
    }

    public static AgentActionExecutionResult withSecret(
        Map<String, Object> result,
        String secretType,
        String secretValue
    ) {
        return new AgentActionExecutionResult(result, secretType, secretValue);
    }
}
