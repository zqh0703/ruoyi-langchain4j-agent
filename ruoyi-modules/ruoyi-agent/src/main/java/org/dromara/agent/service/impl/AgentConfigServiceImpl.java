package org.dromara.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.agent.domain.AgentConfig;
import org.dromara.agent.domain.bo.AgentConfigBo;
import org.dromara.agent.domain.vo.AgentConfigVo;
import org.dromara.agent.mapper.AgentConfigMapper;
import org.dromara.agent.service.IAgentConfigService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Agent 配置 Service 业务层处理
 *
 * @author Codex
 */
@RequiredArgsConstructor
@Service
public class AgentConfigServiceImpl implements IAgentConfigService {

    private final AgentConfigMapper baseMapper;

    @Override
    public AgentConfigVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<AgentConfigVo> queryPageList(AgentConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<AgentConfig> lqw = buildQueryWrapper(bo);
        Page<AgentConfigVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<AgentConfigVo> queryList(AgentConfigBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<AgentConfig> buildQueryWrapper(AgentConfigBo bo) {
        LambdaQueryWrapper<AgentConfig> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getAgentName()), AgentConfig::getAgentName, bo.getAgentName());
        lqw.eq(StringUtils.isNotBlank(bo.getAgentCode()), AgentConfig::getAgentCode, bo.getAgentCode());
        lqw.eq(StringUtils.isNotBlank(bo.getProvider()), AgentConfig::getProvider, bo.getProvider());
        lqw.eq(StringUtils.isNotBlank(bo.getModelName()), AgentConfig::getModelName, bo.getModelName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), AgentConfig::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getEnableTool()), AgentConfig::getEnableTool, bo.getEnableTool());
        lqw.orderByDesc(AgentConfig::getCreateTime);
        return lqw;
    }

    @Override
    public Boolean insertByBo(AgentConfigBo bo) {
        AgentConfig add = MapstructUtils.convert(bo, AgentConfig.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(AgentConfigBo bo) {
        AgentConfig update = MapstructUtils.convert(bo, AgentConfig.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    private void validEntityBeforeSave(AgentConfig entity) {
        LambdaQueryWrapper<AgentConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(AgentConfig::getAgentCode, entity.getAgentCode());
        lqw.ne(entity.getId() != null, AgentConfig::getId, entity.getId());
        if (baseMapper.exists(lqw)) {
            throw new ServiceException("Agent编码已存在");
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            List<AgentConfig> list = baseMapper.selectByIds(ids);
            if (list.size() != ids.size()) {
                throw new ServiceException("Agent配置不存在或已被删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

}
