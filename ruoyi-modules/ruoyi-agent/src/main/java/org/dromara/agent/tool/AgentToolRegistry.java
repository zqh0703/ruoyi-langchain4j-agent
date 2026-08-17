package org.dromara.agent.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.dromara.agent.domain.AgentConfigTool;
import org.dromara.agent.domain.AgentToolDefinition;
import org.dromara.agent.domain.vo.AgentToolVo;
import org.dromara.agent.mapper.AgentConfigToolMapper;
import org.dromara.agent.mapper.AgentToolDefinitionMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves Spring tool providers through the allowlist stored for each Agent.
 */
@Slf4j
@Component
public class AgentToolRegistry {

    private final AgentToolDefinitionMapper definitionMapper;
    private final AgentConfigToolMapper configToolMapper;
    private final Map<String, AgentToolProvider> providerByCode;

    public AgentToolRegistry(AgentToolDefinitionMapper definitionMapper,
                             AgentConfigToolMapper configToolMapper,
                             List<AgentToolProvider> providers) {
        this.definitionMapper = definitionMapper;
        this.configToolMapper = configToolMapper;
        Map<String, AgentToolProvider> providerMap = new LinkedHashMap<>();
        for (AgentToolProvider provider : providers) {
            AgentToolProvider previous = providerMap.putIfAbsent(provider.toolCode(), provider);
            if (previous != null) {
                throw new IllegalStateException("Duplicate Agent tool provider: " + provider.toolCode());
            }
        }
        this.providerByCode = Map.copyOf(providerMap);
    }

    public List<Object> resolveEnabledTools(Long agentId) {
        List<AgentConfigTool> allowlist = configToolMapper.selectList(
            Wrappers.lambdaQuery(AgentConfigTool.class)
                .eq(AgentConfigTool::getAgentId, agentId)
                .eq(AgentConfigTool::getEnabled, "1")
        );
        if (allowlist.isEmpty()) {
            return List.of();
        }

        List<Long> toolIds = allowlist.stream().map(AgentConfigTool::getToolId).toList();
        return definitionMapper.selectList(
                Wrappers.lambdaQuery(AgentToolDefinition.class)
                    .in(AgentToolDefinition::getId, toolIds)
                    .eq(AgentToolDefinition::getStatus, "0")
                    .orderByAsc(AgentToolDefinition::getId)
            ).stream()
            .map(definition -> findProvider(definition.getToolCode()))
            .filter(Objects::nonNull)
            .map(AgentToolProvider::toolInstance)
            .toList();
    }

    public List<AgentToolVo> listAvailableTools() {
        return definitionMapper.selectList(
                Wrappers.lambdaQuery(AgentToolDefinition.class)
                    .eq(AgentToolDefinition::getStatus, "0")
                    .orderByAsc(AgentToolDefinition::getId)
            ).stream()
            .filter(definition -> providerByCode.containsKey(definition.getToolCode()))
            .map(definition -> new AgentToolVo(definition.getToolCode(), definition.getDescription()))
            .toList();
    }

    private AgentToolProvider findProvider(String toolCode) {
        AgentToolProvider provider = providerByCode.get(toolCode);
        if (provider == null) {
            log.warn("Enabled Agent tool has no Spring provider: {}", toolCode);
        }
        return provider;
    }

}
