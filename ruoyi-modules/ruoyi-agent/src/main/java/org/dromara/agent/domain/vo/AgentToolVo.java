package org.dromara.agent.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Built-in Agent tool metadata.
 */
@Data
@AllArgsConstructor
public class AgentToolVo {

    private String name;
    private String description;
}
