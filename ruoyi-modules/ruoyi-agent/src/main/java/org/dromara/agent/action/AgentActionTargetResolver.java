package org.dromara.agent.action;

import lombok.RequiredArgsConstructor;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.domain.bo.SysPostBo;
import org.dromara.system.domain.bo.SysRoleBo;
import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysDeptService;
import org.dromara.system.service.ISysPostService;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact and data-scope-aware target resolution shared by write actions.
 */
@Component
@RequiredArgsConstructor
public class AgentActionTargetResolver {

    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final ISysRoleService roleService;
    private final ISysPostService postService;

    public SysUserVo user(Long userId, String userName) {
        requireOneIdentifier(userId, userName, "userId", "userName");
        SysUserVo user = userId != null
            ? userService.selectUserById(userId)
            : userService.selectUserByUserName(userName.trim());
        if (user == null) {
            throw new AgentActionException("NOT_FOUND", "The target user was not found");
        }
        userService.checkUserDataScope(user.getUserId());
        return user;
    }

    public SysDeptVo department(Long deptId, String deptName) {
        requireOneIdentifier(deptId, deptName, "deptId", "deptName");
        if (deptId != null) {
            deptService.checkDeptDataScope(deptId);
            SysDeptVo dept = deptService.selectDeptById(deptId);
            if (dept == null) {
                throw new AgentActionException("NOT_FOUND", "The target department was not found");
            }
            return dept;
        }
        SysDeptBo query = new SysDeptBo();
        query.setDeptName(deptName.trim());
        List<SysDeptVo> matches = deptService.selectDeptList(query).stream()
            .filter(value -> deptName.trim().equals(value.getDeptName()))
            .toList();
        return uniqueDepartment(matches);
    }

    public List<SysRoleVo> roles(List<Long> roleIds, List<String> roleNames) {
        Map<Long, SysRoleVo> resolved = new LinkedHashMap<>();
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                if (roleId == null) {
                    continue;
                }
                roleService.checkRoleDataScope(roleId);
                SysRoleVo role = roleService.selectRoleById(roleId);
                if (role == null) {
                    throw new AgentActionException("NOT_FOUND", "Role " + roleId + " was not found");
                }
                resolved.put(role.getRoleId(), role);
            }
        }
        if (roleNames != null) {
            for (String roleName : roleNames) {
                if (roleName == null || roleName.isBlank()) {
                    continue;
                }
                SysRoleBo query = new SysRoleBo();
                query.setRoleName(roleName.trim());
                List<SysRoleVo> matches = roleService.selectRoleList(query).stream()
                    .filter(value -> roleName.trim().equals(value.getRoleName()))
                    .toList();
                if (matches.isEmpty()) {
                    throw new AgentActionException("NOT_FOUND", "Role " + roleName + " was not found");
                }
                if (matches.size() > 1) {
                    throw new AgentActionException("AMBIGUOUS_TARGET", "More than one role is named " + roleName);
                }
                SysRoleVo role = matches.get(0);
                roleService.checkRoleDataScope(role.getRoleId());
                resolved.put(role.getRoleId(), role);
            }
        }
        if (resolved.isEmpty()) {
            throw new AgentActionException("INVALID_ARGUMENT", "At least one role is required");
        }
        return new ArrayList<>(resolved.values());
    }

    public List<SysPostVo> posts(List<Long> postIds, List<String> postNames, Long deptId) {
        Map<Long, SysPostVo> resolved = new LinkedHashMap<>();
        if (postIds != null) {
            for (Long postId : postIds) {
                if (postId == null) {
                    continue;
                }
                SysPostVo post = postService.selectPostById(postId);
                if (post == null) {
                    throw new AgentActionException("NOT_FOUND", "Post " + postId + " was not found");
                }
                resolved.put(post.getPostId(), post);
            }
        }
        if (postNames != null) {
            for (String postName : postNames) {
                if (postName == null || postName.isBlank()) {
                    continue;
                }
                SysPostBo query = new SysPostBo();
                query.setPostName(postName.trim());
                query.setDeptId(deptId);
                List<SysPostVo> matches = postService.selectPostList(query).stream()
                    .filter(value -> postName.trim().equals(value.getPostName()))
                    .toList();
                if (matches.isEmpty()) {
                    throw new AgentActionException("NOT_FOUND", "Post " + postName + " was not found");
                }
                if (matches.size() > 1) {
                    throw new AgentActionException("AMBIGUOUS_TARGET", "More than one post is named " + postName);
                }
                resolved.put(matches.get(0).getPostId(), matches.get(0));
            }
        }
        for (SysPostVo post : resolved.values()) {
            if (!deptId.equals(post.getDeptId())) {
                throw new AgentActionException("INVALID_ARGUMENT", "Post " + post.getPostName() + " is outside the selected department");
            }
            if (!"0".equals(post.getStatus())) {
                throw new AgentActionException("INVALID_ARGUMENT", "Post " + post.getPostName() + " is disabled");
            }
        }
        return new ArrayList<>(resolved.values());
    }

    private SysDeptVo uniqueDepartment(List<SysDeptVo> matches) {
        if (matches.isEmpty()) {
            throw new AgentActionException("NOT_FOUND", "The target department was not found");
        }
        if (matches.size() > 1) {
            throw new AgentActionException("AMBIGUOUS_TARGET", "More than one department has that name");
        }
        SysDeptVo dept = matches.get(0);
        deptService.checkDeptDataScope(dept.getDeptId());
        return dept;
    }

    private void requireOneIdentifier(Long id, String name, String idLabel, String nameLabel) {
        boolean hasId = id != null;
        boolean hasName = name != null && !name.isBlank();
        if (hasId == hasName) {
            throw new AgentActionException(
                "INVALID_ARGUMENT", "Provide exactly one of " + idLabel + " or " + nameLabel
            );
        }
    }
}
