package org.dromara.agent.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Complete trace for one Agent run.
 */
@Data
public class AgentRunTraceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private AgentRunLogVo runLog;

    private List<AgentMessageVo> messages;

}
