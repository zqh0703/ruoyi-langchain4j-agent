package org.dromara.agent.action;

import java.util.Map;

/**
 * Sanitized action preview and stale-state snapshot.
 */
public record AgentActionPreview(
    boolean actionRequired,
    String summary,
    Map<String, Object> preview,
    Map<String, Object> expectedState
) {

    public AgentActionPreview {
        preview = preview == null ? Map.of() : Map.copyOf(preview);
        expectedState = expectedState == null ? Map.of() : Map.copyOf(expectedState);
    }

    public static AgentActionPreview required(
        String summary,
        Map<String, Object> preview,
        Map<String, Object> expectedState
    ) {
        return new AgentActionPreview(true, summary, preview, expectedState);
    }

    public static AgentActionPreview noChange(String summary, Map<String, Object> preview) {
        return new AgentActionPreview(false, summary, preview, Map.of());
    }
}
