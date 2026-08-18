package org.dromara.agent.service;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.tool.AgentToolParameters;
import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemRoleSearchResult;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.system.domain.bo.SysRoleBo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.service.ISysRoleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Discovers visible roles before a write action requires an exact role target.
 */
@Service
@RequiredArgsConstructor
public class SystemRoleSearchService {

    private final ISysRoleService roleService;

    public AgentToolResult<SystemRoleSearchResult> search(String keyword, String status, Integer limit) {
        String normalizedKeyword = AgentToolParameters.trimToNull(keyword);
        String normalizedStatus = normalizeStatus(status);
        if (normalizedStatus == null) {
            return AgentToolResult.failure(
                "INVALID_ARGUMENT",
                "status must be NORMAL, DISABLED, or ALL"
            );
        }

        int safeLimit = AgentToolParameters.normalizeLimit(limit);
        SysRoleBo query = new SysRoleBo();
        if (!"ALL".equals(normalizedStatus)) {
            query.setStatus("NORMAL".equals(normalizedStatus) ? SystemConstants.NORMAL : SystemConstants.DISABLE);
        }

        List<SysRoleVo> matches = safeList(roleService.selectRoleList(query)).stream()
            .filter(role -> matchesKeyword(role, normalizedKeyword))
            .toList();
        List<SystemRoleSearchResult.RoleSummary> roles = matches.stream()
            .limit(safeLimit)
            .map(this::toSummary)
            .toList();

        return AgentToolResult.success(
            "Role search completed",
            new SystemRoleSearchResult(matches.size(), roles),
            Map.of(
                "truncated", matches.size() > roles.size(),
                "filters", Map.of(
                    "keyword", normalizedKeyword == null ? "" : normalizedKeyword,
                    "status", normalizedStatus,
                    "limit", safeLimit
                )
            )
        );
    }

    private boolean matchesKeyword(SysRoleVo role, String keyword) {
        if (keyword == null) {
            return true;
        }
        String lowered = keyword.toLowerCase(Locale.ROOT);
        return contains(role.getRoleName(), lowered) || contains(role.getRoleKey(), lowered);
    }

    private boolean contains(String value, String loweredKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(loweredKeyword);
    }

    private SystemRoleSearchResult.RoleSummary toSummary(SysRoleVo role) {
        return new SystemRoleSearchResult.RoleSummary(
            role.getRoleId(),
            role.getRoleName(),
            role.getRoleKey(),
            SystemConstants.NORMAL.equals(role.getStatus()) ? "NORMAL" : "DISABLED",
            role.getDataScope()
        );
    }

    private String normalizeStatus(String status) {
        String normalized = AgentToolParameters.trimToNull(status);
        if (normalized == null) {
            return "NORMAL";
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "NORMAL", "DISABLED", "ALL" -> normalized;
            default -> null;
        };
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
