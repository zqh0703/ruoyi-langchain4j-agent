package org.dromara.agent.action;

import org.dromara.common.json.utils.JsonUtils;

import java.util.List;

/**
 * Typed preview and execution contract for one write operation.
 */
public interface AgentActionHandler<C> {

    String toolCode();

    AgentActionRiskLevel riskLevel();

    List<String> requiredPermissions();

    Class<C> commandType();

    AgentActionPreview preview(C command);

    AgentActionExecutionResult execute(C command, AgentActionPreview preview);

    default AgentActionPreview previewObject(Object command) {
        return preview(commandType().cast(command));
    }

    default AgentActionExecutionResult executeJson(String argumentsJson, String previewJson) {
        C command = JsonUtils.parseObject(argumentsJson, commandType());
        AgentActionPreview actionPreview = JsonUtils.parseObject(previewJson, AgentActionPreview.class);
        return execute(command, actionPreview);
    }
}
