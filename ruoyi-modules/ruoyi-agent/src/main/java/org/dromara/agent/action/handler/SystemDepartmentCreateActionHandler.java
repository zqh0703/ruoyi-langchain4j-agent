package org.dromara.agent.action.handler;

import lombok.RequiredArgsConstructor;
import org.dromara.agent.action.AgentActionException;
import org.dromara.agent.action.AgentActionExecutionResult;
import org.dromara.agent.action.AgentActionHandler;
import org.dromara.agent.action.AgentActionPreview;
import org.dromara.agent.action.AgentActionRiskLevel;
import org.dromara.agent.action.AgentActionTargetResolver;
import org.dromara.agent.action.command.SystemDepartmentCreateCommand;
import org.dromara.agent.tool.AgentToolGuard;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysDeptService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SystemDepartmentCreateActionHandler implements AgentActionHandler<SystemDepartmentCreateCommand> {

    public static final String TOOL_CODE = "system_department_create";

    private final AgentActionTargetResolver targetResolver;
    private final ISysDeptService deptService;
    private final AgentToolGuard toolGuard;

    @Override
    public String toolCode() {
        return TOOL_CODE;
    }

    @Override
    public AgentActionRiskLevel riskLevel() {
        return AgentActionRiskLevel.MEDIUM;
    }

    @Override
    public List<String> requiredPermissions() {
        return List.of("system:dept:add", "system:dept:query");
    }

    @Override
    public Class<SystemDepartmentCreateCommand> commandType() {
        return SystemDepartmentCreateCommand.class;
    }

    @Override
    public AgentActionPreview preview(SystemDepartmentCreateCommand command) {
        String deptName = normalizeName(command.deptName());
        int orderNum = normalizeOrder(command.orderNum());
        SysDeptVo parent = targetResolver.department(command.parentId(), command.parentName());
        if (!"0".equals(parent.getStatus())) {
            throw new AgentActionException("INVALID_ARGUMENT", "The parent department is disabled");
        }
        SysUserVo leader = resolveLeader(command);
        ensureUnique(parent.getDeptId(), deptName);

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("parentDepartment", Map.of("id", parent.getDeptId(), "name", parent.getDeptName()));
        preview.put("departmentName", deptName);
        preview.put("orderNum", orderNum);
        if (leader != null) {
            preview.put("leader", Map.of("id", leader.getUserId(), "userName", leader.getUserName(), "nickName", leader.getNickName()));
        }
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("parentId", String.valueOf(parent.getDeptId()));
        expected.put("parentStatus", parent.getStatus());
        expected.put("nameAvailable", true);
        if (leader != null) {
            expected.put("leaderUserId", String.valueOf(leader.getUserId()));
            expected.put("leaderStatus", leader.getStatus());
        }
        return AgentActionPreview.required(
            "Create department " + deptName + " under " + parent.getDeptName(), preview, expected
        );
    }

    @Override
    public AgentActionExecutionResult execute(SystemDepartmentCreateCommand command, AgentActionPreview prepared) {
        AgentActionPreview current = preview(command);
        if (!current.expectedState().equals(prepared.expectedState())) {
            throw new AgentActionException("STALE_STATE", "Department creation targets changed after the preview was created");
        }
        SysDeptVo parent = targetResolver.department(command.parentId(), command.parentName());
        SysUserVo leader = resolveLeader(command);
        SysDeptBo dept = new SysDeptBo();
        dept.setParentId(parent.getDeptId());
        dept.setDeptName(normalizeName(command.deptName()));
        dept.setOrderNum(normalizeOrder(command.orderNum()));
        dept.setLeader(leader == null ? null : leader.getUserId());
        dept.setStatus("0");
        if (deptService.insertDept(dept) != 1) {
            throw new AgentActionException("EXECUTION_FAILED", "The department could not be created");
        }

        SysDeptBo query = new SysDeptBo();
        query.setParentId(parent.getDeptId());
        query.setDeptName(dept.getDeptName());
        SysDeptVo created = deptService.selectDeptList(query).stream()
            .filter(value -> Objects.equals(parent.getDeptId(), value.getParentId()))
            .filter(value -> dept.getDeptName().equals(value.getDeptName()))
            .findFirst().orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("departmentName", dept.getDeptName());
        result.put("parentId", parent.getDeptId());
        result.put("parentName", parent.getDeptName());
        result.put("status", "NORMAL");
        if (created != null) {
            result.put("departmentId", created.getDeptId());
        }
        if (leader != null) {
            result.put("leaderUserId", leader.getUserId());
            result.put("leaderUserName", leader.getUserName());
        }
        return AgentActionExecutionResult.success(result);
    }

    private SysUserVo resolveLeader(SystemDepartmentCreateCommand command) {
        boolean hasId = command.leaderUserId() != null;
        boolean hasName = command.leaderUserName() != null && !command.leaderUserName().isBlank();
        if (!hasId && !hasName) {
            return null;
        }
        toolGuard.check(List.of("system:user:query"));
        SysUserVo leader = targetResolver.user(command.leaderUserId(), command.leaderUserName());
        if (!"0".equals(leader.getStatus())) {
            throw new AgentActionException("INVALID_ARGUMENT", "A disabled user cannot be the department leader");
        }
        return leader;
    }

    private void ensureUnique(Long parentId, String deptName) {
        SysDeptBo candidate = new SysDeptBo();
        candidate.setParentId(parentId);
        candidate.setDeptName(deptName);
        if (!deptService.checkDeptNameUnique(candidate)) {
            throw new AgentActionException("ALREADY_EXISTS", "A department with this name already exists under the selected parent");
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 30) {
            throw new AgentActionException("INVALID_ARGUMENT", "Department name is required and cannot exceed 30 characters");
        }
        return name.trim();
    }

    private int normalizeOrder(Integer orderNum) {
        int normalized = orderNum == null ? 0 : orderNum;
        if (normalized < 0 || normalized > 9999) {
            throw new AgentActionException("INVALID_ARGUMENT", "orderNum must be between 0 and 9999");
        }
        return normalized;
    }
}
