package org.dromara.agent.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * Agent conversation session agent_session.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_session")
public class AgentSession extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long agentId;

    private String title;

    /** 0: active, 1: archived. */
    private String status;

    private Date lastMessageTime;

    private String remark;

}
