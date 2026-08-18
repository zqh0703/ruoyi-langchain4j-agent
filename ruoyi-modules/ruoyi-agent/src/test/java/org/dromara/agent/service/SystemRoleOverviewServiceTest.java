package org.dromara.agent.service;

import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemRoleOverviewResult;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysMenuService;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("local")
class SystemRoleOverviewServiceTest {

    private ISysRoleService roleService;
    private ISysUserService userService;
    private ISysMenuService menuService;
    private SystemRoleOverviewService overviewService;

    @BeforeEach
    void setUp() {
        roleService = mock(ISysRoleService.class);
        userService = mock(ISysUserService.class);
        menuService = mock(ISysMenuService.class);
        overviewService = new SystemRoleOverviewService(roleService, userService, menuService);
    }

    @Test
    void shouldRejectMissingRoleIdentifier() {
        AgentToolResult<SystemRoleOverviewResult> result = overviewService.getOverview(null, " ", false, 10);

        assertFalse(result.success());
        assertEquals("INVALID_ARGUMENT", result.code());
        verify(roleService, never()).selectRoleById(any());
    }

    @Test
    void shouldReturnRoleSummaryAndTruncatePermissionsWithoutLoadingMembers() {
        SysRoleVo role = role(5L, "Auditor");
        role.setDataScope("3");
        when(roleService.selectRoleById(5L)).thenReturn(role);
        when(roleService.countUserRoleByRoleId(5L)).thenReturn(12L);
        when(menuService.selectMenuPermsByRoleId(5L)).thenReturn(
            IntStream.rangeClosed(1, 55)
                .mapToObj(index -> "system:test:" + index)
                .collect(Collectors.toCollection(LinkedHashSet::new))
        );

        AgentToolResult<SystemRoleOverviewResult> result =
            overviewService.getOverview(5L, null, false, 10);

        assertTrue(result.success());
        assertEquals("CURRENT_DEPARTMENT", result.data().dataScopeDescription());
        assertEquals(12, result.data().memberCount());
        assertTrue(result.data().members().isEmpty());
        assertEquals(55, result.data().permissionCount());
        assertEquals(50, result.data().menuPermissions().size());
        assertEquals(true, result.metadata().get("permissionsTruncated"));
        verify(roleService).checkRoleDataScope(5L);
        verify(userService, never()).selectAllocatedList(any(), any());
    }

    @Test
    void shouldLoadSafeMemberDetailsWhenRequested() {
        SysRoleVo role = role(5L, "Auditor");
        when(roleService.selectRoleList(any())).thenReturn(List.of(role));
        when(roleService.countUserRoleByRoleId(5L)).thenReturn(2L);
        when(menuService.selectMenuPermsByRoleId(5L)).thenReturn(new LinkedHashSet<>());
        SysUserVo member = new SysUserVo();
        member.setUserId(7L);
        member.setUserName("zhangsan");
        member.setNickName("Zhang San");
        member.setDeptName("R&D");
        member.setStatus("0");
        when(userService.selectAllocatedList(any(), any()))
            .thenReturn(new TableDataInfo<>(List.of(member), 2));

        AgentToolResult<SystemRoleOverviewResult> result =
            overviewService.getOverview(null, "Auditor", true, 1);

        assertTrue(result.success());
        assertEquals("zhangsan", result.data().members().get(0).userName());
        assertEquals(true, result.metadata().get("membersTruncated"));
    }

    private SysRoleVo role(Long roleId, String roleName) {
        SysRoleVo role = new SysRoleVo();
        role.setRoleId(roleId);
        role.setRoleName(roleName);
        role.setRoleKey("auditor");
        role.setStatus("0");
        return role;
    }

}
