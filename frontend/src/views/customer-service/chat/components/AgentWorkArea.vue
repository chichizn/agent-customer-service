<script setup lang="ts">
/**
 * Agent工作区组件
 * 展示用户画像、服务流程、知识召回、推理过程
 * 数据来源：后端 ChatService.sendMessage 返回的真实处理结果
 */

interface Props {
  aiResponse?: any
}

const props = defineProps<Props>()

const activeTab = ref('reasoning')

const userProfile = ref({
  userId: 'U202501020001',
  nickname: '演示用户',
  level: 'VIP会员',
  tags: ['高频购买', '价格敏感', '服装偏好'],
  recentOrders: 5,
  totalAmount: 2680,
  lastActive: '刚刚'
})

const serviceFlow = ref([
  { step: 1, name: '场景识别', status: 'pending', result: '等待输入' },
  { step: 2, name: '意图分析', status: 'pending', result: '等待输入' },
  { step: 3, name: '情绪判断', status: 'pending', result: '等待输入' },
  { step: 4, name: '知识检索', status: 'pending', result: '等待输入' },
  { step: 5, name: '生成回复', status: 'pending', result: '等待输入' }
])

const knowledgeRecall = ref<any>({ products: [], faqs: [], activities: [] })

const reasoningProcess = ref<any[]>([])

const formatTime = (ts: string | null) => {
  if (!ts) return ''
  try {
    const d = new Date(ts)
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}`
  } catch {
    return ''
  }
}

watch(() => props.aiResponse, (res) => {
  if (!res) return

  console.log('[AgentWorkArea] 收到AI响应:', JSON.stringify(res).substring(0, 500))

  const totalKnowledge = (res.knowledgeRecall?.products?.length || 0)
    + (res.knowledgeRecall?.faqs?.length || 0)
    + (res.knowledgeRecall?.activities?.length || 0)

  serviceFlow.value = [
    { step: 1, name: '场景识别', status: 'completed', result: res.sceneType || '通用咨询' },
    { step: 2, name: '意图分析', status: 'completed', result: res.intent?.intentName || '通用咨询' },
    { step: 3, name: '情绪判断', status: 'completed', result: res.emotion || '中性' },
    { step: 4, name: '知识检索', status: 'completed', result: `检索到${totalKnowledge}条相关知识` },
    { step: 5, name: '生成回复', status: 'completed', result: '已完成' }
  ]

  knowledgeRecall.value = {
    products: (res.knowledgeRecall?.products || []).map((p: any) => ({
      id: p.productId,
      name: p.productName,
      score: p.score ?? 0.8,
      source: p.source || '知识库'
    })),
    faqs: (res.knowledgeRecall?.faqs || []).map((f: any) => ({
      id: f.faqId,
      question: f.question,
      answer: f.answer,
      score: f.score ?? 0.8
    })),
    activities: (res.knowledgeRecall?.activities || []).map((a: any) => ({
      id: a.activityId,
      name: a.activityName,
      score: a.score ?? 0.8
    }))
  }

  reasoningProcess.value = (res.reasoningProcess || []).map((r: any) => ({
    step: r.step,
    title: r.title,
    content: r.content,
    timestamp: formatTime(r.timestamp)
  }))

  userProfile.value.lastActive = '刚刚'
})
</script>

<template>
  <div class="agent-work-area">
    <!-- 用户画像区域 -->
    <div class="user-profile-section">
      <div class="section-header">
        <span class="section-title">👤 用户画像</span>
      </div>
      <div class="user-profile-content">
        <div class="profile-main">
          <div class="profile-avatar">👤</div>
          <div class="profile-info">
            <div class="profile-nickname">{{ userProfile.nickname }}</div>
            <el-tag size="small" type="warning">{{ userProfile.level }}</el-tag>
          </div>
        </div>
        <div class="profile-stats">
          <div class="stat-item">
            <div class="stat-value">{{ userProfile.recentOrders }}</div>
            <div class="stat-label">近期订单</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">¥{{ userProfile.totalAmount }}</div>
            <div class="stat-label">累计消费</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ userProfile.lastActive }}</div>
            <div class="stat-label">最近活跃</div>
          </div>
        </div>
        <div class="profile-tags">
          <el-tag v-for="tag in userProfile.tags" :key="tag" size="small">
            {{ tag }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 服务流程区域 -->
    <div class="service-flow-section">
      <div class="section-header">
        <span class="section-title">🔄 服务流程</span>
      </div>
      <div class="flow-steps">
        <div
          v-for="item in serviceFlow"
          :key="item.step"
          class="flow-step"
          :class="[`flow-step--${item.status}`]"
        >
          <div class="flow-step__indicator">
            <span v-if="item.status === 'completed'">✓</span>
            <span v-else-if="item.status === 'active'" class="loading-spinner"></span>
            <span v-else>{{ item.step }}</span>
          </div>
          <div class="flow-step__content">
            <div class="flow-step__name">{{ item.name }}</div>
            <div class="flow-step__result">{{ item.result }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab切换区域 -->
    <div class="detail-section">
      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane label="推理过程" name="reasoning">
          <div v-if="reasoningProcess.length === 0" class="empty-hint">
            💡 发送消息后，这里将展示 AI 的推理过程
          </div>
          <div v-else class="reasoning-list">
            <div
              v-for="item in reasoningProcess"
              :key="item.step"
              class="reasoning-item"
            >
              <div class="reasoning-item__step">{{ item.step }}</div>
              <div class="reasoning-item__content">
                <div class="reasoning-item__title">
                  {{ item.title }}
                  <span class="reasoning-item__time">{{ item.timestamp }}</span>
                </div>
                <div class="reasoning-item__desc">{{ item.content }}</div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="知识召回" name="knowledge">
          <div v-if="knowledgeRecall.products.length === 0 && knowledgeRecall.faqs.length === 0 && knowledgeRecall.activities.length === 0" class="empty-hint">
            💡 发送消息后，这里将展示知识召回结果
          </div>
          <div v-else class="knowledge-recall">
            <!-- 商品召回 -->
            <div v-if="knowledgeRecall.products.length > 0" class="recall-group">
              <div class="recall-group__title">🛍️ 商品召回</div>
              <div
                v-for="item in knowledgeRecall.products"
                :key="item.id"
                class="recall-item"
              >
                <div class="recall-item__name">{{ item.name }}</div>
                <div class="recall-item__meta">
                  <span class="recall-item__source">{{ item.source }}</span>
                  <span class="recall-item__score">{{ (item.score * 100).toFixed(0) }}%</span>
                </div>
              </div>
            </div>

            <!-- FAQ召回 -->
            <div v-if="knowledgeRecall.faqs.length > 0" class="recall-group">
              <div class="recall-group__title">❓ FAQ召回</div>
              <div
                v-for="item in knowledgeRecall.faqs"
                :key="item.id"
                class="recall-item"
              >
                <div class="recall-item__name">{{ item.question }}</div>
                <div class="recall-item__answer">{{ item.answer }}</div>
                <div class="recall-item__meta">
                  <span class="recall-item__score">匹配度：{{ (item.score * 100).toFixed(0) }}%</span>
                </div>
              </div>
            </div>

            <!-- 活动召回 -->
            <div v-if="knowledgeRecall.activities.length > 0" class="recall-group">
              <div class="recall-group__title">🎁 活动召回</div>
              <div
                v-for="item in knowledgeRecall.activities"
                :key="item.id"
                class="recall-item"
              >
                <div class="recall-item__name">{{ item.name }}</div>
                <div class="recall-item__meta">
                  <span class="recall-item__score">{{ (item.score * 100).toFixed(0) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.agent-work-area {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.section-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

// 用户画像区域
.user-profile-section {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.user-profile-content {
  padding: 16px;
}

.profile-main {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.profile-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #e6f7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 12px;
}

.profile-nickname {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.profile-stats {
  display: flex;
  justify-content: space-between;
  padding: 12px;
  background-color: #fafafa;
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-item {
  text-align: center;

  .stat-value {
    font-size: 16px;
    font-weight: 600;
    color: #1890ff;
  }

  .stat-label {
    font-size: 11px;
    color: #999;
    margin-top: 2px;
  }
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

// 服务流程区域
.service-flow-section {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.flow-steps {
  display: flex;
  padding: 16px;
  overflow-x: auto;
  gap: 8px;
}

.flow-step {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background-color: #f5f5f5;
  border-radius: 8px;
  min-width: 120px;
  flex-shrink: 0;

  &--completed {
    background-color: #f6ffed;

    .flow-step__indicator {
      background-color: #52c41a;
      color: #fff;
    }
  }

  &--active {
    background-color: #e6f7ff;

    .flow-step__indicator {
      background-color: #1890ff;
      color: #fff;
    }
  }

  &__indicator {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background-color: #d9d9d9;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    margin-right: 8px;
    flex-shrink: 0;
  }

  &__content {
    min-width: 0;
  }

  &__name {
    font-size: 12px;
    font-weight: 500;
    color: #333;
  }

  &__result {
    font-size: 11px;
    color: #666;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.loading-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid #fff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

// Tab详情区域
.detail-section {
  flex: 1;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.el-tabs__header) {
    margin: 0;
    padding: 0 16px;
    flex-shrink: 0;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    overflow: hidden;
  }

  :deep(.el-tab-pane) {
    height: 100%;
    overflow-y: auto;
    padding: 16px;
  }
}

.empty-hint {
  padding: 24px 16px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

// 推理过程
.reasoning-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reasoning-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  background-color: #fafafa;
  border-radius: 8px;

  &__step {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background-color: #1890ff;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    margin-right: 12px;
    flex-shrink: 0;
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
    font-weight: 500;
    color: #333;
    margin-bottom: 4px;
  }

  &__time {
    font-size: 11px;
    color: #999;
    font-weight: 400;
  }

  &__desc {
    font-size: 12px;
    color: #666;
    line-height: 1.5;
  }
}

// 知识召回
.knowledge-recall {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recall-group {
  &__title {
    font-size: 13px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;
  }
}

.recall-item {
  padding: 10px 12px;
  background-color: #fafafa;
  border-radius: 6px;
  margin-bottom: 8px;

  &__name {
    font-size: 13px;
    font-weight: 500;
    color: #333;
    margin-bottom: 4px;
  }

  &__answer {
    font-size: 12px;
    color: #666;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: #999;
  }

  &__score {
    color: #1890ff;
    font-weight: 500;
  }
}
</style>

