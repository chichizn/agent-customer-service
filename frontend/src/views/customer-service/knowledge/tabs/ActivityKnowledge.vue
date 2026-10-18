<script setup lang="ts">
/**
 * 活动知识管理 - 调用后端 KnowledgeController 接口
 */
import { fetchListActivities, fetchCreateActivity, fetchUpdateActivity, fetchDeleteActivity } from '@/api/customer-service/knowledge'

interface Activity {
  id: number
  activityName: string
  activityCode: string
  activityType: string
  description: string
  rules: string
  discountRate: number
  startTime: string
  endTime: string
  status: string
  vectorized: boolean
}

const tableData = ref<Activity[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增活动')
const submitting = ref(false)
const formRef = ref()
const editingId = ref<number | null>(null)

const formData = reactive({
  activityName: '',
  activityCode: '',
  activityType: '',
  description: '',
  rules: '',
  discountRate: undefined as number | undefined
})

const formRules = {
  activityName: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  activityCode: [{ required: true, message: '请输入活动编码', trigger: 'blur' }],
  activityType: [{ required: true, message: '请选择活动类型', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await fetchListActivities({})
    tableData.value = res.records || []
  } catch { /* 后端不可用时保持空数据 */ } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })

const resetForm = () => {
  editingId.value = null
  formData.activityName = ''
  formData.activityCode = ''
  formData.activityType = ''
  formData.description = ''
  formData.rules = ''
  formData.discountRate = undefined
}

const handleAdd = () => { resetForm(); dialogTitle.value = '新增活动'; dialogVisible.value = true }

const handleEdit = (row: Activity) => {
  editingId.value = row.id
  dialogTitle.value = '编辑活动'
  formData.activityName = row.activityName
  formData.activityCode = row.activityCode
  formData.activityType = row.activityType
  formData.description = row.description || ''
  formData.rules = row.rules || ''
  formData.discountRate = row.discountRate
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  try {
    if (editingId.value) {
      await fetchUpdateActivity({ id: editingId.value, ...formData })
      ElMessage.success('更新成功')
    } else {
      await fetchCreateActivity({ ...formData })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch { ElMessage.error('操作失败') } finally { submitting.value = false }
}

const handleDelete = (row: Activity) => {
  ElMessageBox.confirm('确定要删除该活动知识吗？', '提示', { type: 'warning' }).then(async () => {
    await fetchDeleteActivity(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}
</script>

<template>
  <div class="activity-knowledge">
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增活动
      </el-button>
    </div>

    <el-table :data="tableData" :loading="loading" stripe>
      <el-table-column prop="activityName" label="活动名称" width="150" />
      <el-table-column prop="description" label="活动描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="activityType" label="活动类型" width="120" />
      <el-table-column label="有效期" width="200">
        <template #default="{ row }">
          {{ row.startTime }} ~ {{ row.endTime }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === '进行中' ? 'success' : 'warning'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="向量化" width="100">
        <template #default="{ row }">
          <el-tag :type="row.vectorized ? 'success' : 'info'" size="small">{{ row.vectorized ? '已向量化' : '待向量化' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="活动名称" prop="activityName">
          <el-input v-model="formData.activityName" placeholder="如：满300减50" />
        </el-form-item>
        <el-form-item label="活动编码" prop="activityCode">
          <el-input v-model="formData.activityCode" placeholder="如：FULL-300" />
        </el-form-item>
        <el-form-item label="活动类型" prop="activityType">
          <el-select v-model="formData.activityType" placeholder="请选择" style="width: 100%">
            <el-option label="满减" value="满减" />
            <el-option label="折扣" value="折扣" />
            <el-option label="积分" value="积分" />
            <el-option label="赠品" value="赠品" />
          </el-select>
        </el-form-item>
        <el-form-item label="折扣率">
          <el-input-number v-model="formData.discountRate" :min="0" :max="1" :step="0.05" :precision="2" style="width: 200px" />
        </el-form-item>
        <el-form-item label="活动描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="活动描述，对话检索时会展示给用户" />
        </el-form-item>
        <el-form-item label="活动规则">
          <el-input v-model="formData.rules" type="textarea" :rows="2" placeholder="如：满300减50，满600减120" />
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
.activity-knowledge {
  height: 100%;
}

.action-bar {
  margin-bottom: 16px;
}
</style>

