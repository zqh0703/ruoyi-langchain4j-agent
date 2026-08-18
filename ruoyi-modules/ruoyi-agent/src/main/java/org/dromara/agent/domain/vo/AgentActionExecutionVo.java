package org.dromara.agent.domain.vo;

/**
 * Action execution response. The secret value is never persisted or logged.
 */
public record AgentActionExecutionVo(
    AgentActionVo action,
    String secretType,
    String secretValue
) {
}
