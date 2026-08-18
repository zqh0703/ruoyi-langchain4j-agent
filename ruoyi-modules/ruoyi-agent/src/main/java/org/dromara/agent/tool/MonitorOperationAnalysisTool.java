package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.service.MonitorOperationAnalysisService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j adapter for operation-log analysis.
 */
@Component
@RequiredArgsConstructor
public class MonitorOperationAnalysisTool implements AgentToolProvider {

    public static final String TOOL_CODE = "monitor_operation_analysis";

    private final MonitorOperationAnalysisService analysisService;
    private final AgentToolJsonSerializer jsonSerializer;
    private final AgentToolCallRecorder toolCallRecorder;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = TOOL_CODE,
        value = "Analyze operation logs for up to 30 days. Returns success and failure counts, failure rate, "
            + "average and maximum duration, top failure modules, and recent failures without request payloads."
    )
    public String analyzeOperations(
        @P(value = "Number of days from 1 to 30. Defaults to 7.", required = false)
        Integer days,
        @P(value = "Partial operation module name.", required = false)
        String moduleName,
        @P(value = "Partial operator username.", required = false)
        String operatorName,
        @P(value = "Operation status: SUCCESS, FAILED, or ALL. Defaults to ALL.", required = false)
        String status,
        @P(value = "Maximum recent failures to return, from 1 to 20. Defaults to 10.", required = false)
        Integer detailLimit
    ) {
        toolGuard.check(this);
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("days", days);
        arguments.put("moduleName", moduleName);
        arguments.put("operatorName", operatorName);
        arguments.put("status", status);
        arguments.put("detailLimit", detailLimit);
        return toolCallRecorder.record(
            TOOL_CODE,
            arguments,
            () -> jsonSerializer.serialize(
                analysisService.analyze(days, moduleName, operatorName, status, detailLimit)
            )
        );
    }

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("monitor:operlog:list");
    }

}
