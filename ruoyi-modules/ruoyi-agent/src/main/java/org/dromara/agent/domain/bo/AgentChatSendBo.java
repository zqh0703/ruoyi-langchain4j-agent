package org.dromara.agent.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Send Agent message request.
 */
@Data
public class AgentChatSendBo {

    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotBlank(message = "消息不能为空")
    private String message;

}
