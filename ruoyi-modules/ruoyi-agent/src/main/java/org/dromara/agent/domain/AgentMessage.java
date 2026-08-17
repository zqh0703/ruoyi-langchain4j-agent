package org.dromara.agent.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * Agent conversation message agent_message.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_message")
public class AgentMessage extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long sessionId;

    private Long agentId;

    private Long runLogId;

    /** user, assistant, tool or system. */
    private String role;

    private String content;

    private String toolName;

    private String toolArgs;

    private String toolResult;

    private Long toolDurationMs;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer seq;

}
