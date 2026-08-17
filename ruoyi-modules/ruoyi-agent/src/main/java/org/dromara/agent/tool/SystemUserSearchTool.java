package org.dromara.agent.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Searches users through the existing tenant and data-permission aware mappers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemUserSearchTool implements AgentToolProvider {

    public static final String TOOL_CODE = "system_user_search";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final AgentToolCallRecorder toolCallRecorder;

    @Tool(
        name = TOOL_CODE,
        value = "Search system users visible to the current operator. Use this when the user asks which users exist, "
            + "requests user details, or filters users by department, username, nickname, or account status."
    )
    public String searchUsers(
        @P(value = "Department name or partial department name, for example R&D or 研发. Omit when no department filter is needed.", required = false)
        String deptName,
        @P(value = "Partial username or nickname. Omit when no name filter is needed.", required = false)
        String keyword,
        @P(value = "Account status: 0 means normal and 1 means disabled. Omit to search normal users.", required = false)
        String status,
        @P(value = "Maximum number of users to return, from 1 to 20. Omit to use 10.", required = false)
        Integer limit
    ) {
        Map<String, Object> toolArguments = new LinkedHashMap<>();
        toolArguments.put("deptName", deptName);
        toolArguments.put("keyword", keyword);
        toolArguments.put("status", status);
        toolArguments.put("limit", limit);
        return toolCallRecorder.record(TOOL_CODE, toolArguments, () -> {
        String normalizedDeptName = trimToNull(deptName);
        String normalizedKeyword = trimToNull(keyword);
        String normalizedStatus = normalizeStatus(status);
        int safeLimit = normalizeLimit(limit);

        Map<Long, String> departmentNames = new LinkedHashMap<>();
        List<Long> departmentIds = List.of();
        if (normalizedDeptName != null) {
            List<SysDeptVo> departments = sysDeptMapper.selectDeptList(
                Wrappers.lambdaQuery(SysDept.class)
                    .select(SysDept::getDeptId, SysDept::getDeptName)
                    .like(SysDept::getDeptName, normalizedDeptName)
                    .eq(SysDept::getStatus, "0")
                    .orderByAsc(SysDept::getOrderNum)
            );
            if (departments.isEmpty()) {
                return "Query completed. No visible active department matched: " + normalizedDeptName;
            }
            departmentNames.putAll(departments.stream().collect(Collectors.toMap(
                SysDeptVo::getDeptId,
                SysDeptVo::getDeptName,
                (left, right) -> left,
                LinkedHashMap::new
            )));
            departmentIds = List.copyOf(departmentNames.keySet());
        }

        var userQuery = Wrappers.lambdaQuery(SysUser.class)
            .select(
                SysUser::getUserId,
                SysUser::getDeptId,
                SysUser::getUserName,
                SysUser::getNickName,
                SysUser::getStatus,
                SysUser::getCreateTime
            )
            .eq(SysUser::getStatus, normalizedStatus)
            .in(!departmentIds.isEmpty(), SysUser::getDeptId, departmentIds)
            .and(normalizedKeyword != null, query -> query
                .like(SysUser::getUserName, normalizedKeyword)
                .or()
                .like(SysUser::getNickName, normalizedKeyword))
            .orderByAsc(SysUser::getUserId);

        Page<SysUserVo> userPage = sysUserMapper.selectPageUserList(
            new Page<>(1, safeLimit, false),
            userQuery
        );
        List<SysUserVo> users = userPage.getRecords();
        if (users.isEmpty()) {
            return "Query completed. No users matched the supplied filters.";
        }

        if (normalizedDeptName == null) {
            List<Long> userDeptIds = users.stream()
                .map(SysUserVo::getDeptId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
            if (!userDeptIds.isEmpty()) {
                departmentNames.putAll(sysDeptMapper.selectDeptList(
                    Wrappers.lambdaQuery(SysDept.class)
                        .select(SysDept::getDeptId, SysDept::getDeptName)
                        .in(SysDept::getDeptId, userDeptIds)
                ).stream().collect(Collectors.toMap(
                    SysDeptVo::getDeptId,
                    SysDeptVo::getDeptName,
                    (left, right) -> left,
                    LinkedHashMap::new
                )));
            }
        }

        StringBuilder result = new StringBuilder("Query completed. Matched ")
            .append(users.size())
            .append(" user(s).\n");
        for (SysUserVo user : users) {
            result.append("- userId=").append(user.getUserId())
                .append(", username=").append(user.getUserName())
                .append(", nickname=").append(user.getNickName())
                .append(", department=").append(departmentNames.getOrDefault(user.getDeptId(), "unknown"))
                .append(", status=").append("0".equals(user.getStatus()) ? "normal" : "disabled")
                .append(", createdAt=").append(user.getCreateTime())
                .append('\n');
        }

        log.info("Agent tool executed: {}, deptName={}, keyword={}, status={}, limit={}, matched={}",
            TOOL_CODE, normalizedDeptName, normalizedKeyword, normalizedStatus, safeLimit, users.size());
        return result.toString().trim();
        });
    }

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null || "0".equals(normalized)
            || "normal".equalsIgnoreCase(normalized) || "active".equalsIgnoreCase(normalized)
            || "正常".equals(normalized)) {
            return "0";
        }
        if ("1".equals(normalized) || "disabled".equalsIgnoreCase(normalized)
            || "inactive".equalsIgnoreCase(normalized) || "停用".equals(normalized)) {
            return "1";
        }
        return "0";
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

}
