package org.dromara.agent.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.agent.domain.AgentActionRequest;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Safe action data returned to the chat UI.
 */
@Data
@AutoMapper(target = AgentActionRequest.class)
public class AgentActionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private Long agentId;
    private Long runLogId;
    private Long toolMessageId;
    private String toolCode;
    private String riskLevel;
    private String status;
    private String summary;
    private String errorCode;
    private String errorMessage;
    private Date expiresAt;
    private Long confirmedBy;
    private Date confirmedTime;
    private Date startedTime;
    private Date finishedTime;
    private Long durationMs;
    private Integer version;
    private Date createTime;
    private Map<String, Object> preview;
    private Map<String, Object> result;
}
