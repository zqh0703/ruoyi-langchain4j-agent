package org.dromara.agent.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.domain.vo.AgentMessageVo;
import org.dromara.agent.service.AgentConversationService;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent message endpoints.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/message")
public class AgentMessageController {

    private final AgentConversationService agentConversationService;

    @SaCheckPermission("agent:chat:list")
    @GetMapping("/list/{sessionId}")
    public R<List<AgentMessageVo>> list(@NotNull(message = "会话ID不能为空") @PathVariable Long sessionId) {
        return R.ok(agentConversationService.queryMessages(sessionId));
    }

}
