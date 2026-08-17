package org.dromara.agent.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Agent chat response.
 */
@Data
@AllArgsConstructor
public class AgentChatReplyVo {

    private Long sessionId;
    private Long messageId;
    private Long runLogId;
    private String content;
    private Long durationMs;
}
