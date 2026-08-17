import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { AgentConfigForm, AgentConfigQuery, AgentConfigVO } from './types';

export function listAgentConfig(query: AgentConfigQuery): AxiosPromise<AgentConfigVO[]> {
  return request({
    url: '/agent/config/list',
    method: 'get',
    params: query
  });
}

export function getAgentConfig(id: number): AxiosPromise<AgentConfigVO> {
  return request({
    url: `/agent/config/${id}`,
    method: 'get'
  });
}

export function addAgentConfig(data: AgentConfigForm) {
  return request({
    url: '/agent/config',
    method: 'post',
    data
  });
}

export function updateAgentConfig(data: AgentConfigForm) {
  return request({
    url: '/agent/config',
    method: 'put',
    data
  });
}

export function delAgentConfig(ids: number | number[]) {
  return request({
    url: `/agent/config/${ids}`,
    method: 'delete'
  });
}
