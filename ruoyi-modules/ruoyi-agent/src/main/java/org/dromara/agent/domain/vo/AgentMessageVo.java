package org.dromara.agent.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.agent.domain.AgentMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Agent message response.
 */
@Data
@AutoMapper(target = AgentMessage.class)
public class AgentMessageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private Long agentId;
    private Long runLogId;
    private String role;
    private String content;
    private String toolName;
    private String toolArgs;
    private String toolResult;
    private String toolStatus;
    private Long toolDurationMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer seq;
    private Date createTime;
}
