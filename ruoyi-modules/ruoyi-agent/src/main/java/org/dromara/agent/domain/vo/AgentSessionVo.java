package org.dromara.agent.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.agent.domain.AgentSession;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Agent session response.
 */
@Data
@AutoMapper(target = AgentSession.class)
public class AgentSessionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long agentId;
    private String title;
    private String status;
    private Date lastMessageTime;
    private Date createTime;
    private Date updateTime;
}
