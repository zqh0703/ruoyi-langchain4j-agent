package org.dromara.agent.action;

/**
 * Stable business error surfaced to the model and confirmation UI.
 */
public class AgentActionException extends RuntimeException {

    private final String code;

    public AgentActionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
