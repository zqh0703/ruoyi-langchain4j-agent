export interface AgentSessionVO extends BaseEntity {
  id: number;
  agentId: number;
  title: string;
  status: string;
  lastMessageTime: string;
}

export interface AgentMessageVO extends BaseEntity {
  id: number;
  sessionId: number;
  runLogId?: string | number;
  agentId: number;
  role: 'user' | 'assistant' | 'tool';
  content: string;
  toolName: string;
  toolArgs: string;
  toolDurationMs?: number;
  toolResult: string;
  promptTokens: number;
  completionTokens: number;
  seq: number;
}

export interface AgentChatReplyVO {
  sessionId: number;
  messageId: number;
  runLogId: number;
  content: string;
  durationMs: number;
}

export interface AgentSessionCreateForm {
  agentId: number;
  title: string;
}

export interface AgentChatSendForm {
  sessionId: number;
  message: string;
}
