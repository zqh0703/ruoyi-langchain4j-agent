package org.dromara.agent.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent model connectivity test request.
 */
@Data
public class AgentChatTestBo {

    /** Test prompt sent to the configured default model. */
    @NotBlank(message = "测试消息不能为空")
    private String message;

}
