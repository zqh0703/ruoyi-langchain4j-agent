package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.action.command.SystemUserCreateCommand;
import org.dromara.agent.action.handler.SystemUserCreateActionHandler;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemUserCreateTool implements AgentToolProvider {

    private final AgentActionToolSupport actionSupport;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = SystemUserCreateActionHandler.TOOL_CODE,
        value = "Prepare creation of a normal system user. Resolve one exact department, at least one role, "
            + "and optional posts. This only creates a confirmation proposal; never claim the user exists until confirmed."
    )
    public String createUser(
        @P("Unique login username, 2 to 30 characters") String userName,
        @P("Display nickname, at most 30 characters") String nickName,
        @P(value = "Exact department ID; provide either this or deptName", required = false) Long deptId,
        @P(value = "Exact unique department name; provide either this or deptId", required = false) String deptName,
        @P(value = "Role IDs to assign; may be combined with roleNames", required = false) List<Long> roleIds,
        @P(value = "Exact unique role names to assign; at least one role is required", required = false) List<String> roleNames,
        @P(value = "Optional post IDs in the selected department", required = false) List<Long> postIds,
        @P(value = "Optional exact post names in the selected department", required = false) List<String> postNames
    ) {
        toolGuard.check(this);
        SystemUserCreateCommand command = new SystemUserCreateCommand(
            userName, nickName, deptId, deptName, roleIds, roleNames, postIds, postNames
        );
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("userName", userName);
        arguments.put("nickName", nickName);
        arguments.put("deptId", deptId);
        arguments.put("deptName", deptName);
        arguments.put("roleIds", roleIds);
        arguments.put("roleNames", roleNames);
        arguments.put("postIds", postIds);
        arguments.put("postNames", postNames);
        return actionSupport.prepare(toolCode(), command, arguments);
    }

    @Override
    public String toolCode() {
        return SystemUserCreateActionHandler.TOOL_CODE;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:user:add", "system:dept:query", "system:role:query", "system:post:list");
    }
}
