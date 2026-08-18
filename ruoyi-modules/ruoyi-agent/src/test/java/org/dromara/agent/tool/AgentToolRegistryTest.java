package org.dromara.agent.tool;

import org.dromara.agent.domain.AgentConfigTool;
import org.dromara.agent.domain.AgentToolDefinition;
import org.dromara.agent.mapper.AgentConfigToolMapper;
import org.dromara.agent.mapper.AgentToolDefinitionMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("local")
class AgentToolRegistryTest {

    @Test
    void shouldOnlyExposeToolsAllowedForCurrentOperator() {
        AgentToolDefinitionMapper definitionMapper = mock(AgentToolDefinitionMapper.class);
        AgentConfigToolMapper configToolMapper = mock(AgentConfigToolMapper.class);
        AgentToolGuard guard = mock(AgentToolGuard.class);
        AgentToolProvider provider = new TestToolProvider();

        AgentConfigTool assignment = new AgentConfigTool();
        assignment.setToolId(1L);
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setId(1L);
        definition.setToolCode(provider.toolCode());

        when(configToolMapper.selectList(any())).thenReturn(List.of(assignment));
        when(definitionMapper.selectList(any())).thenReturn(List.of(definition));

        AgentToolRegistry registry = new AgentToolRegistry(
            definitionMapper,
            configToolMapper,
            guard,
            List.of(provider)
        );

        when(guard.isAllowed(provider)).thenReturn(false);
        assertTrue(registry.resolveEnabledTools(1L).isEmpty());

        when(guard.isAllowed(provider)).thenReturn(true);
        assertEquals(List.of(provider), registry.resolveEnabledTools(1L));

        definition.setToolCode("missing_provider");
        assertTrue(registry.listAvailableTools().isEmpty());
    }

    private static class TestToolProvider implements AgentToolProvider {

        @Override
        public String toolCode() {
            return "test_tool";
        }

        @Override
        public List<String> requiredPermissions() {
            return List.of("system:test:list");
        }

    }

}
