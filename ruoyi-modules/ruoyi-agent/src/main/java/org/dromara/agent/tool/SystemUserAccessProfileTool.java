package org.dromara.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.service.SystemUserAccessProfileService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j adapter for the user access profile use case.
 */
@Component
@RequiredArgsConstructor
public class SystemUserAccessProfileTool implements AgentToolProvider {

    public static final String TOOL_CODE = "system_user_access_profile";

    private final SystemUserAccessProfileService profileService;
    private final AgentToolJsonSerializer jsonSerializer;
    private final AgentToolCallRecorder toolCallRecorder;
    private final AgentToolGuard toolGuard;

    @Tool(
        name = TOOL_CODE,
        value = "Get the access profile of one exact system user, including department, posts, roles, account "
            + "status, and effective menu permissions. Use this to explain what access a user currently has."
    )
    public String getUserAccessProfile(
        @P(value = "Exact user ID. Provide userId or userName.", required = false)
        Long userId,
        @P(value = "Exact login username. Provide userId or userName.", required = false)
        String userName
    ) {
        toolGuard.check(this);
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("userId", userId);
        arguments.put("userName", userName);
        return toolCallRecorder.record(
            TOOL_CODE,
            arguments,
            () -> jsonSerializer.serialize(profileService.getProfile(userId, userName))
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
