<script setup lang="ts">
  import { ref, watch } from 'vue'
  import type {
    ServiceTicket,
    SessionSnapshot,
    TicketStatus,
    UpdateTicketParams
  } from '@/api/customer-service/model/workbenchModel'
  import { formatTime, orderLabels, ticketLabels, ticketTransitions } from '../workbenchLabels'

  const props = defineProps<{ snapshot?: SessionSnapshot; busy: boolean }>()
  const emit = defineEmits<{ updateTicket: [params: UpdateTicketParams] }>()
  const tab = ref('orders')
  const editingTicket = ref<ServiceTicket>()
  const targetStatus = ref<TicketStatus>('PROCESSING')
  const note = ref('')
  const validation = ref('')
  const replyLabels = {
    MODEL_KNOWLEDGE: '大模型匹配知识回答',
    RULE_KNOWLEDGE: '规则知识兜底回答',
    BUSINESS_TOOL: '业务工具处理',
    CLARIFICATION: '继续补充问题'
  }
  watch(
    () => props.snapshot?.session.sessionId,
    () => {
      tab.value = 'orders'
      editingTicket.value = undefined
      note.value = ''
    }
  )
  watch(
    () => props.snapshot?.tickets,
    (tickets) => {
      if (
        editingTicket.value &&
        tickets?.find((ticket) => ticket.ticketId === editingTicket.value?.ticketId)?.status !==
          editingTicket.value.status
      )
        editingTicket.value = undefined
    }
  )
  function edit(ticket: ServiceTicket) {
    editingTicket.value = ticket
    targetStatus.value = ticketTransitions[ticket.status][0]
    note.value = ''
    validation.value = ''
  }
  function save() {
    if (!editingTicket.value || !props.snapshot) return
    if (!note.value.trim()) {
      validation.value = '请填写处理说明。'
      return
    }
    emit('updateTicket', {
      sessionId: props.snapshot.session.sessionId,
      ticketId: editingTicket.value.ticketId,
      expectedStatus: editingTicket.value.status,
      status: targetStatus.value,
      processingNote: note.value.trim()
    })
  }
</script>

<template>
  <aside class="cs-business cs-panel">
    <div class="cs-panel-heading"><h2>业务资料</h2><span class="cs-eyebrow">CONTEXT</span></div>
    <div class="cs-tabs" role="tablist" aria-label="业务资料分类">
      <button
        v-for="item in [
          { id: 'orders', label: '订单' },
          { id: 'tickets', label: '工单' },
          { id: 'knowledge', label: '知识' },
          { id: 'executions', label: '执行记录' }
        ]"
        :id="`business-tab-${item.id}`"
        :key="item.id"
        role="tab"
        :aria-selected="tab === item.id"
        :aria-controls="`business-panel-${item.id}`"
        :class="{ active: tab === item.id }"
        @click="tab = item.id"
        >{{ item.label
        }}<span v-if="item.id === 'tickets' && snapshot?.tickets.length" class="cs-count">{{
          snapshot.tickets.length
        }}</span></button
      >
    </div>
    <div
      :id="`business-panel-${tab}`"
      role="tabpanel"
      :aria-labelledby="`business-tab-${tab}`"
      class="cs-business-content"
    >
      <div v-if="!snapshot" class="cs-empty">选择会话后查看对应顾客资料</div>
      <template v-else>
        <div class="cs-scope"
          ><span class="cs-dot green"></span>当前店铺 ·
          {{ snapshot.session.customerName }} 的资料</div
        >
        <template v-if="tab === 'orders'">
          <div v-if="!snapshot.orders.length" class="cs-empty">当前顾客暂无订单</div>
          <article
            v-for="order in snapshot.orders"
            :key="order.orderId"
            class="cs-card"
            :class="{ selected: snapshot.selectedOrderId === order.orderId }"
          >
            <div class="cs-card-top"
              ><span class="cs-mono">{{ order.orderNumber || order.orderId }}</span
              ><span class="cs-badge" :class="order.status === 'DELIVERED' ? 'green' : 'neutral'">{{
                orderLabels[order.status]
              }}</span></div
            >
            <div v-for="item in order.items" :key="item.name" class="cs-product"
              ><div class="cs-product-icon" aria-hidden="true">{{
                item.name.includes('杯') ? '◡' : '▤'
              }}</div
              ><div
                ><strong>{{ item.name }}</strong
                ><p>数量 {{ item.quantity }}</p></div
              ></div
            >
            <div class="cs-card-bottom"
              ><span>{{ formatTime(order.createdAt) }}</span
              ><strong class="cs-price">¥{{ order.paidAmount.toFixed(2) }}</strong></div
            >
            <span v-if="snapshot.selectedOrderId === order.orderId" class="cs-reference"
              >顾客已确认本单</span
            >
            <details class="cs-logistics"
              ><summary>物流详情 <span>↓</span></summary
              ><template
                v-for="logistics in snapshot.logistics.filter(
                  (item) => item.orderId === order.orderId
                )"
                :key="logistics.orderId"
                ><p>{{ logistics.status }}</p
                ><p v-if="logistics.carrier" class="cs-muted"
                  >{{ logistics.carrier }} · {{ logistics.trackingNumber }}</p
                ><ol class="cs-timeline"
                  ><li v-for="node in logistics.nodes" :key="node.time"
                    ><span>{{ node.description }}</span
                    ><time>{{ formatTime(node.time) }}</time></li
                  ></ol
                ></template
              ><p v-if="!snapshot.logistics.some((item) => item.orderId === order.orderId)"
                >暂无物流记录</p
              ></details
            >
          </article>
        </template>
        <template v-if="tab === 'tickets'">
          <p class="cs-notice">本地工单仅记录诉求，处理完成不代表平台退款成功。</p>
          <div v-if="!snapshot.tickets.length" class="cs-empty"
            >暂无本地服务工单<br />顾客确认诉求后会显示在这里</div
          >
          <article v-for="ticket in snapshot.tickets" :key="ticket.ticketId" class="cs-card">
            <div class="cs-card-top"
              ><strong>{{ ticket.type === 'RETURN' ? '退货诉求' : '异常 / 补偿诉求' }}</strong
              ><span class="cs-badge neutral">{{ ticketLabels[ticket.status] }}</span></div
            >
            <p class="cs-mono">{{ ticket.ticketNumber || ticket.ticketId }}</p
            ><p class="cs-muted">关联订单 {{ ticket.orderNumber || ticket.orderId }}</p
            ><p>{{ ticket.reason }}</p
            ><p v-if="ticket.processingNote" class="cs-note"
              >处理说明：{{ ticket.processingNote }}</p
            ><details v-if="ticket.history?.length" class="cs-logistics"
              ><summary>处理历史 <span>↓</span></summary
              ><ol class="cs-timeline"
                ><li v-for="record in ticket.history" :key="record.historyId"
                  ><span
                    ><strong>{{
                      record.fromStatus
                        ? `${ticketLabels[record.fromStatus]} → ${ticketLabels[record.toStatus]}`
                        : `创建工单 · ${ticketLabels[record.toStatus]}`
                    }}</strong
                    ><br />{{ record.note }}</span
                  ><time>{{ formatTime(record.occurredAt) }}</time></li
                ></ol
              ></details
            ><small class="cs-muted">更新于 {{ formatTime(ticket.updatedAt) }}</small>
            <button
              v-if="ticketTransitions[ticket.status].length"
              class="cs-btn small full"
              :disabled="busy"
              @click="edit(ticket)"
              >更新处理状态</button
            >
            <form
              v-if="editingTicket?.ticketId === ticket.ticketId"
              class="cs-ticket-form"
              @submit.prevent="save"
              ><label
                >目标状态<select v-model="targetStatus"
                  ><option
                    v-for="status in ticketTransitions[ticket.status]"
                    :key="status"
                    :value="status"
                    >{{ ticketLabels[status] }}</option
                  ></select
                ></label
              ><label
                >处理说明<textarea
                  v-model="note"
                  rows="3"
                  maxlength="500"
                  placeholder="填写本次处理结果或原因"
                ></textarea></label
              ><p v-if="validation" class="cs-error-text" role="alert">{{ validation }}</p
              ><div class="cs-inline-actions"
                ><button class="cs-btn primary small" type="submit" :disabled="busy"
                  >保存状态</button
                ><button
                  class="cs-btn small"
                  type="button"
                  :disabled="busy"
                  @click="editingTicket = undefined"
                  >取消</button
                ></div
              ></form
            >
          </article>
        </template>
        <template v-if="tab === 'knowledge'">
          <article v-if="snapshot.analysis" class="cs-card" aria-label="本轮客服分析">
            <div class="cs-card-top">
              <strong>本轮客服分析</strong>
              <span
                class="cs-badge"
                :class="snapshot.analysis.analysisSource === 'MODEL' ? 'green' : 'amber'"
                >{{
                  snapshot.analysis.analysisSource === 'MODEL' ? '大模型分析' : '规则分析兜底'
                }}</span
              >
            </div>
            <p
              >{{ snapshot.analysis.scene }} · {{ snapshot.analysis.intentName }} ·
              {{ snapshot.analysis.emotion }}</p
            >
            <div class="cs-card-bottom">{{ replyLabels[snapshot.analysis.replySource] }}</div>
          </article>
          <div v-if="!snapshot.knowledge.length" class="cs-empty"
            >本轮尚无知识引用<br />引用将以实际返回的数据展示</div
          >
          <article
            v-for="knowledge in snapshot.knowledge"
            :key="knowledge.knowledgeId"
            class="cs-card"
            ><span class="cs-badge green">{{ knowledge.scope }}</span
            ><h3>{{ knowledge.title }}</h3
            ><p>{{ knowledge.content }}</p
            ><div class="cs-card-bottom">来源：{{ knowledge.source }}</div></article
          >
        </template>
        <template v-if="tab === 'executions'">
          <div v-if="!snapshot.executions.length" class="cs-empty"
            >暂无执行记录<br />完成业务动作后显示结果</div
          >
          <ol class="cs-executions"
            ><li v-for="record in [...snapshot.executions].reverse()" :key="record.executionId"
              ><div class="cs-card-top"
                ><strong>{{ record.action }}</strong
                ><span class="cs-badge" :class="record.status === 'SUCCESS' ? 'green' : 'red'">{{
                  record.status === 'SUCCESS' ? '成功' : '失败'
                }}</span></div
              ><p>{{ record.summary }}</p
              ><p v-if="record.relatedId" class="cs-mono">{{ record.relatedId }}</p
              ><time>{{ formatTime(record.createdAt) }}</time></li
            ></ol
          >
        </template>
      </template>
    </div>
  </aside>
</template>
