package org.dromara.agent.service;

import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemUserSearchResult;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.service.ISysDeptService;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
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
class SystemUserSearchServiceTest {

    private ISysUserService userService;
    private ISysDeptService deptService;
    private ISysRoleService roleService;
    private SysUserRoleMapper userRoleMapper;
    private SystemUserSearchService searchService;

    @BeforeEach
    void setUp() {
        userService = mock(ISysUserService.class);
        deptService = mock(ISysDeptService.class);
        roleService = mock(ISysRoleService.class);
        userRoleMapper = mock(SysUserRoleMapper.class);
        searchService = new SystemUserSearchService(userService, deptService, roleService, userRoleMapper);
    }

    @Test
    void shouldSearchWithSafeStructuredResultAndDefaultNormalStatus() {
        SysUserVo user = new SysUserVo();
        user.setUserId(7L);
        user.setUserName("zhangsan");
        user.setNickName("Zhang San");
        user.setDeptId(103L);
        user.setStatus("0");
        user.setCreateTime(new Date(1234L));
        user.setEmail("secret@example.com");
        user.setPhonenumber("13800000000");
        when(userService.selectPageUserList(any(), any()))
            .thenReturn(new TableDataInfo<>(List.of(user), 3));
        when(deptService.selectDeptByIds(List.of(103L)))
            .thenReturn(List.of(department(103L, "R&D")));

        AgentToolResult<SystemUserSearchResult> result = searchService.search("zhang", null, null, null, 1);

        assertTrue(result.success());
        assertEquals(3, result.data().total());
        assertEquals(1, result.data().users().size());
        assertEquals("R&D", result.data().users().get(0).deptName());
        assertEquals("NORMAL", result.data().users().get(0).status());
        assertEquals(true, result.metadata().get("truncated"));

        ArgumentCaptor<SysUserBo> queryCaptor = ArgumentCaptor.forClass(SysUserBo.class);
        verify(userService).selectPageUserList(queryCaptor.capture(), any());
        assertEquals("zhang", queryCaptor.getValue().getKeyword());
        assertEquals("0", queryCaptor.getValue().getStatus());
        assertNull(queryCaptor.getValue().getEmail());
        assertNull(queryCaptor.getValue().getPhonenumber());
    }

    @Test
    void shouldRejectInvalidStatusBeforeQueryingDatabase() {
        AgentToolResult<SystemUserSearchResult> result = searchService.search(null, null, null, "LOCKED", 10);

        assertFalse(result.success());
        assertEquals("INVALID_ARGUMENT", result.code());
        verify(userService, never()).selectPageUserList(any(), any());
    }

    @Test
    void shouldReturnDepartmentCandidatesWhenNameIsAmbiguous() {
        SysDeptVo first = department(10L, "R&D Platform");
        SysDeptVo second = department(11L, "R&D Security");
        when(deptService.selectDeptList(any())).thenReturn(List.of(first, second));

        AgentToolResult<SystemUserSearchResult> result = searchService.search(null, "R&D", null, "ALL", 10);

        assertFalse(result.success());
        assertEquals("AMBIGUOUS_TARGET", result.code());
        verify(userService, never()).selectPageUserList(any(), any());
    }

    @Test
    void shouldResolveRoleAndKeepFinalQueryOnDataPermissionAwareUserService() {
        SysRoleVo role = new SysRoleVo();
        role.setRoleId(5L);
        role.setRoleName("Auditor");
        when(roleService.selectRoleList(any())).thenReturn(List.of(role));
        when(userRoleMapper.selectUserIdsByRoleId(5L)).thenReturn(List.of(7L, 9L));
        when(userService.selectPageUserList(any(), any()))
            .thenReturn(new TableDataInfo<>(List.of(), 0));

        AgentToolResult<SystemUserSearchResult> result = searchService.search(null, null, "Auditor", "NORMAL", 10);

        assertTrue(result.success());
        ArgumentCaptor<SysUserBo> queryCaptor = ArgumentCaptor.forClass(SysUserBo.class);
        verify(userService).selectPageUserList(queryCaptor.capture(), any());
        assertEquals("7,9", queryCaptor.getValue().getUserIds());
    }

    private SysDeptVo department(Long id, String name) {
        SysDeptVo department = new SysDeptVo();
        department.setDeptId(id);
        department.setDeptName(name);
        return department;
    }

}
