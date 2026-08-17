package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
/**
 * The first business tool exposed during incremental Agent development.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemUserCountTool implements AgentToolProvider {

    public static final String TOOL_CODE = "system_user_count";

    private final SystemAgentTools systemAgentTools;
    private final AgentToolCallRecorder toolCallRecorder;

    @Tool(
        name = TOOL_CODE,
        value = "Count active, non-deleted users in the current tenant. Use this tool instead of guessing the count."
    )
    public String countNormalUsers() {
        String result = toolCallRecorder.record(TOOL_CODE, Map.of(), systemAgentTools::systemUserCount);
        log.info("Agent tool executed: system_user_count, result={}", result);
        return result;
    }

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

}
