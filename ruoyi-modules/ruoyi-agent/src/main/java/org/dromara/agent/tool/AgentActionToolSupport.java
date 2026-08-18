package org.dromara.agent.tool;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.action.AgentActionException;
import org.dromara.agent.action.AgentActionPreparation;
import org.dromara.agent.service.AgentActionService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared adapter that turns write-tool calls into persisted confirmation proposals.
 */
@Component
@RequiredArgsConstructor
public class AgentActionToolSupport {

    private final AgentActionService actionService;
    private final AgentToolJsonSerializer jsonSerializer;
    private final AgentToolCallRecorder toolCallRecorder;

    public String prepare(String toolCode, Object command, Map<String, Object> safeArguments) {
        AgentActionPreparation preparation;
        try {
            preparation = actionService.prepare(toolCode, command);
        } catch (AgentActionException businessError) {
            return toolCallRecorder.record(toolCode, safeArguments, () -> jsonSerializer.serialize(
                AgentToolResult.failure(businessError.code(), businessError.getMessage())
            ));
        } catch (RuntimeException error) {
            return toolCallRecorder.record(toolCode, safeArguments, () -> {
                throw error;
            });
        }

        Long actionId = preparation.action() == null ? null : preparation.action().getId();
        return toolCallRecorder.recordAction(toolCode, safeArguments, actionId, () -> {
            Map<String, Object> data = new LinkedHashMap<>();
            if (preparation.action() != null) {
                data.put("action", preparation.action());
            }
            data.put("preview", preparation.preview());
            AgentToolResult<Map<String, Object>> result = new AgentToolResult<>(
                true,
                preparation.code(),
                preparation.message(),
                data,
                Map.of("requiresUserConfirmation", preparation.action() != null)
            );
            return jsonSerializer.serialize(result);
        });
    }
}
