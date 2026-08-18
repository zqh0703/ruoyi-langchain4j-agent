package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.action.command.SystemUserRoleAssignCommand;
import org.dromara.agent.action.handler.SystemUserRoleAssignActionHandler;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemUserRoleAssignTool implements AgentToolProvider {

    private final AgentActionToolSupport actionSupport;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = SystemUserRoleAssignActionHandler.TOOL_CODE,
        value = "Prepare adding or removing selected roles for one exact user. This is incremental, never replaces all roles, "
            + "and only creates a high-risk confirmation proposal."
    )
    public String assignRoles(
        @P(value = "Exact user ID; provide either this or userName", required = false) Long userId,
        @P(value = "Exact username; provide either this or userId", required = false) String userName,
        @P("Role operation: ADD or REMOVE") String operation,
        @P(value = "Exact role IDs; may be combined with roleNames", required = false) List<Long> roleIds,
        @P(value = "Exact unique role names; at least one role is required", required = false) List<String> roleNames
    ) {
        toolGuard.check(this);
        SystemUserRoleAssignCommand command = new SystemUserRoleAssignCommand(
            userId, userName, operation, roleIds, roleNames
        );
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("userId", userId);
        arguments.put("userName", userName);
        arguments.put("operation", operation);
        arguments.put("roleIds", roleIds);
        arguments.put("roleNames", roleNames);
        return actionSupport.prepare(toolCode(), command, arguments);
    }

    @Override
    public String toolCode() {
        return SystemUserRoleAssignActionHandler.TOOL_CODE;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:user:edit", "system:role:query");
    }
}
