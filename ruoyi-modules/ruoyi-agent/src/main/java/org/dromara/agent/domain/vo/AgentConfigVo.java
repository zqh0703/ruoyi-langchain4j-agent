package org.dromara.agent.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.agent.domain.AgentConfig;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Agent 配置视图对象 agent_config
 *
 * @author Codex
 */
@Data
@AutoMapper(target = AgentConfig.class)
public class AgentConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Agent ID
     */
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
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

}
