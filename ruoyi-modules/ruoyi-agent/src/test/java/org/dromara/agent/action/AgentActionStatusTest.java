package org.dromara.agent.action;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
class AgentActionStatusTest {

    @Test
    void shouldAllowOnlyTwoPhaseLifecycleTransitions() {
        assertTrue(AgentActionStatus.PENDING_CONFIRMATION.canTransitionTo(AgentActionStatus.EXECUTING));
        assertTrue(AgentActionStatus.PENDING_CONFIRMATION.canTransitionTo(AgentActionStatus.CANCELLED));
        assertTrue(AgentActionStatus.PENDING_CONFIRMATION.canTransitionTo(AgentActionStatus.EXPIRED));
        assertTrue(AgentActionStatus.EXECUTING.canTransitionTo(AgentActionStatus.SUCCESS));
        assertTrue(AgentActionStatus.EXECUTING.canTransitionTo(AgentActionStatus.FAILED));

        assertFalse(AgentActionStatus.PENDING_CONFIRMATION.canTransitionTo(AgentActionStatus.SUCCESS));
        assertFalse(AgentActionStatus.SUCCESS.canTransitionTo(AgentActionStatus.EXECUTING));
        assertFalse(AgentActionStatus.CANCELLED.canTransitionTo(AgentActionStatus.EXECUTING));
        assertFalse(AgentActionStatus.EXPIRED.canTransitionTo(AgentActionStatus.EXECUTING));
    }
}
