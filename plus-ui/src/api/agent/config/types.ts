export interface AgentConfigVO extends BaseEntity {
  id: number;
  agentName: string;
  agentCode: string;
  provider: string;
  modelName: string;
  systemPrompt: string;
  temperature: number;
  maxTokens: number;
  enableTool: string;
  status: string;
  remark: string;
}

export interface AgentConfigQuery extends PageQuery {
  agentName: string;
  agentCode: string;
  provider: string;
  status: string;
}

export interface AgentConfigForm {
  id: number | undefined;
  agentName: string;
  agentCode: string;
  provider: string;
  modelName: string;
  systemPrompt: string;
  temperature: number;
  maxTokens: number;
  enableTool: string;
  status: string;
  remark: string;
}
