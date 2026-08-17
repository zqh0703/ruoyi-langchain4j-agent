package org.dromara.agent.provider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.config.DeepSeekProperties;
import org.dromara.agent.domain.AgentConfig;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Creates LangChain4j ChatModel instances for DeepSeek-compatible Agents.
 */
@Component
@RequiredArgsConstructor
public class DeepSeekChatModelFactory {

    private final DeepSeekProperties properties;

    /** Creates a model using module defaults. */
    public ChatModel createDefault() {
        return create(null);
    }

    /** Creates a model using an Agent configuration, falling back to module defaults. */
    public ChatModel create(AgentConfig agentConfig) {
        if (!StringUtils.isNotBlank(properties.getApiKey())) {
            throw new ServiceException("未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY");
        }

        String modelName = agentConfig == null || !StringUtils.isNotBlank(agentConfig.getModelName())
            ? properties.getDefaultModel() : agentConfig.getModelName();
        Double temperature = agentConfig == null || agentConfig.getTemperature() == null
            ? properties.getTemperature() : agentConfig.getTemperature().doubleValue();
        Integer maxTokens = agentConfig == null || agentConfig.getMaxTokens() == null
            ? properties.getMaxTokens() : agentConfig.getMaxTokens();

        return OpenAiChatModel.builder()
            .baseUrl(properties.getBaseUrl())
            .apiKey(properties.getApiKey())
            .modelName(modelName)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .timeout(properties.getTimeout())
            .build();
    }

}
