<template>
  <div class="chat-page p-2">
    <div class="chat-layout">
      <aside class="session-panel">
        <div class="panel-heading">
          <div>
            <h2>Agent 调试</h2>
            <p>选择配置并开始一段会话</p>
          </div>
          <el-tooltip content="新建会话" placement="top">
            <el-button v-hasPermi="['agent:chat:send']" circle type="primary" icon="Plus" :disabled="!selectedAgentId" @click="handleCreateSession" />
          </el-tooltip>
        </div>

        <el-select v-model="selectedAgentId" class="agent-select" placeholder="请选择 Agent" @change="handleAgentChange">
          <el-option v-for="agent in agentList" :key="agent.id" :label="agent.agentName" :value="agent.id">
            <span>{{ agent.agentName }}</span>
            <span class="option-model">{{ agent.modelName }}</span>
          </el-option>
        </el-select>

        <div v-loading="sessionLoading" class="session-list">
          <el-empty v-if="selectedAgentId && !sessionList.length" description="还没有会话" :image-size="72" />
          <el-empty v-else-if="!selectedAgentId" description="请先选择 Agent" :image-size="72" />
          <button
            v-for="session in sessionList"
            :key="session.id"
            class="session-item"
            :class="{ active: session.id === selectedSessionId }"
            type="button"
            @click="handleSelectSession(session.id)"
          >
            <span class="session-title">{{ session.title || '未命名会话' }}</span>
            <span class="session-time">{{ proxy?.parseTime(session.lastMessageTime || session.createTime, '{m}-{d} {h}:{i}') }}</span>
          </button>
        </div>
      </aside>

      <main class="conversation-panel">
        <template v-if="selectedAgentId">
          <header class="conversation-header">
            <div>
              <h1>{{ selectedAgent?.agentName }}</h1>
              <span>{{ selectedAgent?.modelName }} · {{ selectedAgent?.enableTool === '1' ? '业务工具已启用' : '仅模型对话' }}</span>
            </div>
            <el-tag :type="selectedAgent?.status === '0' ? 'success' : 'danger'">
              {{ selectedAgent?.status === '0' ? '可用' : '已停用' }}
            </el-tag>
          </header>

          <section ref="messageContainerRef" v-loading="messageLoading" class="message-list">
            <el-empty v-if="!selectedSessionId" description="新建会话后即可开始调试" :image-size="100" />
            <el-empty v-else-if="!messageList.length" description="发送第一条消息开始对话" :image-size="100" />
            <article v-for="message in messageList" :key="message.id" class="message-row" :class="message.role">
              <div class="message-avatar">{{ message.role === 'user' ? '我' : message.role === 'tool' ? '工具' : 'AI' }}</div>
              <div class="message-content">
                <div class="message-meta">
                  {{ message.role === 'user' ? '你' : message.role === 'tool' ? message.toolName || '业务工具' : selectedAgent?.agentName }}
                  <span>{{ proxy?.parseTime(message.createTime, '{h}:{i}:{s}') }}</span>
                </div>
                <div class="message-bubble">{{ message.content }}</div>
                <div v-if="message.role === 'tool' && message.toolResult" class="tool-result">{{ message.toolResult }}</div>
              </div>
            </article>
            <article v-if="sending" class="message-row assistant">
              <div class="message-avatar">AI</div>
              <div class="message-content">
                <div class="message-meta">{{ selectedAgent?.agentName }}</div>
                <div class="message-bubble pending">正在思考…</div>
              </div>
            </article>
          </section>

          <footer class="composer">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              resize="none"
              placeholder="输入消息，Enter 发送，Shift + Enter 换行"
              :disabled="sending || selectedAgent?.status !== '0'"
              @keydown.enter.exact.prevent="handleSend"
            />
            <div class="composer-actions">
              <span>{{ selectedSessionId ? '消息会自动保存到当前会话' : '首次发送会自动创建会话' }}</span>
              <el-button
                v-hasPermi="['agent:chat:send']"
                type="primary"
                icon="Promotion"
                :loading="sending"
                :disabled="!inputMessage.trim() || selectedAgent?.status !== '0'"
                @click="handleSend"
              >
                发送
              </el-button>
            </div>
          </footer>
        </template>
        <el-empty v-else description="请选择一个 Agent 配置" :image-size="120" />
      </main>
    </div>
  </div>
</template>

<script setup name="AgentChat" lang="ts">
import { listAgentConfig } from '@/api/agent/config';
import { AgentConfigVO } from '@/api/agent/config/types';
import { createAgentSession, listAgentMessages, listAgentSessions, sendAgentMessage } from '@/api/agent/chat';
import { AgentMessageVO, AgentSessionVO } from '@/api/agent/chat/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const agentList = ref<AgentConfigVO[]>([]);
const sessionList = ref<AgentSessionVO[]>([]);
const messageList = ref<AgentMessageVO[]>([]);
const selectedAgentId = ref<number>();
const selectedSessionId = ref<number>();
const sessionLoading = ref(false);
const messageLoading = ref(false);
const sending = ref(false);
const inputMessage = ref('');
const messageContainerRef = ref<HTMLElement>();

const selectedAgent = computed(() => agentList.value.find((agent) => agent.id === selectedAgentId.value));

const scrollToBottom = async () => {
  await nextTick();
  const container = messageContainerRef.value;
  if (container) container.scrollTop = container.scrollHeight;
};

const getAgentList = async () => {
  const res = await listAgentConfig({ pageNum: 1, pageSize: 100, agentName: '', agentCode: '', provider: '', status: '0' });
  agentList.value = res.rows;
  if (agentList.value.length) selectedAgentId.value = agentList.value[0].id;
};

const getSessions = async () => {
  if (!selectedAgentId.value) return;
  sessionLoading.value = true;
  try {
    const res = await listAgentSessions({ pageNum: 1, pageSize: 100 });
    sessionList.value = res.rows.filter((session) => session.agentId === selectedAgentId.value);
  } finally {
    sessionLoading.value = false;
  }
};

const getMessages = async () => {
  if (!selectedSessionId.value) {
    messageList.value = [];
    return;
  }
  messageLoading.value = true;
  try {
    const res = await listAgentMessages(selectedSessionId.value);
    messageList.value = res.data;
    await scrollToBottom();
  } finally {
    messageLoading.value = false;
  }
};

const handleAgentChange = async () => {
  selectedSessionId.value = undefined;
  messageList.value = [];
  await getSessions();
  if (sessionList.value.length) await handleSelectSession(sessionList.value[0].id);
};

const handleSelectSession = async (sessionId: number) => {
  selectedSessionId.value = sessionId;
  await getMessages();
};

const createSession = async () => {
  if (!selectedAgentId.value) throw new Error('请先选择 Agent');
  const res = await createAgentSession({
    agentId: selectedAgentId.value,
    title: `调试会话 ${new Date().toLocaleString('zh-CN', { hour12: false })}`
  });
  selectedSessionId.value = res.data.id;
  sessionList.value.unshift(res.data);
  messageList.value = [];
};

const handleCreateSession = async () => {
  await createSession();
  proxy?.$modal.msgSuccess('已创建新会话');
};

const handleSend = async () => {
  const message = inputMessage.value.trim();
  if (!message || sending.value || !selectedAgentId.value) return;
  if (!selectedSessionId.value) await createSession();

  sending.value = true;
  inputMessage.value = '';
  messageList.value.push({
    id: -Date.now(),
    sessionId: selectedSessionId.value!,
    agentId: selectedAgentId.value,
    role: 'user',
    content: message,
    toolName: '',
    toolArgs: '',
    toolResult: '',
    promptTokens: 0,
    completionTokens: 0,
    seq: 0,
    createTime: new Date().toISOString()
  });
  await scrollToBottom();

  try {
    await sendAgentMessage({ sessionId: selectedSessionId.value!, message });
    await getMessages();
    await getSessions();
  } catch (error) {
    inputMessage.value = message;
    messageList.value = messageList.value.filter((item) => item.id > 0);
    throw error;
  } finally {
    sending.value = false;
  }
};

onMounted(async () => {
  await getAgentList();
  await handleAgentChange();
});
</script>

<style scoped lang="scss">
.chat-page {
  height: calc(100vh - 84px);
  min-height: 620px;
}

.chat-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  height: 100%;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.session-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  border-right: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-lighter);
}

.panel-heading,
.conversation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-heading {
  padding: 18px 16px 14px;
}

h1,
h2,
p {
  margin: 0;
}

h1,
h2 {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.panel-heading p,
.conversation-header span,
.composer-actions span,
.session-time,
.message-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.panel-heading p {
  margin-top: 4px;
}

.agent-select {
  width: auto;
  margin: 0 16px 12px;
}

.option-model {
  float: right;
  margin-left: 24px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.session-list {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}

.session-item {
  display: flex;
  width: 100%;
  justify-content: space-between;
  gap: 8px;
  padding: 11px 10px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--el-text-color-regular);
  cursor: pointer;
  text-align: left;
}

.session-item:hover,
.session-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  flex: 0 0 auto;
}

.conversation-panel {
  display: flex;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
}

.conversation-header {
  min-height: 72px;
  padding: 0 22px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.conversation-header span {
  display: inline-block;
  margin-top: 5px;
}

.message-list {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 24px clamp(18px, 4vw, 64px);
  background: var(--el-bg-color-page);
}

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.message-row.user .message-avatar {
  background: var(--el-color-success);
}

.message-row.tool .message-avatar {
  background: var(--el-color-warning);
}

.message-content {
  max-width: min(76%, 760px);
}

.message-row.user .message-content {
  text-align: right;
}

.message-meta {
  margin: 0 0 5px;
}

.message-meta span {
  margin-left: 8px;
}

.message-bubble,
.tool-result {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  border-radius: 5px;
  padding: 10px 12px;
  line-height: 1.65;
  text-align: left;
}

.message-bubble {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  box-shadow: 0 1px 2px rgb(0 0 0 / 7%);
}

.message-row.user .message-bubble {
  background: var(--el-color-primary);
  color: #fff;
}

.tool-result {
  margin-top: 6px;
  border: 1px solid var(--el-color-warning-light-5);
  background: var(--el-color-warning-light-9);
  color: var(--el-text-color-regular);
}

.pending {
  color: var(--el-text-color-secondary);
}

.composer {
  padding: 16px 22px;
  border-top: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

@media (max-width: 768px) {
  .chat-page {
    height: calc(100vh - 64px);
    min-height: 560px;
  }

  .chat-layout {
    grid-template-columns: 1fr;
  }

  .session-panel {
    max-height: 180px;
    border-right: 0;
    border-bottom: 1px solid var(--el-border-color-light);
  }

  .panel-heading {
    padding: 10px 12px;
  }

  .panel-heading p {
    display: none;
  }

  .agent-select {
    margin: 0 12px 8px;
  }

  .session-list {
    display: flex;
    overflow-x: auto;
    padding: 0 8px 8px;
  }

  .session-item {
    width: 160px;
    flex: 0 0 160px;
  }

  .conversation-header,
  .composer {
    padding-right: 14px;
    padding-left: 14px;
  }

  .message-list {
    padding: 16px;
  }

  .message-content {
    max-width: calc(100% - 42px);
  }
}
</style>
