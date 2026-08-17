package org.dromara.agent.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.domain.bo.AgentRunLogQueryBo;
import org.dromara.agent.domain.vo.AgentRunLogVo;
import org.dromara.agent.service.AgentConversationService;
import org.dromara.agent.domain.vo.AgentRunTraceVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent run log endpoints.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/run")
public class AgentRunLogController {

    private final AgentConversationService agentConversationService;

    @SaCheckPermission("agent:run:list")
    @GetMapping("/list")
    public TableDataInfo<AgentRunLogVo> list(AgentRunLogQueryBo bo, PageQuery pageQuery) {
        return agentConversationService.queryRunLogPage(bo, pageQuery);
    }

    @SaCheckPermission("agent:run:query")
    @GetMapping("/{id}")
    public R<AgentRunLogVo> getInfo(@NotNull(message = "执行记录ID不能为空") @PathVariable Long id) {
        return R.ok(agentConversationService.queryRunLogById(id));
    }

    @SaCheckPermission("agent:run:query")
    @GetMapping("/{id}/trace")
    public R<AgentRunTraceVo> getTrace(@NotNull(message = "Run log ID is required") @PathVariable Long id) {
        return R.ok(agentConversationService.queryRunTraceById(id));
    }

}
