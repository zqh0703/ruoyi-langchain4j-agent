package org.dromara.agent.service;

import org.dromara.agent.domain.bo.AgentConfigBo;
import org.dromara.agent.domain.vo.AgentConfigVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Agent 配置 Service 接口
 *
 * @author Codex
 */
public interface IAgentConfigService {

    /**
     * 查询 Agent 配置
     */
    AgentConfigVo queryById(Long id);

    /**
     * 查询 Agent 配置分页列表
     */
    TableDataInfo<AgentConfigVo> queryPageList(AgentConfigBo bo, PageQuery pageQuery);

    /**
     * 查询 Agent 配置列表
     */
    List<AgentConfigVo> queryList(AgentConfigBo bo);

    /**
     * 新增 Agent 配置
     */
    Boolean insertByBo(AgentConfigBo bo);

    /**
     * 修改 Agent 配置
     */
    Boolean updateByBo(AgentConfigBo bo);

    /**
     * 批量删除 Agent 配置
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
