<script setup lang="ts">
  import { computed, ref, watch } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { isWorkbenchMock } from '@/api/customer-service/workbench'
  import MessageThread from '../chat/components/MessageThread.vue'
  import { useWorkbench } from '../chat/composables/useWorkbench'
  import { platformLabels } from '../chat/workbenchLabels'
  import '../chat/workbench.css'

  defineOptions({ name: 'CustomerSimulator' })
  const route = useRoute()
  const router = useRouter()
  const {
    catalog,
    sessions,
    selectedId,
    snapshot,
    syncError,
    actionError,
    busy,
    draft,
    pending,
    openSession,
    selectSession,
    sendMessage,
    sendQuickMessage,
    retryMessage,
    discardFailedMessage,
    refresh
  } = useWorkbench(false)
  const shopId = ref(typeof route.query.shopId === 'string' ? route.query.shopId : '')
  const customerId = ref(typeof route.query.customerId === 'string' ? route.query.customerId : '')
  const customers = computed(() =>
    catalog.value.customers.filter((customer) => customer.shopId === shopId.value)
  )
  const shop = computed(() => catalog.value.shops.find((item) => item.shopId === shopId.value))
  const matchingSessions = computed(() =>
    sessions.value.filter(
      (session) =>
        session.shopId === shopId.value &&
        session.customerId === customerId.value &&
        session.status === 'active'
    )
  )
  watch(
    () => catalog.value.shops,
    (shops) => {
      if (!shops.some((item) => item.shopId === shopId.value)) shopId.value = shops[0]?.shopId || ''
    }
  )
  watch(
    customers,
    (items) => {
      if (!items.length) return
      if (!items.some((item) => item.customerId === customerId.value))
        customerId.value = items[0]?.customerId || ''
    },
    { immediate: true }
  )
  watch([shopId, customerId], () => selectSession(''))
  async function enter(forceNew: boolean) {
    await openSession({ shopId: shopId.value, customerId: customerId.value, forceNew })
  }
  const questions = [
    '我的杯子到哪了？',
    '我想退货，原因是不喜欢这个颜色',
    '商品破损了，希望记录补偿诉求',
    '刚才的工单进度怎么样了？',
    '店铺的退货规则是什么？',
    '我要转人工'
  ]
</script>

<template>
  <main class="cs-app cs-simulator">
    <header class="cs-page-header"
      ><div
        ><div class="cs-eyebrow">CUSTOMER SIMULATOR</div
        ><h1
          >顾客模拟入口<span class="cs-badge amber">{{
            isWorkbenchMock ? '前端本地模拟' : '平台消息模拟'
          }}</span></h1
        ><p>以顾客视角，体验一次完整的咨询。</p></div
      ><a
        class="cs-btn"
        :href="router.resolve('/customer-workbench-preview').href"
        target="_blank"
        rel="noopener"
        >打开客服工作台 ↗</a
      ></header
    >
    <div class="cs-demo-banner"
      ><span class="cs-dot amber"></span
      >{{
        isWorkbenchMock
          ? '当前数据与规则回复均在浏览器中模拟，不调用真实模型和平台。请在同一浏览器打开工作台，体验双端同步。'
          : '平台消息为演示消息，业务结果由后端返回。'
      }}</div
    >
    <div v-if="syncError || actionError" class="cs-error-banner" role="alert"
      ><span>{{ actionError || syncError }}</span
      ><button class="cs-link" :disabled="busy" @click="refresh">重新同步</button></div
    >
    <div class="cs-simulator-grid">
      <aside class="cs-simulator-settings cs-panel">
        <div class="cs-panel-heading"
          ><h2>演示身份</h2><span class="cs-eyebrow">01 / IDENTITY</span></div
        >
        <div class="cs-settings-body"
          ><label
            >平台与店铺<select v-model="shopId" :disabled="busy"
              ><option v-for="item in catalog.shops" :key="item.shopId" :value="item.shopId"
                >{{ platformLabels[item.platform] }} · {{ item.name }}</option
              ></select
            ></label
          ><label
            >顾客身份<select v-model="customerId" :disabled="busy"
              ><option
                v-for="customer in customers"
                :key="customer.customerId"
                :value="customer.customerId"
                >{{ customer.name }}</option
              ></select
            ></label
          ><button class="cs-btn primary full" :disabled="busy || !customerId" @click="enter(false)"
            >{{ matchingSessions.length ? '恢复最近会话' : '开始咨询' }} ↗</button
          ><button class="cs-btn full" :disabled="busy || !customerId" @click="enter(true)"
            >显式新建会话</button
          ><label v-if="matchingSessions.length"
            >已有会话<select
              :value="selectedId"
              :disabled="busy"
              @change="selectSession(($event.target as HTMLSelectElement).value)"
              ><option value="">请选择会话</option
              ><option
                v-for="session in matchingSessions"
                :key="session.sessionId"
                :value="session.sessionId"
                >{{ session.sessionId.slice(0, 16) }} ·
                {{ session.lastMessage.slice(0, 14) }}</option
              ></select
            ></label
          ><div class="cs-divider"></div><div class="cs-eyebrow">02 / QUICK START</div
          ><h3>试试这些问题</h3
          ><button
            v-for="question in questions"
            :key="question"
            class="cs-question"
            :disabled="!snapshot || busy || !!pending"
            @click="draft = question"
            ><span>{{ question }}</span
            ><span aria-hidden="true">↗</span></button
          ><p class="cs-muted"
            >快捷问题只填入输入框，由你确认发送。切换身份会清空当前会话视图。</p
          ></div
        >
      </aside>
      <section class="cs-conversation cs-panel"
        ><div class="cs-conversation-header"
          ><div
            ><h2
              >{{ snapshot ? `${snapshot.session.customerName} 的咨询` : '开始一段咨询'
              }}<span v-if="shop" class="cs-platform" :class="shop.platform.toLowerCase()">{{
                platformLabels[shop.platform]
              }}</span></h2
            ><p>{{ shop?.name || '请选择左侧店铺与顾客' }}</p></div
          ><span class="cs-badge green">顾客视角</span></div
        ><MessageThread
          v-model="draft"
          customer
          :snapshot="snapshot"
          :busy="busy"
          :pending="pending"
          @send="sendMessage(false)"
          @quick="sendQuickMessage"
          @retry="retryMessage"
          @discard="discardFailedMessage"
      /></section>
    </div>
  </main>
</template>
