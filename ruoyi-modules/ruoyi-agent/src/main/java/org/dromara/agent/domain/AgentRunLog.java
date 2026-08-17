package org.dromara.agent.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * Agent model invocation log agent_run_log.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_run_log")
public class AgentRunLog extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long agentId;

    private Long sessionId;

    private String provider;

    private String modelName;

    private String requestBody;

    private String responseBody;

    /** 0: success, 1: failed. */
    private String status;

    private String errorMsg;

    private Long durationMs;

}
