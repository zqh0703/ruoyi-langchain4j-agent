package org.dromara.agent.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.domain.bo.AgentActionDecisionBo;
import org.dromara.agent.domain.vo.AgentActionExecutionVo;
import org.dromara.agent.domain.vo.AgentActionVo;
import org.dromara.agent.service.AgentActionService;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Explicit user decisions for two-phase Agent write actions.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/agent/action")
public class AgentActionController {

    private final AgentActionService actionService;

    @SaCheckPermission("agent:action:list")
    @GetMapping("/list/{sessionId}")
    public R<List<AgentActionVo>> list(
        @NotNull(message = "Session ID is required") @PathVariable Long sessionId
    ) {
        return R.ok(actionService.listBySession(sessionId));
    }

    @SaCheckPermission("agent:action:list")
    @GetMapping("/{id}")
    public R<AgentActionVo> get(@NotNull(message = "Action ID is required") @PathVariable Long id) {
        return R.ok(actionService.get(id));
    }

    @SaCheckPermission("agent:action:confirm")
    @Log(title = "Confirm Agent action", businessType = BusinessType.UPDATE, isSaveResponseData = false)
    @RepeatSubmit(interval = 2000)
    @PostMapping("/{id}/confirm")
    public R<AgentActionExecutionVo> confirm(
        @PathVariable Long id,
        @Valid @RequestBody AgentActionDecisionBo bo
    ) {
        return R.ok(actionService.confirm(id, bo.getVersion()));
    }

    @SaCheckPermission("agent:action:cancel")
    @Log(title = "Cancel Agent action", businessType = BusinessType.UPDATE)
    @RepeatSubmit(interval = 2000)
    @PostMapping("/{id}/cancel")
    public R<AgentActionVo> cancel(
        @PathVariable Long id,
        @Valid @RequestBody AgentActionDecisionBo bo
    ) {
        return R.ok(actionService.cancel(id, bo.getVersion()));
    }
}
