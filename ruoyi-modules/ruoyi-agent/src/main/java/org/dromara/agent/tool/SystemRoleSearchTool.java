package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.service.SystemRoleSearchService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j adapter for discovering roles before user and role write actions.
 */
@Component
@RequiredArgsConstructor
public class SystemRoleSearchTool implements AgentToolProvider {

    public static final String TOOL_CODE = "system_role_search";

    private final SystemRoleSearchService searchService;
    private final AgentToolJsonSerializer jsonSerializer;
    private final AgentToolCallRecorder toolCallRecorder;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = TOOL_CODE,
        value = "Search or list assignable system roles by role name or role key. Use this before user creation "
            + "or role assignment when the requested exact role is unknown. Pass no keyword to list available roles."
    )
    public String searchRoles(
        @P(value = "Optional fuzzy keyword matched against role name and role key", required = false)
        String keyword,
        @P(value = "Role status: NORMAL, DISABLED, or ALL. Defaults to NORMAL", required = false)
        String status,
        @P(value = "Maximum roles to return, from 1 to 20. Defaults to 10", required = false)
        Integer limit
    ) {
        toolGuard.check(this);
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("keyword", keyword);
        arguments.put("status", status);
        arguments.put("limit", limit);
        return toolCallRecorder.record(
            TOOL_CODE,
            arguments,
            () -> jsonSerializer.serialize(searchService.search(keyword, status, limit))
        );
    }

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:role:list");
    }
}
