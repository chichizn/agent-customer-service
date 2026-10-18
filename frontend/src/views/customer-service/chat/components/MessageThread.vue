<script setup lang="ts">
  import { computed, nextTick, ref, watch } from 'vue'
  import type { SessionSnapshot } from '@/api/customer-service/model/workbenchModel'
  import { formatTime, modeLabels } from '../workbenchLabels'

  const props = defineProps<{
    snapshot?: SessionSnapshot
    customer?: boolean
    busy: boolean
    pending?: { content: string; status: 'sending' | 'failed' }
  }>()
  const draft = defineModel<string>({ default: '' })
  const emit = defineEmits<{ send: []; retry: []; discard: []; quick: [content: string] }>()
  const list = ref<HTMLElement>()
  const roleLabels = { customer: '顾客', ai: 'AI 客服', human: '人工客服', system: '系统事件' }
  const canCompose = computed(
    () => !!props.snapshot && (props.customer || props.snapshot.session.receptionMode === 'HUMAN')
  )
  const currentId = computed(() => props.snapshot?.session.sessionId)
  const signature = computed(() =>
    props.snapshot?.messages.map((message) => message.messageId).join(',')
  )
  watch([currentId, signature], async ([id], [previousId]) => {
    const element = list.value
    const nearBottom =
      !element || element.scrollHeight - element.scrollTop - element.clientHeight < 100
    await nextTick()
    if (list.value && (id !== previousId || nearBottom))
      list.value.scrollTop = list.value.scrollHeight
  })
  function submit() {
    if (canCompose.value && !props.busy && !props.pending && draft.value.trim()) emit('send')
  }
  function onKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault()
      submit()
    }
  }
</script>

<template>
  <div class="cs-thread">
    <template v-if="snapshot">
      <div class="cs-thread-context">
        <span
          class="cs-dot"
          :class="snapshot.session.receptionMode === 'AI' ? 'green' : 'amber'"
        ></span>
        <span>{{ modeLabels[snapshot.session.receptionMode] }}</span>
        <span class="cs-muted">{{
          customer ? '客服消息会自动同步到此处' : '仅在人工接管后发送客服回复'
        }}</span>
      </div>
      <div
        ref="list"
        class="cs-messages"
        role="log"
        aria-label="会话消息"
        aria-live="polite"
        aria-relevant="additions"
      >
        <div v-if="!snapshot.messages.length" class="cs-empty"
          >还没有消息，发送第一条咨询开始会话。</div
        >
        <article
          v-for="message in snapshot.messages"
          :key="message.messageId"
          class="cs-message"
          :class="[
            message.role,
            { own: customer ? message.role === 'customer' : message.role === 'human' }
          ]"
        >
          <template v-if="message.role === 'system'">
            <p class="cs-event">{{ message.content }}</p>
          </template>
          <template v-else>
            <div class="cs-avatar" :class="message.role">{{
              message.role === 'ai'
                ? 'AI'
                : message.role === 'human'
                  ? '人'
                  : snapshot.session.customerName.slice(0, 1)
            }}</div>
            <div class="cs-message-body">
              <div class="cs-message-meta"
                ><strong>{{ roleLabels[message.role] }}</strong
                ><time>{{ formatTime(message.createdAt) }}</time></div
              >
              <p class="cs-bubble">{{ message.content }}</p>
              <div v-if="customer && message.orderIds?.length" class="cs-inline-actions">
                <button
                  v-for="id in message.orderIds"
                  :key="id"
                  class="cs-chip"
                  :disabled="busy || !!pending"
                  @click="emit('quick', id)"
                  >{{ id }}</button
                >
              </div>
              <span v-if="message.ticketId" class="cs-reference"
                >本地工单 · {{ message.ticketId }}</span
              >
            </div>
          </template>
        </article>
      </div>
      <div
        v-if="pending"
        class="cs-pending"
        :class="{ failed: pending.status === 'failed' }"
        role="status"
      >
        <span>{{
          pending.status === 'sending'
            ? '正在发送并等待处理…'
            : '发送结果未确认，请重试核对；重试保留原消息标识。'
        }}</span>
        <p>{{ pending.content }}</p>
        <template v-if="pending.status === 'failed'">
          <button class="cs-link" :disabled="busy" @click="emit('retry')">重试本条</button>
          <button class="cs-link" :disabled="busy" @click="emit('discard')">取消重试</button>
        </template>
      </div>
      <div v-if="customer && snapshot.awaitingConfirmation" class="cs-confirmation">
        <span>有一项售后诉求等待确认</span>
        <button
          class="cs-btn primary small"
          :disabled="busy || !!pending"
          @click="emit('quick', '确认提交')"
          >确认提交</button
        >
        <button
          class="cs-btn small"
          :disabled="busy || !!pending"
          @click="emit('quick', '暂不提交')"
          >暂不提交</button
        >
      </div>
      <form class="cs-composer" @submit.prevent="submit">
        <label class="cs-sr-only" :for="customer ? 'customer-message' : 'human-message'">{{
          customer ? '顾客消息' : '人工回复'
        }}</label>
        <textarea
          :id="customer ? 'customer-message' : 'human-message'"
          v-model="draft"
          maxlength="2000"
          rows="3"
          :disabled="!canCompose || busy || !!pending"
          :placeholder="
            !canCompose
              ? '请先点击上方「接管会话」，再发送人工回复'
              : customer
                ? '输入想咨询的问题…'
                : '输入人工回复，发送后顾客端可见…'
          "
          @keydown="onKeydown"
        ></textarea>
        <div class="cs-composer-footer"
          ><span
            >Enter 发送 · Shift + Enter 换行
            <span class="cs-muted">{{ draft.length }}/2000</span></span
          ><button
            class="cs-btn primary"
            type="submit"
            :disabled="!canCompose || busy || !!pending || !draft.trim()"
            >{{ busy ? '处理中…' : customer ? '发送消息' : '发送人工回复' }}
            <span aria-hidden="true">↗</span></button
          ></div
        >
      </form>
    </template>
    <div v-else class="cs-empty cs-fill"
      ><div class="cs-empty-symbol">◌</div
      ><h3>{{ customer ? '选择身份，开启一段咨询' : '选择一条会话开始处理' }}</h3
      ><p>{{
        customer ? '恢复已有会话可继续之前的订单与售后咨询。' : '会话消息和业务资料会自动同步。'
      }}</p></div
    >
  </div>
</template>
