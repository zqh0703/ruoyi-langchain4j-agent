package org.dromara.agent.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.domain.vo.AgentToolVo;
import org.dromara.agent.service.AgentConversationService;
import org.dromara.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Built-in Agent tool metadata endpoint.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/tool")
public class AgentToolController {

    private final AgentConversationService agentConversationService;

    @SaCheckPermission("agent:chat:list")
    @GetMapping("/list")
    public R<List<AgentToolVo>> list() {
        return R.ok(agentConversationService.listTools());
    }

}
