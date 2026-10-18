<script setup lang="ts">
/**
 * FAQ问答管理 - 调用后端 KnowledgeController 接口
 */
import { fetchListFaqs, fetchCreateFaq, fetchUpdateFaq, fetchDeleteFaq } from '@/api/customer-service/knowledge'

interface Faq {
  id: number
  question: string
  answer: string
  category: string
  tags: string[]
  priority: number
  hitCount: number
  status: string
  updatedAt: string
}

const searchKeyword = ref('')
const tableData = ref<Faq[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增FAQ')
const submitting = ref(false)
const formRef = ref()
const editingId = ref<number | null>(null)

const formData = reactive({
  question: '',
  answer: '',
  category: '',
  tags: '' as string,
  priority: 5
})

const formRules = {
  question: [{ required: true, message: '请输入问题', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await fetchListFaqs({ keyword: searchKeyword.value || undefined })
    tableData.value = res.records || []
  } catch { /* 后端不可用时保持空数据 */ } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })
const handleSearch = () => { loadData() }

const resetForm = () => {
  editingId.value = null
  formData.question = ''
  formData.answer = ''
  formData.category = ''
  formData.tags = ''
  formData.priority = 5
}

const handleAdd = () => { resetForm(); dialogTitle.value = '新增FAQ'; dialogVisible.value = true }

const handleEdit = (row: Faq) => {
  editingId.value = row.id
  dialogTitle.value = '编辑FAQ'
  formData.question = row.question
  formData.answer = row.answer
  formData.category = row.category || ''
  formData.tags = Array.isArray(row.tags) ? row.tags.join(',') : ''
  formData.priority = row.priority || 5
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  const payload = {
    ...formData,
    tags: formData.tags ? formData.tags.split(',').map((t: string) => t.trim()).filter(Boolean) : []
  }
  try {
    if (editingId.value) {
      await fetchUpdateFaq({ id: editingId.value, ...payload })
      ElMessage.success('更新成功')
    } else {
      await fetchCreateFaq(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch { ElMessage.error('操作失败') } finally { submitting.value = false }
}

const handleDelete = (row: Faq) => {
  ElMessageBox.confirm('确定要删除该FAQ吗？', '提示', { type: 'warning' }).then(async () => {
    await fetchDeleteFaq(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}
</script>

<template>
  <div class="faq-knowledge">
    <div class="action-bar">
      <el-input v-model="searchKeyword" placeholder="搜索问题..." style="width: 300px; margin-right: 16px" clearable
        @keyup.enter="handleSearch" @clear="handleSearch">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增FAQ
      </el-button>
    </div>

    <el-table :data="tableData" :loading="loading" stripe>
      <el-table-column prop="question" label="问题" min-width="250" show-overflow-tooltip />
      <el-table-column prop="answer" label="答案" min-width="300" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="hitCount" label="命中次数" width="100" sortable>
        <template #default="{ row }">
          <span class="hit-count">{{ row.hitCount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === '启用' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="150" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="60px">
        <el-form-item label="问题" prop="question">
          <el-input v-model="formData.question" placeholder="如：如何申请退换货？" />
        </el-form-item>
        <el-form-item label="答案" prop="answer">
          <el-input v-model="formData.answer" type="textarea" :rows="4" placeholder="详细的回答内容，对话匹配命中后会作为知识参考" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="formData.category" placeholder="请选择" style="width: 100%">
                <el-option label="售前咨询" value="售前咨询" />
                <el-option label="售后服务" value="售后服务" />
                <el-option label="物流配送" value="物流配送" />
                <el-option label="支付问题" value="支付问题" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-input-number v-model="formData.priority" :min="1" :max="10" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标签">
          <el-input v-model="formData.tags" placeholder="逗号分隔，如：退货,换货,退款" />
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
.faq-knowledge {
  height: 100%;
}

.action-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.hit-count {
  color: #1890ff;
  font-weight: 600;
}
</style>

