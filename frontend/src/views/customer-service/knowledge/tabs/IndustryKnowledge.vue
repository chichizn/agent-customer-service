<script setup lang="ts">
/**
 * 行业知识管理 - 调用后端 KnowledgeController 接口
 */
import { fetchListIndustries, fetchCreateIndustry, fetchUpdateIndustry, fetchDeleteIndustry } from '@/api/customer-service/knowledge'

interface Industry {
  id: number
  title: string
  content: string
  industryType: string
  keywords: string[]
  source: string
  status: string
  updatedAt: string
}

const tableData = ref<Industry[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增知识')
const submitting = ref(false)
const formRef = ref()
const editingId = ref<number | null>(null)

const formData = reactive({
  title: '',
  industryType: '',
  content: '',
  source: '',
  keywords: '' as string
})

const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  industryType: [{ required: true, message: '请输入行业分类', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await fetchListIndustries({})
    tableData.value = res.records || []
  } catch { /* 后端不可用时保持空数据 */ } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })

const resetForm = () => {
  editingId.value = null
  formData.title = ''
  formData.industryType = ''
  formData.content = ''
  formData.source = ''
  formData.keywords = ''
}

const handleAdd = () => { resetForm(); dialogTitle.value = '新增知识'; dialogVisible.value = true }

const handleEdit = (row: Industry) => {
  editingId.value = row.id
  dialogTitle.value = '编辑知识'
  formData.title = row.title
  formData.industryType = row.industryType || ''
  formData.content = row.content || ''
  formData.source = row.source || ''
  formData.keywords = Array.isArray(row.keywords) ? row.keywords.join(',') : ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  const payload = {
    ...formData,
    keywords: formData.keywords ? formData.keywords.split(',').map((t: string) => t.trim()).filter(Boolean) : []
  }
  try {
    if (editingId.value) {
      await fetchUpdateIndustry({ id: editingId.value, ...payload })
      ElMessage.success('更新成功')
    } else {
      await fetchCreateIndustry(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch { ElMessage.error('操作失败') } finally { submitting.value = false }
}

const handleDelete = (row: Industry) => {
  ElMessageBox.confirm('确定要删除该行业知识吗？', '提示', { type: 'warning' }).then(async () => {
    await fetchDeleteIndustry(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}
</script>

<template>
  <div class="industry-knowledge">
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增知识
      </el-button>
    </div>

    <el-table :data="tableData" :loading="loading" stripe>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="content" label="内容摘要" min-width="250" show-overflow-tooltip />
      <el-table-column prop="industryType" label="行业分类" width="100" />
      <el-table-column label="标签" width="180">
        <template #default="{ row }">
          <el-tag v-for="tag in (row.keywords || [])" :key="tag" size="small" style="margin-right: 4px">{{ tag }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="source" label="来源" width="100" />
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
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="如：2025年智能家居行业趋势" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="行业分类" prop="industryType">
              <el-input v-model="formData.industryType" placeholder="如：智能家居" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源">
              <el-input v-model="formData.source" placeholder="如：艾瑞咨询" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="4" placeholder="知识内容" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="formData.keywords" placeholder="逗号分隔，如：智能家居,AI,物联网" />
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
.industry-knowledge {
  height: 100%;
}

.action-bar {
  margin-bottom: 16px;
}
</style>

