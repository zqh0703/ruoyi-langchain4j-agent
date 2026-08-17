import type { AgentMessageVO } from '@/api/agent/chat/types';

export interface AgentRunLogVO extends BaseEntity {
  id: string | number;
  agentId: string | number;
  sessionId: string | number;
  provider: string;
  modelName: string;
  requestBody: string;
  responseBody: string;
  status: string;
  errorMsg: string;
  durationMs: number;
  createTime: string;
}

export interface AgentRunLogQuery extends PageQuery {
  agentId?: string | number;
  sessionId?: string | number;
  provider?: string;
  modelName?: string;
  status?: string;
}

export interface AgentRunTraceVO {
  runLog: AgentRunLogVO;
  messages: AgentMessageVO[];
}
