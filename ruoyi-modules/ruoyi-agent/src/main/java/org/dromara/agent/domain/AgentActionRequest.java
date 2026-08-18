package org.dromara.agent.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * A user-confirmed Agent write action.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_action_request")
public class AgentActionRequest extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private Long sessionId;
    private Long agentId;
    private Long runLogId;
    private Long toolMessageId;
    private String toolCode;
    private String riskLevel;
    private String status;
    private String requestKey;
    private String argumentsJson;
    private String previewJson;
    private String summary;
    private String resultJson;
    private String errorCode;
    private String errorMessage;
    private Date expiresAt;
    private Long confirmedBy;
    private Date confirmedTime;
    private Date startedTime;
    private Date finishedTime;
    private Long durationMs;

    @Version
    private Integer version;
}
