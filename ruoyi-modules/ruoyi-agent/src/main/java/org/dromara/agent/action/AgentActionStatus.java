package org.dromara.agent.action;

import java.util.Set;

/**
 * Persistent action lifecycle states.
 */
public enum AgentActionStatus {
    PENDING_CONFIRMATION,
    EXECUTING,
    SUCCESS,
    FAILED,
    CANCELLED,
    EXPIRED;

    public boolean canTransitionTo(AgentActionStatus target) {
        return switch (this) {
            case PENDING_CONFIRMATION -> Set.of(EXECUTING, CANCELLED, EXPIRED).contains(target);
            case EXECUTING -> Set.of(SUCCESS, FAILED).contains(target);
            case SUCCESS, FAILED, CANCELLED, EXPIRED -> false;
        };
    }
}