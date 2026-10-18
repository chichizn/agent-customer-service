<script setup lang="ts">
/**
 * 场景解决方案管理 - 调用后端 KnowledgeController 接口
 */
import { fetchListSolutions, fetchCreateSolution, fetchUpdateSolution, fetchDeleteSolution } from '@/api/customer-service/knowledge'

interface Solution {
  id: number
  solutionName: string
  solutionCode: string
  sceneType: string
  description: string
  triggerCondition: string
  expectedOutcome: string
  usageCount: number
  successRate: number
  status: string
  updatedAt: string
}

const tableData = ref<Solution[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增方案')
const submitting = ref(false)
const formRef = ref()
const editingId = ref<number | null>(null)

const formData = reactive({
  solutionName: '',
  solutionCode: '',
  sceneType: '',
  description: '',
  triggerCondition: '',
  expectedOutcome: ''
})

const formRules = {
  solutionName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  solutionCode: [{ required: true, message: '请输入方案编码', trigger: 'blur' }],
  sceneType: [{ required: true, message: '请选择场景类型', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await fetchListSolutions({})
    tableData.value = res.records || []
  } catch { /* 后端不可用时保持空数据 */ } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })

const resetForm = () => {
  editingId.value = null
  formData.solutionName = ''
  formData.solutionCode = ''
  formData.sceneType = ''
  formData.description = ''
  formData.triggerCondition = ''
  formData.expectedOutcome = ''
}

const handleAdd = () => { resetForm(); dialogTitle.value = '新增方案'; dialogVisible.value = true }

const handleEdit = (row: Solution) => {
  editingId.value = row.id
  dialogTitle.value = '编辑方案'
  formData.solutionName = row.solutionName
  formData.solutionCode = row.solutionCode || ''
  formData.sceneType = row.sceneType || ''
  formData.description = row.description || ''
  formData.triggerCondition = row.triggerCondition || ''
  formData.expectedOutcome = row.expectedOutcome || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  try {
    if (editingId.value) {
      await fetchUpdateSolution({ id: editingId.value, ...formData })
      ElMessage.success('更新成功')
    } else {
      await fetchCreateSolution({ ...formData })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch { ElMessage.error('操作失败') } finally { submitting.value = false }
}

const handleDelete = (row: Solution) => {
  ElMessageBox.confirm('确定要删除该解决方案吗？', '提示', { type: 'warning' }).then(async () => {
    await fetchDeleteSolution(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}
</script>

<template>
  <div class="solution-knowledge">
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增方案
      </el-button>
    </div>

    <div class="solution-grid">
      <div v-for="item in tableData" :key="item.id" class="solution-card">
        <div class="solution-card__header">
          <span class="solution-card__name">{{ item.solutionName }}</span>
          <el-tag size="small" type="success">{{ item.sceneType }}</el-tag>
        </div>
        <div class="solution-card__desc">{{ item.description }}</div>
        <div class="solution-card__stats">
          <div class="stat-item">
            <span class="stat-value">{{ item.usageCount }}</span>
            <span class="stat-label">使用次数</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ ((item.successRate || 0) * 100).toFixed(0) }}%</span>
            <span class="stat-label">成功率</span>
          </div>
        </div>
        <div class="solution-card__footer">
          <span class="update-time">{{ item.updatedAt }}</span>
          <div class="actions">
            <el-button type="primary" link size="small" @click="handleEdit(item)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(item)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="方案名称" prop="solutionName">
          <el-input v-model="formData.solutionName" placeholder="如：退货退款标准流程" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="方案编码" prop="solutionCode">
              <el-input v-model="formData.solutionCode" placeholder="如：REFUND-STANDARD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景类型" prop="sceneType">
              <el-select v-model="formData.sceneType" placeholder="请选择" style="width: 100%">
                <el-option label="售前咨询" value="售前咨询" />
                <el-option label="售后服务" value="售后服务" />
                <el-option label="物流配送" value="物流配送" />
                <el-option label="支付问题" value="支付问题" />
                <el-option label="通用咨询" value="通用咨询" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="方案描述" />
        </el-form-item>
        <el-form-item label="触发条件">
          <el-input v-model="formData.triggerCondition" placeholder="如：用户意图=退货退款" />
        </el-form-item>
        <el-form-item label="预期结果">
          <el-input v-model="formData.expectedOutcome" placeholder="如：用户成功提交退货申请" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.solution-knowledge {
  height: 100%;
}

.action-bar {
  margin-bottom: 16px;
}

.solution-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.solution-card {
  padding: 20px;
  background-color: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    border-color: #1890ff;
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  &__name {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    flex: 1;
    margin-right: 12px;
  }

  &__desc {
    font-size: 13px;
    color: #666;
    line-height: 1.6;
    margin-bottom: 16px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__stats {
    display: flex;
    gap: 24px;
    padding: 12px;
    background-color: #fff;
    border-radius: 8px;
    margin-bottom: 12px;

    .stat-item {
      text-align: center;
    }

    .stat-value {
      display: block;
      font-size: 18px;
      font-weight: 600;
      color: #1890ff;
    }

    .stat-label {
      font-size: 11px;
      color: #999;
    }
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
  }

  .update-time {
    font-size: 12px;
    color: #999;
  }

  .actions {
    display: flex;
    gap: 4px;
  }
}
</style>

