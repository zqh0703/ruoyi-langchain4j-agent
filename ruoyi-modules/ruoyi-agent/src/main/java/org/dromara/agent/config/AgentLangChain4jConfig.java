package org.dromara.agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Agent module configuration properties.
 */
@Configuration
@EnableConfigurationProperties(DeepSeekProperties.class)
public class AgentLangChain4jConfig {
}
