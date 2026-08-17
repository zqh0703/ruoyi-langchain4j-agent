package org.dromara.agent.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.domain.bo.AgentChatTestBo;
import org.dromara.agent.provider.DeepSeekChatModelFactory;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent model connectivity endpoints.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/chat")
public class AgentChatController {

    private final DeepSeekChatModelFactory deepSeekChatModelFactory;

    /** Tests the LangChain4j to DeepSeek connection with the configured default model. */
    @SaCheckPermission("agent:chat:send")
    @Log(title = "Agent模型连通性测试", businessType = BusinessType.OTHER)
    @RepeatSubmit
    @PostMapping("/test")
    public R<String> test(@Valid @RequestBody AgentChatTestBo bo) {
        return R.ok(deepSeekChatModelFactory.createDefault().chat(bo.getMessage()));
    }

}
