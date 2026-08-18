package org.dromara.agent.action;

import org.dromara.agent.domain.vo.AgentActionVo;

import java.util.Map;

/**
 * Result returned by a write tool before user confirmation.
 */
public record AgentActionPreparation(
    String code,
    String message,
    AgentActionVo action,
    Map<String, Object> preview
) {
}
