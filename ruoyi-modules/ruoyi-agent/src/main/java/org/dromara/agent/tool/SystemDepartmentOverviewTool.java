package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.service.SystemDepartmentOverviewService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j adapter for the department overview use case.
 */
@Component
@RequiredArgsConstructor
public class SystemDepartmentOverviewTool implements AgentToolProvider {

    public static final String TOOL_CODE = "system_department_overview";

    private final SystemDepartmentOverviewService overviewService;
    private final AgentToolJsonSerializer jsonSerializer;
    private final AgentToolCallRecorder toolCallRecorder;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = TOOL_CODE,
        value = "Get one exact department overview, including parent, leader, direct child departments, normal and "
            + "disabled user counts, and post count. Metrics can optionally include all descendant departments."
    )
    public String getDepartmentOverview(
        @P(value = "Exact department ID. Provide deptId or deptName.", required = false)
        Long deptId,
        @P(value = "Exact department name. Provide deptId or deptName.", required = false)
        String deptName,
        @P(value = "Whether user and post metrics include all descendant departments. Defaults to false.", required = false)
        Boolean includeChildren
    ) {
        toolGuard.check(this);
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("deptId", deptId);
        arguments.put("deptName", deptName);
        arguments.put("includeChildren", includeChildren);
        return toolCallRecorder.record(
            TOOL_CODE,
            arguments,
            () -> jsonSerializer.serialize(overviewService.getOverview(deptId, deptName, includeChildren))
        );
    }

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:dept:query", "system:user:list", "system:post:list");
    }

}
