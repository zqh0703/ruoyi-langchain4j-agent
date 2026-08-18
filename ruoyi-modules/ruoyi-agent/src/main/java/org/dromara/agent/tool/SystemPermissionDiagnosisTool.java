package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.service.SystemPermissionDiagnosisService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j adapter for the permission diagnosis use case.
 */
@Component
@RequiredArgsConstructor
public class SystemPermissionDiagnosisTool implements AgentToolProvider {

    public static final String TOOL_CODE = "system_permission_diagnosis";

    private final SystemPermissionDiagnosisService diagnosisService;
    private final AgentToolJsonSerializer jsonSerializer;
    private final AgentToolCallRecorder toolCallRecorder;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = TOOL_CODE,
        value = "Diagnose whether one exact user can access one exact permission or menu. Returns authorization "
            + "status, source roles, menu status, blocking reasons, and candidate roles that can grant access."
    )
    public String diagnosePermission(
        @P(value = "Exact user ID. Provide userId or userName.", required = false)
        Long userId,
        @P(value = "Exact login username. Provide userId or userName.", required = false)
        String userName,
        @P(value = "Exact permission code, such as system:user:list. Provide this or menuName.", required = false)
        String permissionCode,
        @P(value = "Exact menu name. Provide this or permissionCode.", required = false)
        String menuName
    ) {
        toolGuard.check(this);
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("userId", userId);
        arguments.put("userName", userName);
        arguments.put("permissionCode", permissionCode);
        arguments.put("menuName", menuName);
        return toolCallRecorder.record(
            TOOL_CODE,
            arguments,
            () -> jsonSerializer.serialize(diagnosisService.diagnose(userId, userName, permissionCode, menuName))
        );
    }

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:user:query", "system:role:query", "system:menu:list");
    }

}
