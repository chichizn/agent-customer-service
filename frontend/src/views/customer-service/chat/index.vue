<script setup lang="ts">
/**
 * 对话中心 - 四栏布局
 * 1. 常见问题侧边栏
 * 2. 对话区域（iPhone模拟器样式）
 * 3. 知识选择区域
 * 4. Agent工作区（推理过程+知识召回）
 */

import QuestionSidebar from './components/QuestionSidebar.vue'
import ChatInterface from './components/ChatInterface.vue'
import KnowledgeSelect from './components/KnowledgeSelect.vue'
import AgentWorkArea from './components/AgentWorkArea.vue'

// 选中的问题
const selectedQuestion = ref<string>('')
const selectedQuestionId = ref<string>('')

// 知识选择
const knowledgeSelection = ref<{ selectedProducts: number[]; selectedActivities: number[] }>({
  selectedProducts: [],
  selectedActivities: []
})

// 最新一次的 AI 响应（传给 AgentWorkArea 展示）
const lastAiResponse = ref<any>(null)

// 处理问题选择
const handleQuestionSelect = (question: { id: string; text: string }) => {
  selectedQuestion.value = question.text
  selectedQuestionId.value = question.id
}

// 问题发送后清空选中
const handleQuestionSent = () => {
  selectedQuestion.value = ''
  selectedQuestionId.value = ''
}

// 知识选择变化
const handleKnowledgeChange = (data: { selectedProducts: number[]; selectedActivities: number[] }) => {
  knowledgeSelection.value = data
}

// AI 响应更新
const handleAiResponse = (response: any) => {
  lastAiResponse.value = response
}
</script>

<template>
  <div class="chat-center">
    <!-- 1. 常见问题侧边栏 -->
    <div class="chat-center__sidebar">
      <QuestionSidebar
        :selected-question="selectedQuestion"
        @select="handleQuestionSelect"
      />
    </div>

    <!-- 2. 对话区域 - iPhone模拟器样式 -->
    <div class="chat-center__chat">
      <ChatInterface
        :selected-question="selectedQuestion"
        :selected-question-id="selectedQuestionId"
        :knowledge-selection="knowledgeSelection"
        @question-sent="handleQuestionSent"
        @ai-response="handleAiResponse"
      />
    </div>

    <!-- 3. 知识选择区域 -->
    <div class="chat-center__knowledge">
      <KnowledgeSelect @selection-change="handleKnowledgeChange" />
    </div>

    <!-- 4. Agent工作区 -->
    <div class="chat-center__agent">
      <AgentWorkArea :ai-response="lastAiResponse" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.chat-center {
  display: flex;
  gap: 12px;
  padding: 12px;
  background-color: #f7f5ee;
  height: calc(100vh - 120px);
  width: 100%;
  box-sizing: border-box;

  &__sidebar {
    width: 18%;
    min-width: 200px;
    height: 100%;
    flex-shrink: 0;
  }

  &__chat {
    width: 22%;
    min-width: 340px;
    height: 100%;
    background-color: #f0f1f4;
    border: 6px solid rgb(204, 233, 255);
    border-radius: 40px;
    overflow: hidden;
    flex-shrink: 0;
  }

  &__knowledge {
    width: 20%;
    min-width: 200px;
    height: 100%;
    flex-shrink: 0;
  }

  &__agent {
    flex: 1;
    min-width: 300px;
    height: 100%;
  }
}

// 响应式适配
@media (max-width: 1400px) {
  .chat-center {
    &__sidebar {
      width: 16%;
      min-width: 180px;
    }

    &__chat {
      width: 24%;
    }

    &__knowledge {
      width: 18%;
      min-width: 180px;
    }
  }
}

@media (max-width: 1200px) {
  .chat-center {
    flex-wrap: wrap;

    &__sidebar {
      width: 100%;
      height: auto;
      max-height: 150px;
    }

    &__chat {
      width: 35%;
      height: calc(100vh - 300px);
    }

    &__knowledge {
      width: 30%;
      height: calc(100vh - 300px);
    }

    &__agent {
      width: 30%;
      height: calc(100vh - 300px);
    }
  }
}
</style>
