package org.dromara.agent.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.agent.domain.AgentConfig;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;

/**
 * Agent 配置业务对象 agent_config
 *
 * @author Codex
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = AgentConfig.class, reverseConvertGenerate = false)
public class AgentConfigBo extends BaseEntity {

    /**
     * Agent ID
     */
    @NotNull(message = "Agent ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * Agent名称
     */
    @NotBlank(message = "Agent名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String agentName;

    /**
     * Agent编码
     */
    @NotBlank(message = "Agent编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String agentCode;

    /**
     * 模型供应商
     */
    @NotBlank(message = "模型供应商不能为空", groups = {AddGroup.class, EditGroup.class})
    private String provider;

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空", groups = {AddGroup.class, EditGroup.class})
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
