package org.dromara.agent.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * Tool allowlist entry for one Agent configuration.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_config_tool")
public class AgentConfigTool extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long agentId;
    private Long toolId;
    private String enabled;
    private String configJson;
    private String remark;

}
