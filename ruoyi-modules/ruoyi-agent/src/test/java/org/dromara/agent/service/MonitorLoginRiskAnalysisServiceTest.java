package org.dromara.agent.service;

import org.dromara.agent.mapper.AgentMonitorAnalysisMapper;
import org.dromara.agent.tool.AgentExecutionContext;
import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.MonitorLoginRiskAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("local")
class MonitorLoginRiskAnalysisServiceTest {

    private AgentMonitorAnalysisMapper mapper;
    private AgentExecutionContext executionContext;
    private MonitorLoginRiskAnalysisService analysisService;

    @BeforeEach
    void setUp() {
        mapper = mock(AgentMonitorAnalysisMapper.class);
        executionContext = mock(AgentExecutionContext.class);
        when(executionContext.currentTenantId()).thenReturn("000000");
        analysisService = new MonitorLoginRiskAnalysisService(mapper, executionContext);
    }

    @Test
    void shouldRejectInvalidLoginStatus() {
        AgentToolResult<MonitorLoginRiskAnalysisResult> result =
            analysisService.analyze(7, null, null, "LOCKED", 10);

        assertFalse(result.success());
        assertEquals("INVALID_ARGUMENT", result.code());
        verify(mapper, never()).selectLoginSummary(any(), any(), any(), any(), any());
    }

    @Test
    void shouldCalculateLoginRiskMetricsAndRankings() {
        AgentMonitorAnalysisMapper.LoginSummaryRow summary = new AgentMonitorAnalysisMapper.LoginSummaryRow();
        summary.setTotalCount(10);
        summary.setSuccessCount(6);
        summary.setFailureCount(4);
        when(mapper.selectLoginSummary(anyString(), any(), any(), any(), any())).thenReturn(summary);

        AgentMonitorAnalysisMapper.LoginFailureRankRow userRank =
            new AgentMonitorAnalysisMapper.LoginFailureRankRow();
        userRank.setItem("admin");
        userRank.setFailureCount(4);
        when(mapper.selectLoginFailureUsers(anyString(), any(), any(), any(), any()))
            .thenReturn(List.of(userRank));
        when(mapper.selectLoginFailureIps(anyString(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(mapper.selectLoginDevices(anyString(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(mapper.selectRecentLoginFailures(anyString(), any(), any(), any(), any(), anyInt()))
            .thenReturn(List.of());

        AgentToolResult<MonitorLoginRiskAnalysisResult> result =
            analysisService.analyze(7, null, null, "ALL", 10);

        assertTrue(result.success());
        assertEquals(40.0, result.data().failureRatePercent());
        assertEquals("admin", result.data().failureUsers().get(0).item());
    }

}
