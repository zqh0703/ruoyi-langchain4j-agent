package org.dromara.agent.tool;

import org.dromara.common.core.utils.StringUtils;

/**
 * Shared normalization rules for model-generated tool arguments.
 */
public final class AgentToolParameters {

    public static final int DEFAULT_LIST_LIMIT = 10;
    public static final int MAX_LIST_LIMIT = 20;
    public static final int DEFAULT_ANALYSIS_DAYS = 7;
    public static final int MAX_ANALYSIS_DAYS = 30;

    private AgentToolParameters() {
    }

    public static String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    public static int normalizeLimit(Integer limit) {
        return normalizeLimit(limit, DEFAULT_LIST_LIMIT, MAX_LIST_LIMIT);
    }

    public static int normalizeLimit(Integer limit, int defaultLimit, int maxLimit) {
        if (defaultLimit < 1 || maxLimit < defaultLimit) {
            throw new IllegalArgumentException("Invalid limit bounds");
        }
        if (limit == null) {
            return defaultLimit;
        }
        return Math.max(1, Math.min(limit, maxLimit));
    }

    public static int validateAnalysisDays(Integer days) {
        int normalized = days == null ? DEFAULT_ANALYSIS_DAYS : days;
        if (normalized < 1 || normalized > MAX_ANALYSIS_DAYS) {
            throw new IllegalArgumentException("days must be between 1 and " + MAX_ANALYSIS_DAYS);
        }
        return normalized;
    }

}
