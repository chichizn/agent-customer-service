<script setup lang="ts">
/**
 * 对话界面组件 - iPhone模拟器样式
 * 核心对话交互区域，调用后端 ChatController 接口
 */
import { fetchCreateSession, fetchSendMessage, fetchGetSessionDetail } from '@/api/customer-service/chat'
import { ElMessage } from 'element-plus'

interface Message {
  id: string
  content: string
  type: 'user' | 'assistant'
  timestamp: Date
  cardType?: string
}

interface Props {
  selectedQuestion?: string
  selectedQuestionId?: string
  knowledgeSelection?: { selectedProducts: number[]; selectedActivities: number[] }
}

interface Emits {
  questionSent: []
  aiResponse: [response: any]
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 当前会话ID
const currentSessionId = ref<string>('')

// 消息列表
const messages = ref<Message[]>([
  {
    id: '1',
    content: '您好！我是智能客服小助手，请问有什么可以帮助您的吗？',
    type: 'assistant',
    timestamp: new Date()
  }
])

// 输入内容
const inputContent = ref('')
const loading = ref(false)
const messageListRef = ref<HTMLElement>()

// 当前时间（顶部状态栏显示）
const currentTime = computed(() => {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
})

// 初始化会话
const initSession = async () => {
  try {
    const res = await fetchCreateSession({
      userId: 'user_demo',
      userName: '演示用户',
      channel: 'web'
    })
    currentSessionId.value = res.sessionId
    // 加载欢迎消息（后端创建会话时会自动添加）
    const detail = await fetchGetSessionDetail(res.sessionId)
    if (detail?.messages?.length) {
      messages.value = detail.messages.map((m: any) => ({
        id: m.messageId,
        content: m.content,
        type: m.role === 'user' ? 'user' : 'assistant',
        timestamp: new Date(m.createdAt),
        cardType: m.cardType
      }))
    }
  } catch {
    // 后端不可用时使用默认欢迎消息
    currentSessionId.value = `local_${Date.now()}`
  }
}

// 页面加载时初始化会话
onMounted(() => {
  initSession()
})

// 监听选中问题变化
watch(() => props.selectedQuestion, (newVal) => {
  if (newVal) {
    inputContent.value = newVal
  }
})

// 发送消息
const sendMessage = async () => {
  const content = inputContent.value.trim()
  if (!content || loading.value) return

  // 添加用户消息到界面
  const userMessage: Message = {
    id: `user_${Date.now()}`,
    content,
    type: 'user',
    timestamp: new Date()
  }
  messages.value.push(userMessage)

  // 清空输入
  inputContent.value = ''
  emit('questionSent')

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 调用后端发送消息接口
  loading.value = true
  try {
    const res = await fetchSendMessage({
      sessionId: currentSessionId.value,
      content,
      extra: {
        selectedProducts: props.knowledgeSelection?.selectedProducts || [],
        selectedActivities: props.knowledgeSelection?.selectedActivities || []
      }
    })

    const aiMessage: Message = {
      id: res.messageId || `ai_${Date.now()}`,
      content: res.content,
      type: 'assistant',
      timestamp: new Date(res.createdAt || Date.now()),
      cardType: res.cardType
    }
    messages.value.push(aiMessage)

    emit('aiResponse', res)

    await nextTick()
    scrollToBottom()
  } catch (error: any) {
    ElMessage.error('发送消息失败，请检查后端服务是否启动')
    // 回退：移除用户消息
    messages.value.pop()
  } finally {
    loading.value = false
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 按下回车发送
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}
</script>

<template>
  <div class="chat-interface">
    <!-- iPhone状态栏 -->
    <div class="phone-status-bar">
      <span class="phone-status-bar__time">{{ currentTime }}</span>
      <div class="phone-status-bar__notch"></div>
      <div class="phone-status-bar__icons">
        <span>📶</span>
        <span>🔋</span>
      </div>
    </div>

    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="chat-header__avatar">🤖</div>
      <div class="chat-header__info">
        <div class="chat-header__name">智能客服</div>
        <div class="chat-header__status">在线</div>
      </div>
    </div>

    <!-- 消息列表 -->
    <div ref="messageListRef" class="message-list">
      <div
        v-for="message in messages"
        :key="message.id"
        class="message-item"
        :class="[`message-item--${message.type}`]"
      >
        <div class="message-item__avatar">
          {{ message.type === 'user' ? '👤' : '🤖' }}
        </div>
        <div class="message-item__content">
          <div class="message-bubble" :class="[`message-bubble--${message.type}`]">
            {{ message.content }}
          </div>
          <!-- 卡片类型标签 -->
          <div v-if="message.cardType" class="message-card-tag">
            <el-tag size="small" type="info">{{ message.cardType }}</el-tag>
          </div>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="message-item message-item--assistant">
        <div class="message-item__avatar">🤖</div>
        <div class="message-item__content">
          <div class="message-bubble message-bubble--assistant">
            <span class="loading-dots">
              <span></span>
              <span></span>
              <span></span>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        v-model="inputContent"
        type="textarea"
        :rows="2"
        placeholder="请输入您的问题..."
        resize="none"
        @keydown="handleKeydown"
      />
      <el-button
        type="primary"
        :loading="loading"
        :disabled="!inputContent.trim()"
        @click="sendMessage"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.chat-interface {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.phone-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background-color: #f0f1f4;
  font-size: 12px;

  &__time {
    font-weight: 600;
  }

  &__notch {
    width: 80px;
    height: 24px;
    background-color: #000;
    border-radius: 12px;
  }

  &__icons {
    display: flex;
    gap: 4px;
    font-size: 10px;
  }
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #eee;

  &__avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background-color: #e6f7ff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    margin-right: 12px;
  }

  &__name {
    font-size: 15px;
    font-weight: 600;
    color: #333;
  }

  &__status {
    font-size: 12px;
    color: #52c41a;
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: #ddd;
    border-radius: 2px;
  }
}

.message-item {
  display: flex;
  margin-bottom: 16px;

  &--user {
    flex-direction: row-reverse;

    .message-item__content {
      align-items: flex-end;
    }
  }

  &__avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background-color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    flex-shrink: 0;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  &__content {
    display: flex;
    flex-direction: column;
    margin: 0 10px;
    max-width: 70%;
  }
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;

  &--user {
    background-color: #1890ff;
    color: #fff;
    border-bottom-right-radius: 4px;
  }

  &--assistant {
    background-color: #fff;
    color: #333;
    border-bottom-left-radius: 4px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  }
}

.message-card-tag {
  margin-top: 4px;
}

.loading-dots {
  display: flex;
  gap: 4px;

  span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: #999;
    animation: bounce 1.4s infinite ease-in-out both;

    &:nth-child(1) {
      animation-delay: -0.32s;
    }

    &:nth-child(2) {
      animation-delay: -0.16s;
    }
  }
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.input-area {
  padding: 12px;
  background-color: #fff;
  border-top: 1px solid #eee;
  display: flex;
  gap: 8px;
  align-items: flex-end;

  :deep(.el-textarea__inner) {
    border-radius: 20px;
    padding: 10px 16px;
    font-size: 14px;

    &:focus {
      box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
    }
  }

  .el-button {
    border-radius: 20px;
    padding: 10px 20px;
    height: auto;
  }
}
</style>

