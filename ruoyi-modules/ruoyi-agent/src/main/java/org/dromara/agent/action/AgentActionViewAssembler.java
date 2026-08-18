package org.dromara.agent.action;

import com.fasterxml.jackson.core.type.TypeReference;
import org.dromara.agent.domain.AgentActionRequest;
import org.dromara.agent.domain.vo.AgentActionVo;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Converts persisted action JSON into safe UI fields.
 */
@Component
public class AgentActionViewAssembler {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    public AgentActionVo toVo(AgentActionRequest action) {
        AgentActionVo vo = new AgentActionVo();
        vo.setId(action.getId());
        vo.setSessionId(action.getSessionId());
        vo.setAgentId(action.getAgentId());
        vo.setRunLogId(action.getRunLogId());
        vo.setToolMessageId(action.getToolMessageId());
        vo.setToolCode(action.getToolCode());
        vo.setRiskLevel(action.getRiskLevel());
        vo.setStatus(action.getStatus());
        vo.setSummary(action.getSummary());
        vo.setErrorCode(action.getErrorCode());
        vo.setErrorMessage(action.getErrorMessage());
        vo.setExpiresAt(action.getExpiresAt());
        vo.setConfirmedBy(action.getConfirmedBy());
        vo.setConfirmedTime(action.getConfirmedTime());
        vo.setStartedTime(action.getStartedTime());
        vo.setFinishedTime(action.getFinishedTime());
        vo.setDurationMs(action.getDurationMs());
        vo.setVersion(action.getVersion());
        vo.setCreateTime(action.getCreateTime());
        AgentActionPreview preview = JsonUtils.parseObject(action.getPreviewJson(), AgentActionPreview.class);
        vo.setPreview(preview == null ? Map.of() : preview.preview());
        vo.setResult(parseMap(action.getResultJson()));
        return vo;
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        Map<String, Object> value = JsonUtils.parseObject(json, MAP_TYPE);
        return value == null ? Map.of() : value;
    }
}
