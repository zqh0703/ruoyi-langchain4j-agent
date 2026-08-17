import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type { AgentRunLogQuery, AgentRunLogVO, AgentRunTraceVO } from './types';

export function listAgentRuns(query: AgentRunLogQuery): AxiosPromise<AgentRunLogVO[]> {
  return request({
    url: '/agent/run/list',
    method: 'get',
    params: query
  });
}

export function getAgentRunTrace(id: string | number): AxiosPromise<AgentRunTraceVO> {
  return request({
    url: '/agent/run/' + id + '/trace',
    method: 'get'
  });
}
