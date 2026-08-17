package org.dromara.agent.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.dromara.system.domain.SysMenu;
import org.dromara.system.domain.SysOperLog;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysMenuMapper;
import org.dromara.system.mapper.SysOperLogMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;

/**
 * Read-only system tools exposed to enabled Agents.
 */
@Component
@RequiredArgsConstructor
public class SystemAgentTools {

    private final SysUserMapper sysUserMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysOperLogMapper sysOperLogMapper;

    @Tool("统计当前租户中状态正常的系统用户数量。")
    public String systemUserCount() {
        long count = sysUserMapper.selectCount(
            Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getStatus, "0")
                .eq(SysUser::getDelFlag, "0")
        );
        return "当前租户状态正常的系统用户数为 " + count + "。";
    }

    @Tool("统计系统中状态正常的菜单与权限数量。")
    public String systemMenuCount() {
        long count = sysMenuMapper.selectCount(
            Wrappers.lambdaQuery(SysMenu.class)
                .eq(SysMenu::getStatus, "0")
        );
        return "当前系统状态正常的菜单与权限数量为 " + count + "。";
    }

    @Tool("汇总当前租户最近七天的操作日志总数、成功数和失败数。")
    public String operLogSummary() {
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - Duration.ofDays(7).toMillis());
        long total = sysOperLogMapper.selectCount(
            Wrappers.lambdaQuery(SysOperLog.class).ge(SysOperLog::getOperTime, sevenDaysAgo)
        );
        long success = sysOperLogMapper.selectCount(
            Wrappers.lambdaQuery(SysOperLog.class)
                .ge(SysOperLog::getOperTime, sevenDaysAgo)
                .eq(SysOperLog::getStatus, 0)
        );
        return "最近七天操作日志总数 " + total + "，成功 " + success + "，失败 " + (total - success) + "。";
    }

}
