package org.dromara.agent.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Optimistic-lock token submitted by an action decision button.
 */
@Data
public class AgentActionDecisionBo {

    @NotNull(message = "Action version is required")
    private Integer version;
}
