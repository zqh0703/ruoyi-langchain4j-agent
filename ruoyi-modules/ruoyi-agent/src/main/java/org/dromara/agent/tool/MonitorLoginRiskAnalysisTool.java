package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.service.MonitorLoginRiskAnalysisService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j adapter for login-risk analysis.
 */
@Component
@RequiredArgsConstructor
public class MonitorLoginRiskAnalysisTool implements AgentToolProvider {

    public static final String TOOL_CODE = "monitor_login_risk_analysis";

    private final MonitorLoginRiskAnalysisService analysisService;
    private final AgentToolJsonSerializer jsonSerializer;
    private final AgentToolCallRecorder toolCallRecorder;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = TOOL_CODE,
        value = "Analyze login activity for up to 30 days. Returns success and failure counts, failure user and IP "
            + "rankings, device distribution, and recent failed logins without tokens or credentials."
    )
    public String analyzeLoginRisk(
        @P(value = "Number of days from 1 to 30. Defaults to 7.", required = false)
        Integer days,
        @P(value = "Partial login username.", required = false)
        String userName,
        @P(value = "Exact IP address.", required = false)
        String ipAddress,
        @P(value = "Login status: SUCCESS, FAILED, or ALL. Defaults to ALL.", required = false)
        String status,
        @P(value = "Maximum recent failures to return, from 1 to 20. Defaults to 10.", required = false)
        Integer detailLimit
    ) {
        toolGuard.check(this);
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("days", days);
        arguments.put("userName", userName);
        arguments.put("ipAddress", ipAddress);
        arguments.put("status", status);
        arguments.put("detailLimit", detailLimit);
        return toolCallRecorder.record(
            TOOL_CODE,
            arguments,
            () -> jsonSerializer.serialize(
                analysisService.analyze(days, userName, ipAddress, status, detailLimit)
            )
        );
    }

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("monitor:logininfor:list");
    }

}
