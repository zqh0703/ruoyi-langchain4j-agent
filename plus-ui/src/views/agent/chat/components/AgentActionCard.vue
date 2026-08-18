<template>
  <section class="action-card" :class="`risk-${current.riskLevel.toLowerCase()}`">
    <header class="action-header">
      <div class="action-title">
        <el-icon><EditPen /></el-icon>
        <span>{{ current.summary }}</span>
      </div>
      <div class="action-tags">
        <el-tag size="small" :type="riskType">{{ riskLabel }}</el-tag>
        <el-tag size="small" :type="statusType">{{ statusLabel }}</el-tag>
      </div>
    </header>

    <el-collapse class="action-details">
      <el-collapse-item title="查看操作预览" name="preview">
        <pre>{{ previewText }}</pre>
        <p v-if="current.expiresAt && current.status === 'PENDING_CONFIRMATION'" class="expires">确认截止：{{ formatTime(current.expiresAt) }}</p>
        <p v-if="current.errorMessage" class="action-error">{{ current.errorCode }}：{{ current.errorMessage }}</p>
      </el-collapse-item>
    </el-collapse>

    <footer v-if="current.status === 'PENDING_CONFIRMATION'" class="action-buttons">
      <el-button v-hasPermi="['agent:action:cancel']" size="small" icon="Close" :loading="submitting" @click="handleCancel"> 取消 </el-button>
      <el-button v-hasPermi="['agent:action:confirm']" size="small" type="primary" icon="Check" :loading="submitting" @click="handleConfirm">
        确认执行
      </el-button>
    </footer>

    <el-dialog v-model="secretVisible" title="用户创建成功" width="460px" append-to-body :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" show-icon> 临时密码只显示这一次。关闭后无法再次查看，请立即交给对应用户。 </el-alert>
      <div class="secret-row">
        <el-input :model-value="secretValue" readonly />
        <el-tooltip content="复制临时密码" placement="top">
          <el-button type="primary" icon="CopyDocument" @click="copySecret" />
        </el-tooltip>
      </div>
      <template #footer>
        <el-button type="primary" @click="secretVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { cancelAgentAction, confirmAgentAction } from '@/api/agent/action';
import { AgentActionVO } from '@/api/agent/action/types';

const props = defineProps<{ action: AgentActionVO }>();
const emit = defineEmits<{ changed: [action: AgentActionVO] }>();

const current = ref<AgentActionVO>(props.action);
const submitting = ref(false);
const secretVisible = ref(false);
const secretValue = ref('');

watch(
  () => props.action,
  (value) => (current.value = value),
  { deep: true }
);

const statusLabels: Record<string, string> = {
  PENDING_CONFIRMATION: '待确认',
  EXECUTING: '执行中',
  SUCCESS: '已完成',
  FAILED: '失败',
  CANCELLED: '已取消',
  EXPIRED: '已过期'
};

const statusLabel = computed(() => statusLabels[current.value.status] || current.value.status);
const riskLabel = computed(() => (current.value.riskLevel === 'HIGH' ? '高风险' : '中风险'));
const riskType = computed(() => (current.value.riskLevel === 'HIGH' ? 'danger' : 'warning'));
const statusType = computed(() => {
  if (current.value.status === 'SUCCESS') return 'success';
  if (current.value.status === 'FAILED' || current.value.status === 'EXPIRED') return 'danger';
  if (current.value.status === 'CANCELLED') return 'info';
  return 'warning';
});
const previewText = computed(() => JSON.stringify(current.value.preview || {}, null, 2));

const formatTime = (value: string) => new Date(value).toLocaleString('zh-CN', { hour12: false });

const handleConfirm = async () => {
  await ElMessageBox.confirm(`确认执行：${current.value.summary}`, '确认 Agent 操作', {
    confirmButtonText: '确认执行',
    cancelButtonText: '返回检查',
    type: current.value.riskLevel === 'HIGH' ? 'error' : 'warning'
  });
  submitting.value = true;
  try {
    const res = await confirmAgentAction(current.value.id, { version: current.value.version });
    current.value = res.data.action;
    emit('changed', current.value);
    if (res.data.secretValue) {
      secretValue.value = res.data.secretValue;
      secretVisible.value = true;
    } else {
      ElMessage.success('操作已执行');
    }
  } finally {
    submitting.value = false;
  }
};

const handleCancel = async () => {
  await ElMessageBox.confirm(`取消操作：${current.value.summary}`, '取消 Agent 操作', {
    confirmButtonText: '确认取消',
    cancelButtonText: '返回',
    type: 'warning'
  });
  submitting.value = true;
  try {
    const res = await cancelAgentAction(current.value.id, { version: current.value.version });
    current.value = res.data;
    emit('changed', current.value);
    ElMessage.success('操作已取消');
  } finally {
    submitting.value = false;
  }
};

const copySecret = async () => {
  await navigator.clipboard.writeText(secretValue.value);
  ElMessage.success('临时密码已复制');
};
</script>

<style scoped lang="scss">
.action-card {
  margin-top: 8px;
  overflow: hidden;
  border: 1px solid var(--el-color-warning-light-5);
  border-left: 3px solid var(--el-color-warning);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.action-card.risk-high {
  border-color: var(--el-color-danger-light-5);
  border-left-color: var(--el-color-danger);
}

.action-header,
.action-buttons,
.secret-row {
  display: flex;
  align-items: center;
}

.action-header {
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
}

.action-title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.action-title span {
  overflow-wrap: anywhere;
}

.action-tags {
  display: flex;
  flex: 0 0 auto;
  gap: 5px;
}

.action-details {
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 0;
}

.action-details :deep(.el-collapse-item__header) {
  height: 38px;
  padding: 0 12px;
  border: 0;
  font-size: 12px;
}

.action-details :deep(.el-collapse-item__wrap) {
  border: 0;
}

.action-details :deep(.el-collapse-item__content) {
  padding: 0 12px 10px;
}

pre {
  max-height: 260px;
  overflow: auto;
  margin: 0;
  padding: 9px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  font:
    12px/1.55 Consolas,
    'Courier New',
    monospace;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.expires,
.action-error {
  margin: 8px 0 0;
  font-size: 12px;
}

.expires {
  color: var(--el-text-color-secondary);
}

.action-error {
  color: var(--el-color-danger);
}

.action-buttons {
  justify-content: flex-end;
  gap: 8px;
  padding: 0 12px 10px;
}

.secret-row {
  gap: 8px;
  margin-top: 16px;
}

@media (max-width: 600px) {
  .action-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
