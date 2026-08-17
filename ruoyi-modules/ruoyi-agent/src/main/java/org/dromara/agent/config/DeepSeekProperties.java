package org.dromara.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * DeepSeek connection settings for the Agent module.
 */
@Data
@ConfigurationProperties(prefix = "agent.deepseek")
public class DeepSeekProperties {

    /** API key resolved from the DEEPSEEK_API_KEY environment variable. */
    private String apiKey;

    /** OpenAI-compatible API base URL. */
    private String baseUrl = "https://api.deepseek.com/v1";

    /** Default model used when an Agent does not override it. */
    private String defaultModel = "deepseek-v4-pro";

    /** Default sampling temperature. */
    private Double temperature = 0.70D;

    /** Default maximum completion token count. */
    private Integer maxTokens = 2048;

    /** Request timeout. */
    private Duration timeout = Duration.ofSeconds(60);

}
