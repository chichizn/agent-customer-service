<script setup lang="ts">
/**
 * 提示词模板管理 - 调用后端 SceneController 接口
 */
import { fetchListPrompts, fetchDeletePrompt } from '@/api/customer-service/scene'

interface Prompt {
  id: number
  promptName: string
  promptCode: string
  promptType: string
  description: string
  usageCount: number
  version: string
  status: string
  updatedAt: string
}

const tableData = ref<Prompt[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const testDialogVisible = ref(false)
const currentPrompt = ref<Prompt | null>(null)

const loadData = async () => {
  loading.value = true
  try {
    const res = await fetchListPrompts({})
    tableData.value = res.records || []
  } catch {
    // 后端不可用时保持空数据
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })

const handleAdd = () => {
  currentPrompt.value = null
  dialogVisible.value = true
}

const handleEdit = (row: Prompt) => {
  currentPrompt.value = row
  dialogVisible.value = true
}

const handleTest = (row: Prompt) => {
  currentPrompt.value = row
  testDialogVisible.value = true
}

const handleDelete = (row: Prompt) => {
  ElMessageBox.confirm('确定要删除该提示词模板吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await fetchDeletePrompt(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

const getPromptTypeTag = (type: string) => {
  const typeMap: Record<string, { label: string; type: 'primary' | 'success' | 'warning' }> = {
    SYSTEM: { label: '系统', type: 'primary' },
    USER: { label: '用户', type: 'success' },
    ASSISTANT: { label: '助手', type: 'warning' }
  }
  return typeMap[type] || { label: type, type: 'primary' }
}
</script>

<template>
  <div class="prompt-config">
    <!-- 操作栏 -->
    <div class="action-bar">
      <el-input
        placeholder="搜索提示词名称..."
        style="width: 250px; margin-right: 16px"
        clearable
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增提示词
      </el-button>
    </div>

    <!-- 卡片列表 -->
    <div class="prompt-grid">
      <div
        v-for="item in tableData"
        :key="item.id"
        class="prompt-card"
      >
        <div class="prompt-card__header">
          <div class="prompt-card__title">
            <span class="prompt-card__name">{{ item.promptName }}</span>
            <el-tag :type="getPromptTypeTag(item.promptType).type" size="small">
              {{ getPromptTypeTag(item.promptType).label }}
            </el-tag>
          </div>
          <div class="prompt-card__version">{{ item.version }}</div>
        </div>

        <div class="prompt-card__code">
          <code>{{ item.promptCode }}</code>
        </div>

        <div class="prompt-card__desc">{{ item.description }}</div>

        <div class="prompt-card__stats">
          <div class="stat-item">
            <span class="stat-icon">📊</span>
            <span class="stat-value">{{ item.usageCount }}</span>
            <span class="stat-label">使用次数</span>
          </div>
          <div class="stat-item">
            <span class="stat-icon">🕐</span>
            <span class="stat-value">{{ item.updatedAt.split(' ')[0] }}</span>
            <span class="stat-label">更新日期</span>
          </div>
        </div>

        <div class="prompt-card__footer">
          <el-button type="primary" link size="small" @click="handleTest(item)">
            <el-icon><VideoPlay /></el-icon>
            测试
          </el-button>
          <el-button type="primary" link size="small" @click="handleEdit(item)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button type="danger" link size="small" @click="handleDelete(item)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.prompt-config {
  height: 100%;
}

.action-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.prompt-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.prompt-card {
  background-color: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
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

  &__title {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__name {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }

  &__version {
    font-size: 12px;
    color: #999;
    background-color: #f0f0f0;
    padding: 2px 8px;
    border-radius: 10px;
  }

  &__code {
    margin-bottom: 12px;

    code {
      background-color: #e6f7ff;
      color: #1890ff;
      padding: 4px 10px;
      border-radius: 4px;
      font-size: 12px;
    }
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
    gap: 20px;
    padding: 12px;
    background-color: #fff;
    border-radius: 8px;
    margin-bottom: 16px;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .stat-icon {
      font-size: 14px;
    }

    .stat-value {
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }

    .stat-label {
      font-size: 11px;
      color: #999;
    }
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
  }
}
</style>

