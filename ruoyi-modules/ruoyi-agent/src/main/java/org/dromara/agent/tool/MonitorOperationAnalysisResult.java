package org.dromara.agent.tool;

import java.util.Date;
import java.util.List;

/**
 * Structured payload returned by {@code monitor_operation_analysis}.
 */
public record MonitorOperationAnalysisResult(
    int days,
    long totalCount,
    long successCount,
    long failureCount,
    double failureRatePercent,
    double averageCostTimeMs,
    long maxCostTimeMs,
    List<ModuleFailureRank> failureModules,
    List<FailureDetail> recentFailures
) {

    public record ModuleFailureRank(String moduleName, long failureCount) {
    }

    public record FailureDetail(
        Long operationId,
        String moduleName,
        String operatorName,
        String ipAddress,
        String errorMessage,
        Long costTimeMs,
        Date operationTime
    ) {
    }

}
