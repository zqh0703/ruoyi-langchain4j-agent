package org.dromara.agent.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.domain.bo.AgentConfigBo;
import org.dromara.agent.domain.vo.AgentConfigVo;
import org.dromara.agent.service.IAgentConfigService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.core.validate.QueryGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * Agent 配置 Controller
 *
 * @author Codex
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/config")
public class AgentConfigController extends BaseController {

    private final IAgentConfigService agentConfigService;

    /**
     * 查询 Agent 配置列表
     */
    @SaCheckPermission("agent:config:list")
    @GetMapping("/list")
    public TableDataInfo<AgentConfigVo> list(@Validated(QueryGroup.class) AgentConfigBo bo, PageQuery pageQuery) {
        return agentConfigService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取 Agent 配置详细信息
     */
    @SaCheckPermission("agent:config:query")
    @GetMapping("/{id}")
    public R<AgentConfigVo> getInfo(@NotNull(message = "主键不能为空")
                                    @PathVariable("id") Long id) {
        return R.ok(agentConfigService.queryById(id));
    }

    /**
     * 新增 Agent 配置
     */
    @SaCheckPermission("agent:config:add")
    @Log(title = "Agent配置", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody AgentConfigBo bo) {
        return toAjax(agentConfigService.insertByBo(bo));
    }

    /**
     * 修改 Agent 配置
     */
    @SaCheckPermission("agent:config:edit")
    @Log(title = "Agent配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody AgentConfigBo bo) {
        return toAjax(agentConfigService.updateByBo(bo));
    }

    /**
     * 删除 Agent 配置
     */
    @SaCheckPermission("agent:config:remove")
    @Log(title = "Agent配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(agentConfigService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

}
