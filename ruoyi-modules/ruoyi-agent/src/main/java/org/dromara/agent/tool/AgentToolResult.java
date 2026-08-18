package org.dromara.agent.tool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured result returned by every Agent business tool.
 */
public record AgentToolResult<T>(
    boolean success,
    String code,
    String message,
    T data,
    Map<String, Object> metadata
) {

    public AgentToolResult {
        metadata = metadata == null || metadata.isEmpty()
            ? Map.of()
            : Map.copyOf(new LinkedHashMap<>(metadata));
    }

    public static <T> AgentToolResult<T> success(String message, T data) {
        return success(message, data, Map.of());
    }

    public static <T> AgentToolResult<T> success(String message, T data, Map<String, Object> metadata) {
        return new AgentToolResult<>(true, "OK", message, data, metadata);
    }

    public static <T> AgentToolResult<T> failure(String code, String message) {
        return failure(code, message, null, Map.of());
    }

    public static <T> AgentToolResult<T> failure(String code, String message, T data) {
        return failure(code, message, data, Map.of());
    }

    public static <T> AgentToolResult<T> failure(
        String code,
        String message,
        T data,
        Map<String, Object> metadata
    ) {
        return new AgentToolResult<>(false, code, message, data, metadata);
    }


}
