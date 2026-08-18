package org.dromara.agent.service;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.mapper.AgentMonitorAnalysisMapper;
import org.dromara.agent.tool.AgentExecutionContext;
import org.dromara.agent.tool.AgentToolParameters;
import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.MonitorLoginRiskAnalysisResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes fixed aggregate queries for login-risk analysis.
 */
@Service
@RequiredArgsConstructor
public class MonitorLoginRiskAnalysisService {

    private final AgentMonitorAnalysisMapper analysisMapper;
    private final AgentExecutionContext executionContext;

    public AgentToolResult<MonitorLoginRiskAnalysisResult> analyze(
        Integer days,
        String userName,
        String ipAddress,
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

        String normalizedUserName = AgentToolParameters.trimToNull(userName);
        String normalizedIpAddress = AgentToolParameters.trimToNull(ipAddress);
        int safeDetailLimit = AgentToolParameters.normalizeLimit(detailLimit);
        Date startTime = new Date(System.currentTimeMillis() - safeDays * 86_400_000L);
        String tenantId = executionContext.currentTenantId();

        AgentMonitorAnalysisMapper.LoginSummaryRow summary = analysisMapper.selectLoginSummary(
            tenantId, startTime, normalizedUserName, normalizedIpAddress, statusFilter.databaseValue
        );
        long total = summary == null ? 0 : summary.getTotalCount();
        long success = summary == null ? 0 : summary.getSuccessCount();
        long failure = summary == null ? 0 : summary.getFailureCount();

        List<MonitorLoginRiskAnalysisResult.FailureRank> failureUsers = analysisMapper
            .selectLoginFailureUsers(
                tenantId, startTime, normalizedUserName, normalizedIpAddress, statusFilter.databaseValue
            ).stream()
            .map(row -> new MonitorLoginRiskAnalysisResult.FailureRank(row.getItem(), row.getFailureCount()))
            .toList();
        List<MonitorLoginRiskAnalysisResult.FailureRank> failureIps = analysisMapper
            .selectLoginFailureIps(
                tenantId, startTime, normalizedUserName, normalizedIpAddress, statusFilter.databaseValue
            ).stream()
            .map(row -> new MonitorLoginRiskAnalysisResult.FailureRank(row.getItem(), row.getFailureCount()))
            .toList();
        List<MonitorLoginRiskAnalysisResult.DeviceDistribution> devices = analysisMapper
            .selectLoginDevices(
                tenantId, startTime, normalizedUserName, normalizedIpAddress, statusFilter.databaseValue
            ).stream()
            .map(row -> new MonitorLoginRiskAnalysisResult.DeviceDistribution(
                row.getDeviceType(), row.getLoginCount()
            ))
            .toList();
        List<MonitorLoginRiskAnalysisResult.FailureDetail> recentFailures = analysisMapper
            .selectRecentLoginFailures(
                tenantId, startTime, normalizedUserName, normalizedIpAddress,
                statusFilter.databaseValue, safeDetailLimit
            ).stream()
            .map(row -> new MonitorLoginRiskAnalysisResult.FailureDetail(
                row.getInfoId(), row.getUserName(), row.getIpAddress(), row.getLoginLocation(),
                row.getDeviceType(), row.getBrowser(), row.getOs(), truncate(row.getMessage(), 300),
                row.getLoginTime()
            ))
            .toList();

        MonitorLoginRiskAnalysisResult data = new MonitorLoginRiskAnalysisResult(
            safeDays,
            total,
            success,
            failure,
            percentage(failure, total),
            failureUsers,
            failureIps,
            devices,
            recentFailures
        );

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("status", statusFilter.name());
        if (normalizedUserName != null) {
            metadata.put("userName", normalizedUserName);
        }
        if (normalizedIpAddress != null) {
            metadata.put("ipAddress", normalizedIpAddress);
        }
        metadata.put("detailLimit", safeDetailLimit);
        return AgentToolResult.success("Login risk analysis completed", data, metadata);
    }

    private double percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private String truncate(String value, int limit) {
        return value == null || value.length() <= limit ? value : value.substring(0, limit);
    }

    private enum StatusFilter {
        SUCCESS("0"),
        FAILED("1"),
        ALL(null);

        private final String databaseValue;

        StatusFilter(String databaseValue) {
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
