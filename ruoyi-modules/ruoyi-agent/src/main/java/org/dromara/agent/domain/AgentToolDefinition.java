package org.dromara.agent.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * Metadata for a tool that can be exposed to an Agent.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_tool_definition")
public class AgentToolDefinition extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String toolCode;
    private String toolName;
    private String description;
    private String category;
    private String riskLevel;
    private String requireConfirmation;
    private String status;
    private String remark;

}
