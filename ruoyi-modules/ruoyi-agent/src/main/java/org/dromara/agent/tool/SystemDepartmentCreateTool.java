package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.action.command.SystemDepartmentCreateCommand;
import org.dromara.agent.action.handler.SystemDepartmentCreateActionHandler;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemDepartmentCreateTool implements AgentToolProvider {

    private final AgentActionToolSupport actionSupport;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = SystemDepartmentCreateActionHandler.TOOL_CODE,
        value = "Prepare creation of a normal department under one exact parent department, with an optional leader. "
            + "This only creates a confirmation proposal and does not immediately create the department."
    )
    public String createDepartment(
        @P(value = "Exact parent department ID; provide either this or parentName", required = false) Long parentId,
        @P(value = "Exact unique parent department name; provide either this or parentId", required = false) String parentName,
        @P("New department name, unique under the parent and at most 30 characters") String deptName,
        @P(value = "Display order from 0 to 9999; defaults to 0", required = false) Integer orderNum,
        @P(value = "Optional exact leader user ID; provide either this or leaderUserName", required = false) Long leaderUserId,
        @P(value = "Optional exact leader username; provide either this or leaderUserId", required = false) String leaderUserName
    ) {
        toolGuard.check(this);
        SystemDepartmentCreateCommand command = new SystemDepartmentCreateCommand(
            parentId, parentName, deptName, orderNum, leaderUserId, leaderUserName
        );
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("parentId", parentId);
        arguments.put("parentName", parentName);
        arguments.put("deptName", deptName);
        arguments.put("orderNum", orderNum);
        arguments.put("leaderUserId", leaderUserId);
        arguments.put("leaderUserName", leaderUserName);
        return actionSupport.prepare(toolCode(), command, arguments);
    }

    @Override
    public String toolCode() {
        return SystemDepartmentCreateActionHandler.TOOL_CODE;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:dept:add", "system:dept:query");
    }
}
