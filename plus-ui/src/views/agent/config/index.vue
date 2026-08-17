<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="Agent 名称" prop="agentName">
              <el-input v-model="queryParams.agentName" placeholder="请输入 Agent 名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="Agent 编码" prop="agentCode">
              <el-input v-model="queryParams.agentCode" placeholder="请输入 Agent 编码" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
                <el-option label="正常" value="0" />
                <el-option label="停用" value="1" />
              </el-select>
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
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button v-hasPermi="['agent:config:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['agent:config:edit']" type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()">
              修改
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['agent:config:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">
              删除
            </el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="agentList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="Agent 名称" prop="agentName" min-width="150" :show-overflow-tooltip="true" />
        <el-table-column label="编码" prop="agentCode" min-width="130" :show-overflow-tooltip="true" />
        <el-table-column label="供应商" prop="provider" width="110" />
        <el-table-column label="模型" prop="modelName" min-width="150" :show-overflow-tooltip="true" />
        <el-table-column label="温度" prop="temperature" width="85" align="center" />
        <el-table-column label="最大 Token" prop="maxTokens" width="110" align="center" />
        <el-table-column label="业务工具" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.enableTool === '1' ? 'success' : 'info'">
              {{ scope.row.enableTool === '1' ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
              {{ scope.row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180" align="center">
          <template #default="scope">
            {{ proxy?.parseTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="修改" placement="top">
              <el-button v-hasPermi="['agent:config:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['agent:config:remove']" link type="primary" icon="Delete" @click="handleDelete(scope.row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" width="720px" append-to-body>
      <el-form ref="agentFormRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Agent 名称" prop="agentName">
              <el-input v-model="form.agentName" placeholder="例如：简历项目助手" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Agent 编码" prop="agentCode">
              <el-input v-model="form.agentCode" placeholder="例如：resume_agent" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型供应商" prop="provider">
              <el-select v-model="form.provider" class="w-full">
                <el-option label="DeepSeek" value="deepseek" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型名称" prop="modelName">
              <el-input v-model="form.modelName" placeholder="deepseek-v4-pro" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="温度" prop="temperature">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="2" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大 Token" prop="maxTokens">
              <el-input-number v-model="form.maxTokens" :min="1" :max="32768" :step="256" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用业务工具" prop="enableTool">
              <el-radio-group v-model="form.enableTool">
                <el-radio value="1">启用</el-radio>
                <el-radio value="0">关闭</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="系统提示词" prop="systemPrompt">
              <el-input v-model="form.systemPrompt" type="textarea" :rows="5" placeholder="用于定义 Agent 的角色、能力和回答风格" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="AgentConfig" lang="ts">
import { addAgentConfig, delAgentConfig, getAgentConfig, listAgentConfig, updateAgentConfig } from '@/api/agent/config';
import { AgentConfigForm, AgentConfigQuery, AgentConfigVO } from '@/api/agent/config/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const agentList = ref<AgentConfigVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<number[]>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const queryFormRef = ref<ElFormInstance>();
const agentFormRef = ref<ElFormInstance>();
const dialog = reactive<DialogOption>({ visible: false, title: '' });

const initFormData: AgentConfigForm = {
  id: undefined,
  agentName: '',
  agentCode: '',
  provider: 'deepseek',
  modelName: 'deepseek-v4-pro',
  systemPrompt: '',
  temperature: 0.7,
  maxTokens: 2048,
  enableTool: '1',
  status: '0',
  remark: ''
};

const data = reactive<PageData<AgentConfigForm, AgentConfigQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    agentName: '',
    agentCode: '',
    provider: '',
    status: ''
  },
  rules: {
    agentName: [{ required: true, message: 'Agent 名称不能为空', trigger: 'blur' }],
    agentCode: [{ required: true, message: 'Agent 编码不能为空', trigger: 'blur' }],
    provider: [{ required: true, message: '请选择模型供应商', trigger: 'change' }],
    modelName: [{ required: true, message: '模型名称不能为空', trigger: 'blur' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

const getList = async () => {
  loading.value = true;
  try {
    const res = await listAgentConfig(queryParams.value);
    agentList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

const reset = () => {
  form.value = { ...initFormData };
  agentFormRef.value?.resetFields();
};

const cancel = () => {
  reset();
  dialog.visible = false;
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

const handleSelectionChange = (selection: AgentConfigVO[]) => {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length !== 1;
  multiple.value = selection.length === 0;
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '新增 Agent 配置';
};

const handleUpdate = async (row?: AgentConfigVO) => {
  reset();
  const id = row?.id || ids.value[0];
  const res = await getAgentConfig(id);
  Object.assign(form.value, res.data);
  dialog.visible = true;
  dialog.title = '修改 Agent 配置';
};

const submitForm = () => {
  agentFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    form.value.id ? await updateAgentConfig(form.value) : await addAgentConfig(form.value);
    proxy?.$modal.msgSuccess('操作成功');
    dialog.visible = false;
    await getList();
  });
};

const handleDelete = async (row?: AgentConfigVO) => {
  const deleteIds = row?.id || ids.value;
  await proxy?.$modal.confirm(`是否确认删除 Agent 配置编号为“${deleteIds}”的数据项？`);
  await delAgentConfig(deleteIds);
  proxy?.$modal.msgSuccess('删除成功');
  await getList();
};

onMounted(() => {
  getList();
});
</script>
