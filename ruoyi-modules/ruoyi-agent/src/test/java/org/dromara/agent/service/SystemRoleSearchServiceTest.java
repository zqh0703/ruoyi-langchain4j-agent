package org.dromara.agent.service;

import org.dromara.agent.tool.AgentToolResult;
import org.dromara.agent.tool.SystemRoleSearchResult;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.service.ISysRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("local")
class SystemRoleSearchServiceTest {

    private ISysRoleService roleService;
    private SystemRoleSearchService searchService;

    @BeforeEach
    void setUp() {
        roleService = mock(ISysRoleService.class);
        searchService = new SystemRoleSearchService(roleService);
    }

    @Test
    void shouldListAvailableNormalRolesWithoutKeyword() {
        when(roleService.selectRoleList(any())).thenReturn(List.of(
            role(1L, "超级管理员", "superadmin", "0"),
            role(3L, "本部门及以下", "test1", "0")
        ));

        AgentToolResult<SystemRoleSearchResult> result = searchService.search(null, null, 10);

        assertTrue(result.success());
        assertEquals(2, result.data().total());
        assertEquals("本部门及以下", result.data().roles().get(1).roleName());
        assertEquals("NORMAL", result.data().roles().get(1).status());
    }

    @Test
    void shouldFilterRoleNameAndRoleKeyAndReportTruncation() {
        when(roleService.selectRoleList(any())).thenReturn(List.of(
            role(3L, "研发人员", "developer", "0"),
            role(4L, "研发负责人", "developer_manager", "0"),
            role(5L, "审计员", "auditor", "0")
        ));

        AgentToolResult<SystemRoleSearchResult> result = searchService.search("developer", "ALL", 1);

        assertTrue(result.success());
        assertEquals(2, result.data().total());
        assertEquals(1, result.data().roles().size());
        assertEquals(true, result.metadata().get("truncated"));
    }

    @Test
    void shouldRejectInvalidStatus() {
        AgentToolResult<SystemRoleSearchResult> result = searchService.search(null, "UNKNOWN", 10);

        assertFalse(result.success());
        assertEquals("INVALID_ARGUMENT", result.code());
    }

    private SysRoleVo role(Long id, String name, String key, String status) {
        SysRoleVo role = new SysRoleVo();
        role.setRoleId(id);
        role.setRoleName(name);
        role.setRoleKey(key);
        role.setStatus(status);
        role.setDataScope("5");
        return role;
    }
}
