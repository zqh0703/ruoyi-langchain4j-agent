package org.dromara.agent.service;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.mapper.AgentMonitorAnalysisMapper;
import org.dromara.agent.tool.AgentExecutionContext;
import org.dromara.agent.tool.AgentToolParameters;
import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.MonitorOperationAnalysisResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes fixed aggregate queries for operation-log analysis.
 */
@Service
@RequiredArgsConstructor
public class MonitorOperationAnalysisService {

    private final AgentMonitorAnalysisMapper analysisMapper;
    private final AgentExecutionContext executionContext;

    public AgentToolResult<MonitorOperationAnalysisResult> analyze(
        Integer days,
        String moduleName,
        String operatorName,
        String status,
        Integer detailLimit
    ) {
        int safeDays;
        try {
            safeDays = AgentToolParameters.validateAnalysisDays(days);
        } catch (IllegalArgumentException error) {
            return AgentToolResult.failure("INVALID_ARGUMENT", error.getMessage());
        }
        StatusFilter statusFilter = StatusFilter.parse(status);
        if (statusFilter == null) {
            return AgentToolResult.failure("INVALID_ARGUMENT", "status must be SUCCESS, FAILED, or ALL");
        }

        String normalizedModule = AgentToolParameters.trimToNull(moduleName);
        String normalizedOperator = AgentToolParameters.trimToNull(operatorName);
        int safeDetailLimit = AgentToolParameters.normalizeLimit(detailLimit);
        Date startTime = new Date(System.currentTimeMillis() - safeDays * 86_400_000L);
        String tenantId = executionContext.currentTenantId();

        AgentMonitorAnalysisMapper.OperationSummaryRow summary = analysisMapper.selectOperationSummary(
            tenantId, startTime, normalizedModule, normalizedOperator, statusFilter.databaseValue
        );
        long total = summary == null ? 0 : summary.getTotalCount();
        long success = summary == null ? 0 : summary.getSuccessCount();
        long failure = summary == null ? 0 : summary.getFailureCount();

        List<MonitorOperationAnalysisResult.ModuleFailureRank> modules = analysisMapper
            .selectOperationFailureModules(
                tenantId, startTime, normalizedModule, normalizedOperator, statusFilter.databaseValue
            ).stream()
            .map(row -> new MonitorOperationAnalysisResult.ModuleFailureRank(
                row.getModuleName(), row.getFailureCount()
            ))
            .toList();
        List<MonitorOperationAnalysisResult.FailureDetail> failures = analysisMapper
            .selectRecentOperationFailures(
                tenantId, startTime, normalizedModule, normalizedOperator,
                statusFilter.databaseValue, safeDetailLimit
            ).stream()
            .map(row -> new MonitorOperationAnalysisResult.FailureDetail(
                row.getOperId(), row.getModuleName(), row.getOperatorName(), row.getIpAddress(),
                truncate(row.getErrorMessage(), 500), row.getCostTime(), row.getOperationTime()
            ))
            .toList();

        MonitorOperationAnalysisResult data = new MonitorOperationAnalysisResult(
            safeDays,
            total,
            success,
            failure,
            percentage(failure, total),
            summary == null || summary.getAverageCostTime() == null
                ? 0
                : round(summary.getAverageCostTime()),
            summary == null || summary.getMaxCostTime() == null ? 0 : summary.getMaxCostTime(),
            modules,
            failures
        );

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("status", statusFilter.name());
        if (normalizedModule != null) {
            metadata.put("moduleName", normalizedModule);
        }
        if (normalizedOperator != null) {
            metadata.put("operatorName", normalizedOperator);
        }
        metadata.put("detailLimit", safeDetailLimit);
        return AgentToolResult.success("Operation log analysis completed", data, metadata);
    }

    private double percentage(long numerator, long denominator) {
        return denominator == 0 ? 0 : round(numerator * 100.0 / denominator);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String truncate(String value, int limit) {
        return value == null || value.length() <= limit ? value : value.substring(0, limit);
    }

    private enum StatusFilter {
        SUCCESS(0),
        FAILED(1),
        ALL(null);

        private final Integer databaseValue;

        StatusFilter(Integer databaseValue) {
            this.databaseValue = databaseValue;
        }

        private static StatusFilter parse(String value) {
            String normalized = AgentToolParameters.trimToNull(value);
            if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
                return ALL;
            }
            if ("0".equals(normalized) || "SUCCESS".equalsIgnoreCase(normalized)) {
                return SUCCESS;
            }
            if ("1".equals(normalized) || "FAILED".equalsIgnoreCase(normalized)) {
                return FAILED;
            }
            return null;
        }
    }

}
