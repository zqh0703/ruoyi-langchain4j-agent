package org.dromara.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
class AgentToolResultTest {

    @Test
    void shouldSerializeSuccessfulResult() {
        AgentToolResult<Map<String, Object>> result = AgentToolResult.success(
            "Query completed",
            Map.of("total", 3),
            Map.of("truncated", false)
        );

        String json = new AgentToolJsonSerializer(new ObjectMapper()).serialize(result);

        assertTrue(result.success());
        assertEquals("OK", result.code());
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"total\":3"));
    }

    @Test
    void shouldBuildBusinessFailure() {
        AgentToolResult<Void> result = AgentToolResult.failure(
            "INVALID_ARGUMENT",
            "days must be between 1 and 30"
        );

        assertFalse(result.success());
        assertEquals("INVALID_ARGUMENT", result.code());
        assertEquals(Map.of(), result.metadata());
    }

}
