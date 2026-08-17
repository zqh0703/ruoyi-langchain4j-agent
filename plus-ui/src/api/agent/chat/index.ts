import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { AgentChatReplyVO, AgentChatSendForm, AgentMessageVO, AgentSessionCreateForm, AgentSessionVO } from './types';

export function listAgentSessions(params: PageQuery): AxiosPromise<AgentSessionVO[]> {
  return request({
    url: '/agent/session/list',
    method: 'get',
    params
  });
}

export function createAgentSession(data: AgentSessionCreateForm): AxiosPromise<AgentSessionVO> {
  return request({
    url: '/agent/session',
    method: 'post',
    data
  });
}

export function listAgentMessages(sessionId: number): AxiosPromise<AgentMessageVO[]> {
  return request({
    url: `/agent/message/list/${sessionId}`,
    method: 'get'
  });
}

export function sendAgentMessage(data: AgentChatSendForm): AxiosPromise<AgentChatReplyVO> {
  return request({
    url: '/agent/chat/send',
    method: 'post',
    data
  });
}
