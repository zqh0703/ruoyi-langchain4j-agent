export type AgentActionStatus = 'PENDING_CONFIRMATION' | 'EXECUTING' | 'SUCCESS' | 'FAILED' | 'CANCELLED' | 'EXPIRED';

export interface AgentActionVO extends BaseEntity {
  id: string | number;
  sessionId: number;
  agentId: number;
  runLogId: number;
  toolMessageId?: string | number;
  toolCode: string;
  riskLevel: 'MEDIUM' | 'HIGH';
  status: AgentActionStatus;
  summary: string;
  errorCode?: string;
  errorMessage?: string;
  expiresAt: string;
  confirmedBy?: number;
  confirmedTime?: string;
  startedTime?: string;
  finishedTime?: string;
  durationMs?: number;
  version: number;
  preview: Record<string, unknown>;
  result: Record<string, unknown>;
}

export interface AgentActionExecutionVO {
  action: AgentActionVO;
  secretType?: string;
  secretValue?: string;
}

export interface AgentActionDecisionForm {
  version: number;
}
