<template>
  <div class="run-page p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="Agent" prop="agentId">
              <el-select v-model="queryParams.agentId" placeholder="全部 Agent" clearable class="query-select">
                <el-option v-for="agent in agentList" :key="agent.id" :label="agent.agentName" :value="agent.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="执行状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="query-select">
                <el-option label="成功" value="0" />
                <el-option label="失败" value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="模型" prop="modelName">
              <el-input v-model="queryParams.modelName" placeholder="输入模型名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </transition>

    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10">
          <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="runList">
        <el-table-column label="运行 ID" prop="id" min-width="185" :show-overflow-tooltip="true" />
        <el-table-column label="Agent" min-width="145" :show-overflow-tooltip="true">
          <template #default="scope">{{ getAgentName(scope.row.agentId) }}</template>
        </el-table-column>
        <el-table-column label="模型" prop="modelName" min-width="150" :show-overflow-tooltip="true" />
        <el-table-column label="状态" width="88" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
              {{ scope.row.status === '0' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总耗时" prop="durationMs" width="110" align="right">
          <template #default="scope">{{ formatDuration(scope.row.durationMs) }}</template>
        </el-table-column>
        <el-table-column label="执行时间" prop="createTime" width="180" align="center">
          <template #default="scope">{{ proxy?.parseTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="88" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="查看执行轨迹" placement="top">
              <el-button v-hasPermi="['agent:run:query']" link type="primary" icon="View" @click="handleTrace(scope.row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-drawer v-model="drawerVisible" title="执行轨迹" size="760px" destroy-on-close class="trace-drawer">
      <div v-loading="traceLoading" class="trace-content">
        <template v-if="trace">
          <el-descriptions :column="3" border class="trace-summary">
            <el-descriptions-item label="Agent">{{ getAgentName(trace.runLog.agentId) }}</el-descriptions-item>
            <el-descriptions-item label="模型">{{ trace.runLog.modelName }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="trace.runLog.status === '0' ? 'success' : 'danger'">
                {{ trace.runLog.status === '0' ? '成功' : '失败' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="运行 ID" :span="2">{{ trace.runLog.id }}</el-descriptions-item>
            <el-descriptions-item label="总耗时">{{ formatDuration(trace.runLog.durationMs) }}</el-descriptions-item>
          </el-descriptions>

          <el-timeline>
            <el-timeline-item type="primary" :timestamp="formatStepTime(userMessage?.createTime || trace.runLog.createTime)">
              <section class="trace-step">
                <header class="step-header">
                  <span class="step-title"
                    ><el-icon><User /></el-icon>用户请求</span
                  >
                </header>
                <div class="step-body">{{ requestContent }}</div>
              </section>
            </el-timeline-item>

            <el-timeline-item v-for="message in toolMessages" :key="message.id" type="warning" :timestamp="formatStepTime(message.createTime)">
              <section class="trace-step">
                <header class="step-header">
                  <span class="step-title"
                    ><el-icon><Tools /></el-icon>{{ message.toolName || '业务工具' }}</span
                  >
                  <el-tag size="small" type="warning">{{ formatDuration(message.toolDurationMs) }}</el-tag>
                </header>
                <div class="tool-block">
                  <span>调用参数</span>
                  <pre>{{ formatJson(message.toolArgs) }}</pre>
                </div>
                <div class="tool-block">
                  <span>执行结果</span>
                  <pre>{{ formatJson(message.toolResult) }}</pre>
                </div>
              </section>
            </el-timeline-item>

            <el-timeline-item v-if="responseContent" type="success" :timestamp="formatStepTime(assistantMessage?.createTime)">
              <section class="trace-step">
                <header class="step-header">
                  <span class="step-title"
                    ><el-icon><ChatDotRound /></el-icon>模型回答</span
                  >
                </header>
                <div class="step-body">{{ responseContent }}</div>
              </section>
            </el-timeline-item>

            <el-timeline-item v-if="trace.runLog.status === '1'" type="danger">
              <section class="trace-step error-step">
                <header class="step-header">
                  <span class="step-title"
                    ><el-icon><Warning /></el-icon>执行异常</span
                  >
                </header>
                <div class="step-body">{{ trace.runLog.errorMsg || '执行失败，未记录异常详情' }}</div>
              </section>
            </el-timeline-item>
          </el-timeline>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="AgentRun" lang="ts">
import { ChatDotRound, Tools, User, Warning } from '@element-plus/icons-vue';
import { listAgentConfig } from '@/api/agent/config';
import type { AgentConfigVO } from '@/api/agent/config/types';
import type { AgentMessageVO } from '@/api/agent/chat/types';
import { getAgentRunTrace, listAgentRuns } from '@/api/agent/run';
import type { AgentRunLogQuery, AgentRunLogVO, AgentRunTraceVO } from '@/api/agent/run/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const loading = ref(false);
const traceLoading = ref(false);
const showSearch = ref(true);
const drawerVisible = ref(false);
const total = ref(0);
const runList = ref<AgentRunLogVO[]>([]);
const agentList = ref<AgentConfigVO[]>([]);
const trace = ref<AgentRunTraceVO>();
const queryFormRef = ref<ElFormInstance>();

const queryParams = reactive<AgentRunLogQuery>({
  pageNum: 1,
  pageSize: 10,
  agentId: undefined,
  status: '',
  modelName: ''
});

const userMessage = computed(() => trace.value?.messages.find((message) => message.role === 'user'));
const assistantMessage = computed(() => trace.value?.messages.find((message) => message.role === 'assistant'));
const toolMessages = computed(() => trace.value?.messages.filter((message) => message.role === 'tool') || []);
const requestContent = computed(() => userMessage.value?.content || trace.value?.runLog.requestBody || '');
const responseContent = computed(() => assistantMessage.value?.content || trace.value?.runLog.responseBody || '');

const getAgentName = (agentId: string | number) => {
  return agentList.value.find((agent) => String(agent.id) === String(agentId))?.agentName || String(agentId);
};

const formatDuration = (duration?: number) => {
  if (duration === undefined || duration === null) return '-';
  if (duration < 1000) return duration + ' ms';
  return (duration / 1000).toFixed(2) + ' s';
};

const formatStepTime = (value?: string) => {
  return value ? proxy?.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || '' : '';
};

const formatJson = (value?: string) => {
  if (!value) return '-';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
};

const getAgentList = async () => {
  const res = await listAgentConfig({ pageNum: 1, pageSize: 100, agentName: '', agentCode: '', provider: '', status: '' });
  agentList.value = res.rows;
};

const getList = async () => {
  loading.value = true;
  try {
    const res = await listAgentRuns(queryParams);
    runList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

const handleTrace = async (row: AgentRunLogVO) => {
  drawerVisible.value = true;
  traceLoading.value = true;
  trace.value = undefined;
  try {
    const res = await getAgentRunTrace(row.id);
    trace.value = res.data;
  } finally {
    traceLoading.value = false;
  }
};

onMounted(async () => {
  await getAgentList();
  await getList();
});
</script>

<style scoped>
.query-select {
  width: 180px;
}

.trace-content {
  min-height: 320px;
}

.trace-summary {
  margin-bottom: 28px;
}

.trace-step {
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.error-step {
  border-color: var(--el-color-danger-light-7);
}

.step-header {
  display: flex;
  min-height: 24px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.step-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.step-body {
  color: var(--el-text-color-regular);
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.tool-block + .tool-block {
  margin-top: 12px;
}

.tool-block > span {
  display: block;
  margin-bottom: 6px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.tool-block pre {
  max-height: 260px;
  margin: 0;
  padding: 10px 12px;
  overflow: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  font-family: Consolas, Monaco, monospace;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 768px) {
  .query-select {
    width: 100%;
  }

  :deep(.trace-drawer) {
    width: 100% !important;
  }

  :deep(.el-descriptions__body) {
    overflow-x: auto;
  }
}
</style>
