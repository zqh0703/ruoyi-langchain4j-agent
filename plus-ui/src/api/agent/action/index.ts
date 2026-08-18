import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { AgentActionDecisionForm, AgentActionExecutionVO, AgentActionVO } from './types';

export function listAgentActions(sessionId: number): AxiosPromise<AgentActionVO[]> {
  return request({
    url: `/agent/action/list/${sessionId}`,
    method: 'get'
  });
}

export function getAgentAction(id: string | number): AxiosPromise<AgentActionVO> {
  return request({
    url: `/agent/action/${id}`,
    method: 'get'
  });
}

export function confirmAgentAction(id: string | number, data: AgentActionDecisionForm): AxiosPromise<AgentActionExecutionVO> {
  return request({
    url: `/agent/action/${id}/confirm`,
    method: 'post',
    data
  });
}

export function cancelAgentAction(id: string | number, data: AgentActionDecisionForm): AxiosPromise<AgentActionVO> {
  return request({
    url: `/agent/action/${id}/cancel`,
    method: 'post',
    data
  });
}
