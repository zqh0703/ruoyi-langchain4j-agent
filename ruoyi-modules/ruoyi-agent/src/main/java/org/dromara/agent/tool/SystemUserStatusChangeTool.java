package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.action.command.SystemUserStatusChangeCommand;
import org.dromara.agent.action.handler.SystemUserStatusChangeActionHandler;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemUserStatusChangeTool implements AgentToolProvider {

    private final AgentActionToolSupport actionSupport;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = SystemUserStatusChangeActionHandler.TOOL_CODE,
        value = "Prepare enabling or disabling one exact system account. A reason is mandatory. "
            + "This only creates a high-risk confirmation proposal and does not immediately modify the account."
    )
    public String changeStatus(
        @P(value = "Exact user ID; provide either this or userName", required = false) Long userId,
        @P(value = "Exact username; provide either this or userId", required = false) String userName,
        @P("Target status: NORMAL or DISABLED") String targetStatus,
        @P("Business reason for changing the account status, at most 200 characters") String reason
    ) {
        toolGuard.check(this);
        SystemUserStatusChangeCommand command = new SystemUserStatusChangeCommand(
            userId, userName, targetStatus, reason
        );
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("userId", userId);
        arguments.put("userName", userName);
        arguments.put("targetStatus", targetStatus);
        arguments.put("reason", reason);
        return actionSupport.prepare(toolCode(), command, arguments);
    }

    @Override
    public String toolCode() {
        return SystemUserStatusChangeActionHandler.TOOL_CODE;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:user:edit");
    }
}
