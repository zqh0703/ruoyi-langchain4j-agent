package org.dromara.agent.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.agent.domain.AgentRunLog;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Agent run log response.
 */
@Data
@AutoMapper(target = AgentRunLog.class)
public class AgentRunLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long agentId;
    private Long sessionId;
    private String provider;
    private String modelName;
    private String requestBody;
    private String responseBody;
    private String status;
    private String errorMsg;
    private Long durationMs;
    private Date createTime;
}
