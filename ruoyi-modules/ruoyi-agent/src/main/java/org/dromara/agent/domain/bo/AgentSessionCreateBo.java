package org.dromara.agent.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Create Agent session request.
 */
@Data
public class AgentSessionCreateBo {

    @NotNull(message = "Agent ID不能为空")
    private Long agentId;

    private String title;

}
