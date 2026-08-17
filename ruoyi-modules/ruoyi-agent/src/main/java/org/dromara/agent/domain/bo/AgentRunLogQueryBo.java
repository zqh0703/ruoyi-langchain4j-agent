package org.dromara.agent.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * Agent run log query request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentRunLogQueryBo extends BaseEntity {

    private Long agentId;

    private Long sessionId;

    private String provider;

    private String modelName;

    private String status;

}
