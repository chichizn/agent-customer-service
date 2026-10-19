<script setup lang="ts">
  import { computed, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { isWorkbenchMock } from '@/api/customer-service/workbench'
  import type { Platform, ReceptionMode } from '@/api/customer-service/model/workbenchModel'
  import MessageThread from './components/MessageThread.vue'
  import BusinessPanel from './components/BusinessPanel.vue'
  import DemoTimeNotice from './components/DemoTimeNotice.vue'
  import { useWorkbench } from './composables/useWorkbench'
  import { formatTime, modeLabels, platformLabels } from './workbenchLabels'
  import './workbench.css'

  defineOptions({ name: 'ChatCenter' })
  const router = useRouter()
  const {
    catalog,
    sessions,
    selectedId,
    snapshot,
    loading,
    syncing,
    syncError,
    actionError,
    busy,
    draft,
    pending,
    refresh,
    selectSession,
    sendMessage,
    retryMessage,
    discardFailedMessage,
    changeMode,
    updateTicket,
    resetDemo
  } = useWorkbench()
  const platform = ref<Platform | ''>('')
  const mode = ref<ReceptionMode | ''>('')
  const search = ref('')
  const resetVisible = ref(false)
  const waitingCount = computed(
    () => sessions.value.filter((session) => session.receptionMode === 'WAITING_HUMAN').length
  )
  const filteredSessions = computed(() =>
    sessions.value.filter(
      (session) =>
        (!platform.value || session.platform === platform.value) &&
        (!mode.value || session.receptionMode === mode.value) &&
        `${session.customerName} ${session.shopName} ${session.lastMessage}`
          .toLowerCase()
          .includes(search.value.toLowerCase())
    )
  )
  const simulatorHref = computed(
    () =>
      router.resolve({
        path: '/customer-simulator',
        query: snapshot.value
          ? { shopId: snapshot.value.session.shopId, customerId: snapshot.value.session.customerId }
          : {}
      }).href
  )
  async function confirmReset() {
    if (await resetDemo()) resetVisible.value = false
  }
</script>

<template>
  <main class="cs-app cs-workbench">
    <header class="cs-page-header">
      <div
        ><div class="cs-eyebrow">CUSTOMER SERVICE / WORKSPACE</div
        ><h1
          >客服工作台<span class="cs-badge" :class="isWorkbenchMock ? 'amber' : 'green'">{{
            isWorkbenchMock ? '前端本地模拟' : '后端接口模式'
          }}</span></h1
        ><p>连接每一次咨询，让服务有迹可循。</p></div
      >
      <div class="cs-header-actions"
        ><span class="cs-sync" aria-live="polite"
          ><span class="cs-dot" :class="syncError ? 'amber' : 'green'"></span
          >{{ syncError ? '同步中断' : syncing ? '同步中…' : '自动同步已开启' }}</span
        ><a class="cs-btn" :href="simulatorHref" target="_blank" rel="noopener">打开顾客入口 ↗</a
        ><button class="cs-btn subtle" :disabled="busy" @click="resetVisible = true"
          >重置演示</button
        ></div
      >
    </header>
    <div v-if="isWorkbenchMock" class="cs-demo-banner"
      ><span class="cs-badge amber">模拟环境</span
      >数据仅保存在当前浏览器；回复来自前端规则模拟，未调用模型、业务后端或平台。<a
        :href="router.resolve('/customer-workbench-preview').href"
        target="_blank"
        rel="noopener"
        >独立窗口 ↗</a
      ></div
    >
    <DemoTimeNotice :business-time="catalog.businessTime" />
    <div v-if="syncError || actionError" class="cs-error-banner" role="alert"
      ><span>{{ actionError || syncError }}</span
      ><button class="cs-link" :disabled="busy" @click="refresh">重新同步</button></div
    >
    <div class="cs-workspace-grid">
      <aside class="cs-session-panel cs-panel">
        <div class="cs-panel-heading"
          ><h2
            >全部会话 <span class="cs-count">{{ sessions.length }}</span></h2
          ><button class="cs-link" :disabled="busy" aria-label="刷新会话列表" @click="refresh"
            >↻</button
          ></div
        >
        <div class="cs-session-filters"
          ><label class="cs-search"
            ><span aria-hidden="true">⌕</span
            ><input
              v-model="search"
              aria-label="搜索会话"
              placeholder="搜索顾客、店铺或消息" /></label
          ><div class="cs-filter-row"
            ><select v-model="platform" aria-label="筛选平台"
              ><option value="">全部平台</option
              ><option value="TAOBAO">淘宝</option
              ><option value="DOUYIN">抖店</option></select
            ><select v-model="mode" aria-label="筛选接待模式"
              ><option value="">全部接待</option
              ><option value="AI">AI 接待</option
              ><option value="WAITING_HUMAN">等待人工</option
              ><option value="HUMAN">人工接待</option></select
            ></div
          ><button
            class="cs-waiting"
            :class="{ active: mode === 'WAITING_HUMAN' }"
            @click="mode = mode === 'WAITING_HUMAN' ? '' : 'WAITING_HUMAN'"
            ><span><span class="cs-dot amber"></span>等待人工处理</span
            ><strong>{{ waitingCount }}</strong></button
          ></div
        >
        <nav class="cs-session-list" aria-label="会话列表">
          <div v-if="loading" class="cs-empty">正在加载会话…</div
          ><div v-else-if="!filteredSessions.length" class="cs-empty"
            >暂无符合条件的会话<br /><a :href="simulatorHref" target="_blank" rel="noopener"
              >从顾客入口发起咨询 ↗</a
            ></div
          >
          <button
            v-for="session in filteredSessions"
            :key="session.sessionId"
            class="cs-session"
            :class="{ active: selectedId === session.sessionId }"
            :aria-current="selectedId === session.sessionId ? 'true' : undefined"
            @click="selectSession(session.sessionId)"
            ><div class="cs-avatar" :class="session.platform === 'TAOBAO' ? 'taobao' : 'douyin'">{{
              session.customerName.slice(0, 1)
            }}</div
            ><div class="cs-session-content"
              ><div class="cs-session-title"
                ><strong>{{ session.customerName }}</strong
                ><time>{{ formatTime(session.updatedAt).split(' ').at(-1) }}</time></div
              ><div class="cs-session-shop"
                ><span class="cs-platform" :class="session.platform.toLowerCase()">{{
                  platformLabels[session.platform]
                }}</span
                ><span>{{ session.shopName }}</span></div
              ><p>{{ session.lastMessage }}</p
              ><span class="cs-session-mode" :class="session.receptionMode.toLowerCase()"
                ><span
                  class="cs-dot"
                  :class="session.receptionMode === 'AI' ? 'green' : 'amber'"
                ></span
                >{{ modeLabels[session.receptionMode] }}</span
              ></div
            ></button
          > </nav
        ><div class="cs-sidebar-footer"
          ><span class="cs-dot green"></span>
          {{ isWorkbenchMock ? 'B 前端演示 · 独立本地数据' : '商家统一接待台' }}</div
        >
      </aside>
      <section class="cs-conversation cs-panel" aria-label="当前会话">
        <div class="cs-conversation-header"
          ><div
            ><h2
              >{{ snapshot?.session.customerName || '会话详情'
              }}<span
                v-if="snapshot"
                class="cs-platform"
                :class="snapshot.session.platform.toLowerCase()"
                >{{ platformLabels[snapshot.session.platform] }}</span
              ></h2
            ><p>{{ snapshot?.session.shopName || '选择左侧会话查看消息与业务资料' }}</p></div
          ><div v-if="snapshot" class="cs-inline-actions"
            ><button
              v-if="snapshot.session.receptionMode !== 'HUMAN'"
              class="cs-btn primary small"
              :disabled="busy"
              @click="changeMode('HUMAN')"
              >接管会话</button
            ><button v-else class="cs-btn small" :disabled="busy" @click="changeMode('AI')"
              >交回 AI</button
            ></div
          ></div
        >
        <div v-if="snapshot?.session.handoffSummary" class="cs-handoff"
          ><strong>人工交接摘要</strong><p>{{ snapshot.session.handoffSummary }}</p></div
        >
        <MessageThread
          v-model="draft"
          :snapshot="snapshot"
          :busy="busy"
          :pending="pending"
          @send="sendMessage(true)"
          @retry="retryMessage"
          @discard="discardFailedMessage"
        />
      </section>
      <BusinessPanel :snapshot="snapshot" :busy="busy" @update-ticket="updateTicket" />
    </div>
    <el-dialog
      v-model="resetVisible"
      title="恢复演示初始数据"
      width="460px"
      :close-on-click-modal="!busy"
      :close-on-press-escape="!busy"
      :show-close="!busy"
      ><p>{{
        isWorkbenchMock
          ? '仅重置此浏览器中 B 工作台的独立模拟数据，清除模拟会话、工单和执行记录，并恢复预置店铺、顾客与 12 笔订单。'
          : '将请求后端恢复已标记的演示商家数据，清除该演示空间的会话、工单与执行记录。'
      }}</p
      ><p class="cs-muted">其他打开的工作台和顾客页面将自动同步；历史演示会话可能需要重新选择。</p
      ><template #footer
        ><button class="cs-btn" :disabled="busy" @click="resetVisible = false">取消</button
        ><button class="cs-btn danger" :disabled="busy" @click="confirmReset">{{
          busy ? '正在恢复…' : '确认重置演示'
        }}</button></template
      ></el-dialog
    >
  </main>
</template>
