package org.dromara.agent.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Agent 配置对象 agent_config
 *
 * @author Codex
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_config")
public class AgentConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Agent ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * Agent编码
     */
    private String agentCode;

    /**
     * 模型供应商
     */
    private String provider;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 温度参数
     */
    private BigDecimal temperature;

    /**
     * 最大输出Token
     */
    private Integer maxTokens;

    /**
     * 是否启用工具（0否 1是）
     */
    private String enableTool;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
