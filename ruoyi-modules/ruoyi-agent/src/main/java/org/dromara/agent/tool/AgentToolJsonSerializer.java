package org.dromara.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Component;

/**
 * Serializes structured tool results with the application's configured mapper.
 */
@Component
@RequiredArgsConstructor
public class AgentToolJsonSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(AgentToolResult<?> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException error) {
            throw new ServiceException("Failed to serialize Agent tool result").setDetailMessage(error.getMessage());
        }
    }

}
