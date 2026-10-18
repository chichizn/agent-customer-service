<script setup lang="ts">
/**
 * 知识中心 - Tab切换多类型知识管理
 * 调用后端 KnowledgeController 接口获取统计数据
 */

import ProductKnowledge from './tabs/ProductKnowledge.vue'
import ActivityKnowledge from './tabs/ActivityKnowledge.vue'
import FaqKnowledge from './tabs/FaqKnowledge.vue'
import SolutionKnowledge from './tabs/SolutionKnowledge.vue'
import IndustryKnowledge from './tabs/IndustryKnowledge.vue'
import { fetchGetOverview } from '@/api/customer-service/knowledge'

// 当前激活的tab
const activeTab = ref('product')

// Tab配置
const tabs = [
  { name: 'product', label: '商品知识', icon: '🛍️', desc: '对话时作为商品上下文注入LLM' },
  { name: 'activity', label: '活动知识', icon: '🎁', desc: '对话时作为优惠上下文注入LLM' },
  { name: 'faq', label: 'FAQ问答', icon: '❓', desc: '对话时关键词匹配，命中后作为回复参考' },
  { name: 'industry', label: '行业知识（模拟）', icon: '📚', desc: 'CRUD真实，未接入对话链路，预留RAG扩展' },
  { name: 'solution', label: '解决方案（模拟）', icon: '🎯', desc: 'CRUD真实，未接入对话链路，预留流程编排' }
]

// 知识概览统计
const stats = ref({
  solution: 0,
  product: 0,
  activity: 0,
  faq: 0,
  industry: 0
})

// 总知识数
const totalKnowledge = computed(() => {
  return Object.values(stats.value).reduce((a, b) => a + b, 0)
})

// 加载统计数据
const loadOverview = async () => {
  try {
    const res = await fetchGetOverview()
    stats.value = {
      solution: res.solutionCount || 0,
      product: res.productCount || 0,
      activity: res.activityCount || 0,
      faq: res.faqCount || 0,
      industry: res.industryCount || 0
    }
  } catch {
    // 后端不可用时保持默认值
  }
}

onMounted(() => {
  loadOverview()
})
</script>

<template>
  <div class="knowledge-center">
    <!-- 头部统计 -->
    <div class="knowledge-header">
      <div class="header-title">
        <h2>📚 知识中心</h2>
        <p>管理和维护客服系统的知识库，提升智能回复的准确性</p>
      </div>
      <div class="header-stats">
        <div class="stat-card stat-card--total">
          <div class="stat-card__value">{{ totalKnowledge }}</div>
          <div class="stat-card__label">总知识数</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__value">{{ stats.product }}</div>
          <div class="stat-card__label">商品知识</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__value">{{ stats.faq }}</div>
          <div class="stat-card__label">FAQ问答</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__value">{{ stats.activity }}</div>
          <div class="stat-card__label">活动知识</div>
        </div>
      </div>
    </div>

    <!-- Tab内容 -->
    <div class="knowledge-content">
      <el-tabs v-model="activeTab" class="knowledge-tabs">
        <el-tab-pane
          v-for="tab in tabs"
          :key="tab.name"
          :name="tab.name"
        >
          <template #label>
            <el-tooltip :content="tab.desc" placement="top" effect="light" :show-after="500" :offset="8">
              <span class="tab-label-text">{{ tab.icon }} {{ tab.label }}</span>
            </el-tooltip>
          </template>
          <template v-if="activeTab === 'solution'">
            <SolutionKnowledge />
          </template>
          <template v-else-if="activeTab === 'product'">
            <ProductKnowledge />
          </template>
          <template v-else-if="activeTab === 'activity'">
            <ActivityKnowledge />
          </template>
          <template v-else-if="activeTab === 'faq'">
            <FaqKnowledge />
          </template>
          <template v-else-if="activeTab === 'industry'">
            <IndustryKnowledge />
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.knowledge-center {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.knowledge-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-shrink: 0;

  .header-title {
    h2 {
      margin: 0 0 8px 0;
      font-size: 22px;
      color: #333;
    }

    p {
      margin: 0;
      color: #666;
      font-size: 14px;
    }
  }

  .header-stats {
    display: flex;
    gap: 16px;
  }
}

.stat-card {
  padding: 16px 24px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  text-align: center;
  min-width: 100px;

  &--total {
    background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
    
    .stat-card__value,
    .stat-card__label {
      color: #fff;
    }
  }

  &__value {
    font-size: 28px;
    font-weight: 700;
    color: #1890ff;
    line-height: 1.2;
  }

  &__label {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }
}

.knowledge-content {
  flex: 1;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.knowledge-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.el-tabs__header) {
    margin: 0;
    padding: 0 20px;
    background-color: #fafafa;
    border-bottom: 1px solid #f0f0f0;
  }

  :deep(.el-tabs__item) {
    font-size: 14px;
    padding: 0 20px;
    height: 50px;
    line-height: 50px;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    overflow: hidden;
    padding: 0;
  }

  :deep(.el-tab-pane) {
    height: 100%;
    overflow-y: auto;
    padding: 20px;
  }
}
</style>
