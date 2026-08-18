package org.dromara.agent.service;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.action.AgentActionExecutionResult;
import org.dromara.agent.action.AgentActionHandler;
import org.dromara.agent.action.AgentActionRegistry;
import org.dromara.agent.action.AgentActionStatus;
import org.dromara.agent.action.AgentActionViewAssembler;
import org.dromara.agent.domain.AgentActionRequest;
import org.dromara.agent.domain.vo.AgentActionExecutionVo;
import org.dromara.agent.mapper.AgentActionRequestMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Executes a claimed business action and commits its success state atomically.
 */
@Service
@RequiredArgsConstructor
public class AgentActionExecutionService {

    private final AgentActionRequestMapper actionMapper;
    private final AgentActionRegistry actionRegistry;
    private final AgentActionViewAssembler viewAssembler;

    @Transactional(rollbackFor = Exception.class)
    public AgentActionExecutionVo execute(Long actionId) {
        AgentActionRequest action = actionMapper.selectById(actionId);
        if (action == null || !AgentActionStatus.EXECUTING.name().equals(action.getStatus())) {
            throw new ServiceException("Action is not in EXECUTING state");
        }

        AgentActionHandler<?> handler = actionRegistry.require(action.getToolCode());
        AgentActionExecutionResult execution = handler.executeJson(
            action.getArgumentsJson(), action.getPreviewJson()
        );
        Date finishedAt = new Date();
        action.setStatus(AgentActionStatus.SUCCESS.name());
        action.setResultJson(JsonUtils.toJsonString(execution.result()));
        action.setErrorCode(null);
        action.setErrorMessage(null);
        action.setFinishedTime(finishedAt);
        action.setDurationMs(Math.max(0L, finishedAt.getTime() - action.getStartedTime().getTime()));
        if (actionMapper.updateById(action) != 1) {
            throw new ServiceException("Action state changed while committing the result");
        }
        AgentActionRequest committed = actionMapper.selectById(actionId);
        return new AgentActionExecutionVo(
            viewAssembler.toVo(committed), execution.secretType(), execution.secretValue()
        );
    }
}
