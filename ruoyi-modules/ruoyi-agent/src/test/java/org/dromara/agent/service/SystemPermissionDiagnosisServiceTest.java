package org.dromara.agent.service;

import org.dromara.agent.tool.AgentExecutionContext;
import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemPermissionDiagnosisResult;
import org.dromara.system.domain.SysRoleMenu;
import org.dromara.system.domain.vo.SysMenuVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysRoleMenuMapper;
import org.dromara.system.service.ISysMenuService;
import org.dromara.system.service.ISysPermissionService;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("local")
class SystemPermissionDiagnosisServiceTest {

    private ISysUserService userService;
    private ISysRoleService roleService;
    private ISysMenuService menuService;
    private ISysPermissionService permissionService;
    private SysRoleMenuMapper roleMenuMapper;
    private AgentExecutionContext executionContext;
    private SystemPermissionDiagnosisService diagnosisService;

    @BeforeEach
    void setUp() {
        userService = mock(ISysUserService.class);
        roleService = mock(ISysRoleService.class);
        menuService = mock(ISysMenuService.class);
        permissionService = mock(ISysPermissionService.class);
        roleMenuMapper = mock(SysRoleMenuMapper.class);
        executionContext = mock(AgentExecutionContext.class);
        when(executionContext.currentUserId()).thenReturn(1L);
        diagnosisService = new SystemPermissionDiagnosisService(
            userService, roleService, menuService, permissionService, roleMenuMapper, executionContext
        );
    }

    @Test
    void shouldRejectMissingPermissionTarget() {
        AgentToolResult<SystemPermissionDiagnosisResult> result =
            diagnosisService.diagnose(1L, null, null, " ");

        assertFalse(result.success());
        assertEquals("INVALID_ARGUMENT", result.code());
        verify(userService, never()).selectUserById(anyLong());
    }

    @Test
    void shouldExplainSuccessfulRoleBasedAuthorization() {
        SysUserVo user = new SysUserVo();
        user.setUserId(7L);
        user.setUserName("zhangsan");
        user.setNickName("Zhang San");
        user.setStatus("0");
        when(userService.selectUserByUserName("zhangsan")).thenReturn(user);

        SysMenuVo menu = new SysMenuVo();
        menu.setMenuId(100L);
        menu.setMenuName("User list");
        menu.setMenuType("F");
        menu.setPerms("system:user:list");
        menu.setStatus("0");
        when(menuService.selectMenuList(any(), anyLong())).thenReturn(List.of(menu));

        SysRoleVo role = new SysRoleVo();
        role.setRoleId(5L);
        role.setRoleName("Auditor");
        role.setRoleKey("auditor");
        role.setStatus("0");
        when(roleService.selectRolesByUserId(7L)).thenReturn(List.of(role));
        when(roleService.selectRoleByIds(List.of(5L))).thenReturn(List.of(role));
        when(menuService.selectMenuPermsByRoleId(5L)).thenReturn(Set.of("system:user:list"));
        when(permissionService.getMenuPermission(7L)).thenReturn(Set.of("system:user:list"));
        SysRoleMenu relation = new SysRoleMenu();
        relation.setRoleId(5L);
        relation.setMenuId(100L);
        when(roleMenuMapper.selectList(any())).thenReturn(List.of(relation));

        AgentToolResult<SystemPermissionDiagnosisResult> result =
            diagnosisService.diagnose(null, "zhangsan", "system:user:list", null);

        assertTrue(result.success());
        assertTrue(result.data().authorized());
        assertEquals("Auditor", result.data().sourceRoles().get(0).roleName());
        assertTrue(result.data().blockingReasons().isEmpty());
        verify(userService).checkUserDataScope(7L);
    }

}
