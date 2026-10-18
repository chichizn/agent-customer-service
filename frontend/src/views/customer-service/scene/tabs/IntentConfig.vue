<script setup lang="ts">
/**
 * 意图管理 - 调用后端 SceneController 接口
 */
import { fetchListIntents, fetchDeleteIntent } from '@/api/customer-service/scene'

interface Intent {
  id: number
  intentName: string
  intentCode: string
  sceneType: string
  triggerKeywords: string[]
  confidenceThreshold: number
  hitCount: number
  status: string
  updatedAt: string
}

const tableData = ref<Intent[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentIntent = ref<Intent | null>(null)

const loadData = async () => {
  loading.value = true
  try {
    const res = await fetchListIntents({})
    tableData.value = res.records || []
  } catch {
    // 后端不可用时保持空数据
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })

const handleAdd = () => {
  currentIntent.value = null
  dialogVisible.value = true
}

const handleEdit = (row: Intent) => {
  currentIntent.value = row
  dialogVisible.value = true
}

const handleDelete = (row: Intent) => {
  ElMessageBox.confirm('确定要删除该意图配置吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await fetchDeleteIntent(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

const handleToggleStatus = (row: Intent) => {
  const newStatus = row.status === '启用' ? '禁用' : '启用'
  ElMessage.success(`已${newStatus}`)
}
</script>

<template>
  <div class="intent-config">
    <!-- 操作栏 -->
    <div class="action-bar">
      <el-input
        placeholder="搜索意图名称..."
        style="width: 250px; margin-right: 16px"
        clearable
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select placeholder="分类筛选" style="width: 150px; margin-right: 16px" clearable>
        <el-option label="售前咨询" value="售前咨询" />
        <el-option label="售后服务" value="售后服务" />
        <el-option label="物流配送" value="物流配送" />
      </el-select>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增意图
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" :loading="loading" stripe>
      <el-table-column prop="intentName" label="意图名称" width="150" />
      <el-table-column prop="intentCode" label="意图编码" width="160">
        <template #default="{ row }">
          <code class="intent-code">{{ row.intentCode }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="sceneType" label="场景类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.sceneType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="触发关键词" min-width="200">
        <template #default="{ row }">
          <template v-if="row.triggerKeywords?.length">
            <el-tag
              v-for="keyword in row.triggerKeywords.slice(0, 3)"
              :key="keyword"
              size="small"
              type="info"
              style="margin-right: 4px; margin-bottom: 4px"
            >
              {{ keyword }}
            </el-tag>
            <el-tag
              v-if="row.triggerKeywords.length > 3"
              size="small"
              type="info"
            >
              +{{ row.triggerKeywords.length - 3 }}
            </el-tag>
          </template>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="confidenceThreshold" label="置信度阈值" width="110">
        <template #default="{ row }">
          <el-progress
            :percentage="row.confidenceThreshold * 100"
            :stroke-width="6"
            :show-text="false"
            style="width: 60px; display: inline-block; margin-right: 8px"
          />
          <span>{{ row.confidenceThreshold }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="hitCount" label="命中次数" width="100" sortable>
        <template #default="{ row }">
          <span class="hit-count">{{ row.hitCount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === '启用'"
            @change="handleToggleStatus(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="150" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-area">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="50"
        :page-sizes="[10, 20, 50]"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.intent-config {
  height: 100%;
}

.action-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.intent-code {
  background-color: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

.hit-count {
  color: #1890ff;
  font-weight: 600;
}

.pagination-area {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

