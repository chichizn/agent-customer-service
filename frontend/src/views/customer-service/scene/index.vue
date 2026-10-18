<script setup lang="ts">
/**
 * 场景配置 - 意图/提示词/规则管理
 */

import IntentConfig from './tabs/IntentConfig.vue'
import PromptConfig from './tabs/PromptConfig.vue'
import RuleConfig from './tabs/RuleConfig.vue'

// 当前激活的tab
const activeTab = ref('intent')

// Tab配置
const tabs = [
  { name: 'intent', label: '意图管理（模拟）', icon: '🎯', desc: 'CRUD真实，未接入对话链路，意图识别当前由LLM/关键词完成' },
  { name: 'prompt', label: '提示词模板（模拟）', icon: '📝', desc: 'CRUD真实，「测试」按钮真实调LLM，但对话链路使用硬编码提示词' },
  { name: 'rule', label: '场景规则（模拟）', icon: '⚙️', desc: 'CRUD真实，启用/禁用真实，未接入对话链路，预留规则引擎扩展' }
]
</script>

<template>
  <div class="scene-config">
    <!-- 头部 -->
    <div class="scene-header">
      <div class="header-title">
        <h2>⚙️ 场景配置</h2>
        <p>配置智能客服的意图识别、提示词模板和场景规则</p>
      </div>
    </div>

    <!-- Tab内容 -->
    <div class="scene-content">
      <el-tabs v-model="activeTab" class="scene-tabs">
        <el-tab-pane
          v-for="tab in tabs"
          :key="tab.name"
          :name="tab.name"
        >
          <template #label>
            <el-tooltip :content="tab.desc" placement="top" effect="light" :show-after="500" :offset="8">
              <div class="tab-label">
                <span class="tab-icon">{{ tab.icon }}</span>
                <span class="tab-text">{{ tab.label }}</span>
              </div>
            </el-tooltip>
          </template>

          <template v-if="activeTab === 'intent'">
            <IntentConfig />
          </template>
          <template v-else-if="activeTab === 'prompt'">
            <PromptConfig />
          </template>
          <template v-else-if="activeTab === 'rule'">
            <RuleConfig />
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.scene-config {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.scene-header {
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
}

.scene-content {
  flex: 1;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.scene-tabs {
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
    height: 56px;
    padding: 0 24px;
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

.tab-label {
  display: flex;
  align-items: center;
  gap: 8px;

  .tab-icon {
    font-size: 16px;
  }

  .tab-text {
    font-size: 14px;
  }

  .tab-badge {
    :deep(.el-badge__content) {
      font-size: 10px;
      height: 16px;
      line-height: 16px;
      padding: 0 5px;
    }
  }
}
</style>
