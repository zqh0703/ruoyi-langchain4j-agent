package org.dromara.agent.tool;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("local")
class AgentToolParametersTest {

    @Test
    void shouldNormalizeListLimits() {
        assertEquals(10, AgentToolParameters.normalizeLimit(null));
        assertEquals(1, AgentToolParameters.normalizeLimit(0));
        assertEquals(20, AgentToolParameters.normalizeLimit(100));
        assertEquals(8, AgentToolParameters.normalizeLimit(8));
    }

    @Test
    void shouldValidateAnalysisDays() {
        assertEquals(7, AgentToolParameters.validateAnalysisDays(null));
        assertEquals(30, AgentToolParameters.validateAnalysisDays(30));
        assertThrows(IllegalArgumentException.class, () -> AgentToolParameters.validateAnalysisDays(0));
        assertThrows(IllegalArgumentException.class, () -> AgentToolParameters.validateAnalysisDays(31));
    }

    @Test
    void shouldTrimOptionalText() {
        assertNull(AgentToolParameters.trimToNull("  "));
        assertEquals("R&D", AgentToolParameters.trimToNull("  R&D  "));
    }

}
