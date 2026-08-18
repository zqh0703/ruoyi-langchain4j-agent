package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.service.SystemRoleOverviewService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j adapter for the role overview use case.
 */
@Component
@RequiredArgsConstructor
public class SystemRoleOverviewTool implements AgentToolProvider {

    public static final String TOOL_CODE = "system_role_overview";

    private final SystemRoleOverviewService overviewService;
    private final AgentToolJsonSerializer jsonSerializer;
    private final AgentToolCallRecorder toolCallRecorder;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = TOOL_CODE,
        value = "Get one exact role overview, including role key, status, data scope, member count, optional "
            + "member details, and effective menu permissions."
    )
    public String getRoleOverview(
        @P(value = "Exact role ID. Provide roleId or roleName.", required = false)
        Long roleId,
        @P(value = "Exact role name. Provide roleId or roleName.", required = false)
        String roleName,
        @P(value = "Whether to return member details. Defaults to false.", required = false)
        Boolean includeMembers,
        @P(value = "Maximum members to return, from 1 to 20. Defaults to 10.", required = false)
        Integer memberLimit
    ) {
        toolGuard.check(this);
        if (Boolean.TRUE.equals(includeMembers)) {
            toolGuard.check(List.of("system:user:list"));
        }
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("roleId", roleId);
        arguments.put("roleName", roleName);
        arguments.put("includeMembers", includeMembers);
        arguments.put("memberLimit", memberLimit);
        return toolCallRecorder.record(
            TOOL_CODE,
            arguments,
            () -> jsonSerializer.serialize(overviewService.getOverview(roleId, roleName, includeMembers, memberLimit))
        );
    }

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:role:query");
    }

}
