package org.dromara.agent.service;

import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemUserAccessProfileResult;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysDeptService;
import org.dromara.system.service.ISysPermissionService;
import org.dromara.system.service.ISysPostService;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("local")
class SystemUserAccessProfileServiceTest {

    private ISysUserService userService;
    private ISysDeptService deptService;
    private ISysPostService postService;
    private ISysRoleService roleService;
    private ISysPermissionService permissionService;
    private SystemUserAccessProfileService profileService;

    @BeforeEach
    void setUp() {
        userService = mock(ISysUserService.class);
        deptService = mock(ISysDeptService.class);
        postService = mock(ISysPostService.class);
        roleService = mock(ISysRoleService.class);
        permissionService = mock(ISysPermissionService.class);
        profileService = new SystemUserAccessProfileService(
            userService,
            deptService,
            postService,
            roleService,
            permissionService
        );
    }

    @Test
    void shouldRejectMissingIdentifierBeforeQueryingUser() {
        AgentToolResult<SystemUserAccessProfileResult> result = profileService.getProfile(null, "  ");

        assertFalse(result.success());
        assertEquals("INVALID_ARGUMENT", result.code());
        verify(userService, never()).selectUserById(null);
        verify(userService, never()).selectUserByUserName(null);
    }

    @Test
    void shouldReturnNotFoundForUnknownExactUserName() {
        when(userService.selectUserByUserName("missing")).thenReturn(null);

        AgentToolResult<SystemUserAccessProfileResult> result = profileService.getProfile(null, "missing");

        assertFalse(result.success());
        assertEquals("NOT_FOUND", result.code());
        verify(userService, never()).checkUserDataScope(null);
    }

    @Test
    void shouldRejectConflictingUserIdAndUserName() {
        SysUserVo user = user(7L, "zhangsan");
        when(userService.selectUserById(7L)).thenReturn(user);

        AgentToolResult<SystemUserAccessProfileResult> result = profileService.getProfile(7L, "lisi");

        assertFalse(result.success());
        assertEquals("INVALID_ARGUMENT", result.code());
        verify(userService, never()).checkUserDataScope(7L);
    }

    @Test
    void shouldAggregateSafeProfileApplyDataScopeAndTruncatePermissions() {
        SysUserVo user = user(7L, "zhangsan");
        user.setDeptId(103L);
        user.setStatus("0");
        user.setEmail("secret@example.com");
        user.setPhonenumber("13800000000");
        when(userService.selectUserByUserName("zhangsan")).thenReturn(user);

        SysDeptVo department = new SysDeptVo();
        department.setDeptId(103L);
        department.setDeptName("R&D");
        department.setParentId(100L);
        department.setParentName("Headquarters");
        when(deptService.selectDeptById(103L)).thenReturn(department);

        SysPostVo post = new SysPostVo();
        post.setPostId(9L);
        post.setPostCode("dev");
        post.setPostName("Developer");
        post.setStatus("0");
        when(postService.selectPostsByUserId(7L)).thenReturn(List.of(post));

        SysRoleVo role = new SysRoleVo();
        role.setRoleId(5L);
        role.setRoleName("Auditor");
        role.setRoleKey("auditor");
        role.setStatus("0");
        role.setDataScope("3");
        when(roleService.selectRolesByUserId(7L)).thenReturn(List.of(role));

        Set<String> permissions = IntStream.rangeClosed(1, 55)
            .mapToObj(index -> "system:test:" + String.format("%02d", index))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        when(permissionService.getMenuPermission(7L)).thenReturn(permissions);

        AgentToolResult<SystemUserAccessProfileResult> result = profileService.getProfile(null, " zhangsan ");

        assertTrue(result.success());
        assertEquals("NORMAL", result.data().accountStatus());
        assertEquals("R&D", result.data().department().deptName());
        assertEquals("Headquarters", result.data().department().parentName());
        assertEquals("Developer", result.data().posts().get(0).postName());
        assertEquals("Auditor", result.data().roles().get(0).roleName());
        assertEquals(55, result.data().permissionCount());
        assertEquals(50, result.data().permissions().size());
        assertEquals(true, result.metadata().get("permissionsTruncated"));
        verify(userService).checkUserDataScope(7L);
    }

    private SysUserVo user(Long userId, String userName) {
        SysUserVo user = new SysUserVo();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setNickName("Zhang San");
        return user;
    }

}
