<script setup lang="ts">
/**
 * 场景规则管理 - 调用后端 SceneController 接口
 */
import { fetchListRules, fetchDeleteRule, fetchEnableRule, fetchDisableRule } from '@/api/customer-service/scene'

interface Rule {
  id: number
  ruleName: string
  ruleCode: string
  ruleType: string
  description: string
  triggerCount: number
  priority: number
  enabled: boolean
  effectiveTime: string
  expireTime: string
  updatedAt: string
}

const tableData = ref<Rule[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentRule = ref<Rule | null>(null)

const loadData = async () => {
  loading.value = true
  try {
    const res = await fetchListRules({})
    tableData.value = res.records || []
  } catch {
    // 后端不可用时保持空数据
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })

const handleAdd = () => {
  currentRule.value = null
  dialogVisible.value = true
}

const handleEdit = (row: Rule) => {
  currentRule.value = row
  dialogVisible.value = true
}

const handleDelete = (row: Rule) => {
  ElMessageBox.confirm('确定要删除该规则吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await fetchDeleteRule(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

const handleToggleEnabled = async (row: Rule) => {
  try {
    if (row.enabled) {
      await fetchDisableRule(row.id)
    } else {
      await fetchEnableRule(row.id)
    }
    row.enabled = !row.enabled
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  } catch {
    ElMessage.error('操作失败')
  }
}

const getRuleTypeColor = (type: string) => {
  const colorMap: Record<string, string> = {
    '路由规则': 'primary',
    '安全规则': 'danger',
    '转接规则': 'warning',
    '时间规则': 'info'
  }
  return colorMap[type] || ''
}
</script>

<template>
  <div class="rule-config">
    <!-- 操作栏 -->
    <div class="action-bar">
      <el-input
        placeholder="搜索规则名称..."
        style="width: 250px; margin-right: 16px"
        clearable
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select placeholder="规则类型" style="width: 150px; margin-right: 16px" clearable>
        <el-option label="路由规则" value="路由规则" />
        <el-option label="安全规则" value="安全规则" />
        <el-option label="转接规则" value="转接规则" />
        <el-option label="时间规则" value="时间规则" />
      </el-select>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增规则
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" :loading="loading" stripe>
      <el-table-column prop="ruleName" label="规则名称" width="180" />
      <el-table-column prop="ruleCode" label="规则编码" width="160">
        <template #default="{ row }">
          <code class="rule-code">{{ row.ruleCode }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="ruleType" label="规则类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getRuleTypeColor(row.ruleType)" size="small">
            {{ row.ruleType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="规则描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="priority" label="优先级" width="80">
        <template #default="{ row }">
          <span class="priority-badge">P{{ row.priority }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="triggerCount" label="触发次数" width="100" sortable>
        <template #default="{ row }">
          <span class="trigger-count">{{ row.triggerCount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="180">
        <template #default="{ row }">
          <span class="date-range">{{ row.effectiveTime }} ~ {{ row.expireTime }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled"
            @change="handleToggleEnabled(row)"
          />
        </template>
      </el-table-column>
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
        :total="30"
        :page-sizes="[10, 20, 50]"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.rule-config {
  height: 100%;
}

.action-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.rule-code {
  background-color: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

.priority-badge {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background-color: #1890ff;
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.trigger-count {
  color: #52c41a;
  font-weight: 600;
}

.date-range {
  font-size: 12px;
  color: #666;
}

.pagination-area {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

