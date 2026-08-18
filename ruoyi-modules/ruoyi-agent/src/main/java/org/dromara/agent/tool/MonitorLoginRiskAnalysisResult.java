package org.dromara.agent.tool;

import java.util.Date;
import java.util.List;

/**
 * Structured payload returned by {@code monitor_login_risk_analysis}.
 */
public record MonitorLoginRiskAnalysisResult(
    int days,
    long totalCount,
    long successCount,
    long failureCount,
    double failureRatePercent,
    List<FailureRank> failureUsers,
    List<FailureRank> failureIpAddresses,
    List<DeviceDistribution> deviceDistribution,
    List<FailureDetail> recentFailures
) {

    public record FailureRank(String item, long failureCount) {
    }

    public record DeviceDistribution(String deviceType, long loginCount) {
    }

    public record FailureDetail(
        Long loginId,
        String userName,
        String ipAddress,
        String loginLocation,
        String deviceType,
        String browser,
        String operatingSystem,
        String message,
        Date loginTime
    ) {
    }

}
