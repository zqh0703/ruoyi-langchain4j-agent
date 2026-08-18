package org.dromara.agent.service;

import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemDepartmentOverviewResult;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.domain.bo.SysPostBo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysDeptService;
import org.dromara.system.service.ISysPostService;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("local")
class SystemDepartmentOverviewServiceTest {

    private ISysDeptService deptService;
    private ISysUserService userService;
    private ISysPostService postService;
    private SystemDepartmentOverviewService overviewService;

    @BeforeEach
    void setUp() {
        deptService = mock(ISysDeptService.class);
        userService = mock(ISysUserService.class);
        postService = mock(ISysPostService.class);
        overviewService = new SystemDepartmentOverviewService(deptService, userService, postService);
    }

    @Test
    void shouldRejectMissingDepartmentIdentifier() {
        AgentToolResult<SystemDepartmentOverviewResult> result = overviewService.getOverview(null, "  ", false);

        assertFalse(result.success());
        assertEquals("INVALID_ARGUMENT", result.code());
        verify(deptService, never()).selectDeptById(any());
    }

    @Test
    void shouldReturnCandidatesForDuplicateExactDepartmentNames() {
        SysDeptVo first = department(10L, "R&D", 1L);
        SysDeptVo second = department(11L, "R&D", 2L);
        when(deptService.selectDeptList(any())).thenReturn(List.of(first, second));

        AgentToolResult<SystemDepartmentOverviewResult> result =
            overviewService.getOverview(null, "R&D", false);

        assertFalse(result.success());
        assertEquals("AMBIGUOUS_TARGET", result.code());
        assertEquals(2, ((List<?>) result.metadata().get("candidates")).size());
        verify(deptService, never()).checkDeptDataScope(any());
    }

    @Test
    void shouldAggregateDepartmentOnlyMetricsWithExactDepartmentFilters() {
        SysDeptVo department = department(103L, "R&D", 100L);
        department.setParentName("Headquarters");
        department.setLeader(7L);
        department.setStatus("0");
        when(deptService.selectDeptById(103L)).thenReturn(department);
        when(deptService.selectDeptList(any())).thenReturn(List.of(
            child(105L, "Platform", "0"),
            child(106L, "Security", "1")
        ));

        SysUserVo leader = new SysUserVo();
        leader.setUserId(7L);
        leader.setUserName("zhangsan");
        leader.setNickName("Zhang San");
        when(userService.selectPageUserList(any(), any())).thenAnswer(invocation -> {
            SysUserBo query = invocation.getArgument(0);
            if (query.getUserId() != null) {
                return new TableDataInfo<>(List.of(leader), 1);
            }
            return new TableDataInfo<>(List.of(), "0".equals(query.getStatus()) ? 8 : 2);
        });
        when(postService.selectPagePostList(any(), any()))
            .thenReturn(new TableDataInfo<SysPostVo>(List.of(), 4));

        AgentToolResult<SystemDepartmentOverviewResult> result =
            overviewService.getOverview(103L, "R&D", false);

        assertTrue(result.success());
        assertEquals("Headquarters", result.data().parent().deptName());
        assertEquals("zhangsan", result.data().leader().userName());
        assertEquals(2, result.data().childDepartmentCount());
        assertEquals(8, result.data().normalUserCount());
        assertEquals(2, result.data().disabledUserCount());
        assertEquals(4, result.data().postCount());
        assertFalse(result.data().includesChildrenInMetrics());
        verify(deptService).checkDeptDataScope(103L);

        ArgumentCaptor<SysUserBo> userQuery = ArgumentCaptor.forClass(SysUserBo.class);
        verify(userService, org.mockito.Mockito.times(3)).selectPageUserList(userQuery.capture(), any());
        List<SysUserBo> metricQueries = userQuery.getAllValues().stream()
            .filter(query -> query.getStatus() != null)
            .toList();
        assertTrue(metricQueries.stream().allMatch(query -> Long.valueOf(103L).equals(query.getExactDeptId())));
        assertTrue(metricQueries.stream().allMatch(query -> query.getDeptId() == null));

        ArgumentCaptor<SysPostBo> postQuery = ArgumentCaptor.forClass(SysPostBo.class);
        verify(postService).selectPagePostList(postQuery.capture(), any());
        assertEquals(103L, postQuery.getValue().getDeptId());
        assertNull(postQuery.getValue().getBelongDeptId());
    }

    @Test
    void shouldUseDepartmentTreeFiltersWhenChildrenAreIncluded() {
        SysDeptVo department = department(103L, "R&D", 100L);
        when(deptService.selectDeptById(103L)).thenReturn(department);
        when(deptService.selectDeptList(any())).thenReturn(List.of());
        when(userService.selectPageUserList(any(), any()))
            .thenReturn(new TableDataInfo<>(List.of(), 5), new TableDataInfo<>(List.of(), 1));
        when(postService.selectPagePostList(any(), any()))
            .thenReturn(new TableDataInfo<SysPostVo>(List.of(), 6));

        AgentToolResult<SystemDepartmentOverviewResult> result =
            overviewService.getOverview(103L, null, true);

        assertTrue(result.success());
        assertTrue(result.data().includesChildrenInMetrics());
        assertEquals("DEPARTMENT_AND_DESCENDANTS", result.metadata().get("metricScope"));

        ArgumentCaptor<SysUserBo> userQuery = ArgumentCaptor.forClass(SysUserBo.class);
        verify(userService, org.mockito.Mockito.times(2)).selectPageUserList(userQuery.capture(), any());
        assertTrue(userQuery.getAllValues().stream().allMatch(query -> Long.valueOf(103L).equals(query.getDeptId())));
        assertTrue(userQuery.getAllValues().stream().allMatch(query -> query.getExactDeptId() == null));

        ArgumentCaptor<SysPostBo> postQuery = ArgumentCaptor.forClass(SysPostBo.class);
        verify(postService).selectPagePostList(postQuery.capture(), any());
        assertEquals(103L, postQuery.getValue().getBelongDeptId());
        assertNull(postQuery.getValue().getDeptId());
    }

    private SysDeptVo department(Long deptId, String deptName, Long parentId) {
        SysDeptVo department = new SysDeptVo();
        department.setDeptId(deptId);
        department.setDeptName(deptName);
        department.setParentId(parentId);
        return department;
    }

    private SysDeptVo child(Long deptId, String deptName, String status) {
        SysDeptVo department = department(deptId, deptName, 103L);
        department.setStatus(status);
        return department;
    }

}
