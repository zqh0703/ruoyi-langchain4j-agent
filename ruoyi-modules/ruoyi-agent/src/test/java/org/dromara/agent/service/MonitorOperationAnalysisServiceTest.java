package org.dromara.agent.service;

import org.dromara.agent.mapper.AgentMonitorAnalysisMapper;
import org.dromara.agent.tool.AgentExecutionContext;
import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.MonitorOperationAnalysisResult;
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
class MonitorOperationAnalysisServiceTest {

    private AgentMonitorAnalysisMapper mapper;
    private AgentExecutionContext executionContext;
    private MonitorOperationAnalysisService analysisService;

    @BeforeEach
    void setUp() {
        mapper = mock(AgentMonitorAnalysisMapper.class);
        executionContext = mock(AgentExecutionContext.class);
        when(executionContext.currentTenantId()).thenReturn("000000");
        analysisService = new MonitorOperationAnalysisService(mapper, executionContext);
    }

    @Test
    void shouldRejectAnalysisWindowOverThirtyDays() {
        AgentToolResult<MonitorOperationAnalysisResult> result =
            analysisService.analyze(31, null, null, null, 10);

        assertFalse(result.success());
        assertEquals("INVALID_ARGUMENT", result.code());
        verify(mapper, never()).selectOperationSummary(any(), any(), any(), any(), any());
    }

    @Test
    void shouldCalculateOperationMetricsFromFixedAggregateRows() {
        AgentMonitorAnalysisMapper.OperationSummaryRow summary =
            new AgentMonitorAnalysisMapper.OperationSummaryRow();
        summary.setTotalCount(20);
        summary.setSuccessCount(15);
        summary.setFailureCount(5);
        summary.setAverageCostTime(12.345);
        summary.setMaxCostTime(80L);
        when(mapper.selectOperationSummary(anyString(), any(), any(), any(), any())).thenReturn(summary);

        AgentMonitorAnalysisMapper.OperationModuleFailureRow rank =
            new AgentMonitorAnalysisMapper.OperationModuleFailureRow();
        rank.setModuleName("User management");
        rank.setFailureCount(5);
        when(mapper.selectOperationFailureModules(anyString(), any(), any(), any(), any()))
            .thenReturn(List.of(rank));
        when(mapper.selectRecentOperationFailures(anyString(), any(), any(), any(), any(), anyInt()))
            .thenReturn(List.of());

        AgentToolResult<MonitorOperationAnalysisResult> result =
            analysisService.analyze(7, null, null, "ALL", 10);

        assertTrue(result.success());
        assertEquals(25.0, result.data().failureRatePercent());
        assertEquals(12.35, result.data().averageCostTimeMs());
        assertEquals("User management", result.data().failureModules().get(0).moduleName());
    }

}
