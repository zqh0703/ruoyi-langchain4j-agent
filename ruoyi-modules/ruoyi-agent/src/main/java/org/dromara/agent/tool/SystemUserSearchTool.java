package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.service.SystemUserSearchService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j adapter for the structured system user search use case.
 */
@Component
@RequiredArgsConstructor
public class SystemUserSearchTool implements AgentToolProvider {

    public static final String TOOL_CODE = "system_user_search";

    private final SystemUserSearchService searchService;
    private final AgentToolJsonSerializer jsonSerializer;
    private final AgentToolCallRecorder toolCallRecorder;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = TOOL_CODE,
        value = "Search system users visible to the current operator. Supports username or nickname keyword, "
            + "department, role, and account status filters. Use this to answer which users match business conditions."
    )
    public String searchUsers(
        @P(value = "Partial username or nickname. Omit when no name filter is needed.", required = false)
        String keyword,
        @P(value = "Exact or uniquely matching department name. Omit when no department filter is needed.", required = false)
        String deptName,
        @P(value = "Exact or uniquely matching role name. Omit when no role filter is needed.", required = false)
        String roleName,
        @P(value = "Account status: NORMAL, DISABLED, or ALL. Defaults to NORMAL.", required = false)
        String status,
        @P(value = "Maximum number of users to return, from 1 to 20. Defaults to 10.", required = false)
        Integer limit
    ) {
        toolGuard.check(this);
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("keyword", keyword);
        arguments.put("deptName", deptName);
        arguments.put("roleName", roleName);
        arguments.put("status", status);
        arguments.put("limit", limit);
        return toolCallRecorder.record(
            TOOL_CODE,
            arguments,
            () -> jsonSerializer.serialize(searchService.search(keyword, deptName, roleName, status, limit))
        );
    }

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:user:list");
    }

}
