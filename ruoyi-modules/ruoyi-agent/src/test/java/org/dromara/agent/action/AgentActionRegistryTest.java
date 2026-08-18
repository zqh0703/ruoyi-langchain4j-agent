package org.dromara.agent.action;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("local")
class AgentActionRegistryTest {

    @Test
    void shouldResolveHandlerByStableToolCode() {
        AgentActionHandler<String> handler = handler("system_user_create");
        AgentActionRegistry registry = new AgentActionRegistry(List.of(handler));

        assertEquals(handler, registry.require("system_user_create"));
        assertEquals("HANDLER_NOT_FOUND", assertThrows(
            AgentActionException.class, () -> registry.require("missing")
        ).code());
    }

    @Test
    void shouldRejectDuplicateHandlers() {
        assertThrows(IllegalStateException.class, () -> new AgentActionRegistry(List.of(
            handler("duplicate"), handler("duplicate")
        )));
    }

    private AgentActionHandler<String> handler(String code) {
        return new AgentActionHandler<>() {
            @Override
            public String toolCode() {
                return code;
            }

            @Override
            public AgentActionRiskLevel riskLevel() {
                return AgentActionRiskLevel.MEDIUM;
            }

            @Override
            public List<String> requiredPermissions() {
                return List.of("test:action");
            }

            @Override
            public Class<String> commandType() {
                return String.class;
            }

            @Override
            public AgentActionPreview preview(String command) {
                return AgentActionPreview.required(command, Map.of(), Map.of());
            }

            @Override
            public AgentActionExecutionResult execute(String command, AgentActionPreview preview) {
                return AgentActionExecutionResult.success(Map.of("command", command));
            }
        };
    }
}
