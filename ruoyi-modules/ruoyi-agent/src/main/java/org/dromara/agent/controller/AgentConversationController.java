package org.dromara.agent.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.domain.bo.AgentChatSendBo;
import org.dromara.agent.domain.vo.AgentChatReplyVo;
import org.dromara.agent.service.AgentConversationService;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.TimeUnit;

/**
 * Agent conversation execution endpoint.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/chat")
public class AgentConversationController {

    private final AgentConversationService agentConversationService;

    @SaCheckPermission("agent:chat:send")
    @Log(title = "Agent对话", businessType = BusinessType.OTHER)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS)
    @PostMapping("/send")
    public R<AgentChatReplyVo> send(@Valid @RequestBody AgentChatSendBo bo) {
        return R.ok(agentConversationService.send(bo));
    }

}
