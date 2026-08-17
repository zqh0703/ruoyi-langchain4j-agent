package org.dromara.agent.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.domain.bo.AgentSessionCreateBo;
import org.dromara.agent.domain.vo.AgentSessionVo;
import org.dromara.agent.service.AgentConversationService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent session endpoints.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/session")
public class AgentSessionController {

    private final AgentConversationService agentConversationService;

    @SaCheckPermission("agent:chat:list")
    @GetMapping("/list")
    public TableDataInfo<AgentSessionVo> list(PageQuery pageQuery) {
        return agentConversationService.querySessionPage(pageQuery);
    }

    @SaCheckPermission("agent:chat:send")
    @PostMapping
    public R<AgentSessionVo> create(@Valid @RequestBody AgentSessionCreateBo bo) {
        return R.ok(agentConversationService.createSession(bo));
    }

}
