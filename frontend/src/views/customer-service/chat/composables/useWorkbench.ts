import { computed, onActivated, onDeactivated, onMounted, onUnmounted, ref, watch } from 'vue'
import { workbenchApi } from '@/api/customer-service/workbench'
import { MOCK_CHANGE_EVENT, MOCK_STORAGE_KEY } from '@/mock/customer-service/fixtures'
import type {
  DemoCatalog,
  OpenSessionParams,
  SendWorkbenchMessageParams,
  SessionSnapshot,
  UpdateTicketParams,
  WorkbenchSession
} from '@/api/customer-service/model/workbenchModel'

interface PendingMessage extends SendWorkbenchMessageParams {
  human: boolean
  status: 'sending' | 'failed'
}

export function useWorkbench(autoSelect = true) {
  const catalog = ref<DemoCatalog>({ shops: [], customers: [] })
  const sessions = ref<WorkbenchSession[]>([])
  const selectedId = ref('')
  const snapshot = ref<SessionSnapshot>()
  const loading = ref(true)
  const syncing = ref(false)
  const syncError = ref('')
  const actionError = ref('')
  const busy = ref(false)
  const pendingMessages = ref<Record<string, PendingMessage>>({})
  const drafts = ref<Record<string, string>>({})
  const draft = computed({
    get: () => drafts.value[selectedId.value] || '',
    set: (value: string) => {
      drafts.value[selectedId.value] = value
    }
  })
  const pending = computed(() => pendingMessages.value[selectedId.value])
  let controller: AbortController | undefined
  let revision = 0
  let active = false
  let timer: ReturnType<typeof setTimeout> | undefined

  function selectSession(id: string) {
    if (id === selectedId.value) return
    revision++
    controller?.abort()
    snapshot.value = undefined
    selectedId.value = id
    actionError.value = ''
    if (autoSelect) {
      try {
        sessionStorage.setItem('customer-service:selected-session', id)
      } catch {
        /* 存储不可用不影响操作 */
      }
    }
  }

  async function refresh() {
    if (!active || busy.value) return
    const currentRevision = ++revision
    controller?.abort()
    controller = new AbortController()
    const signal = controller.signal
    const sessionId = selectedId.value
    syncing.value = true
    try {
      const [nextCatalog, nextSessions, nextSnapshot] = await Promise.all([
        workbenchApi.fetchCatalog(signal),
        workbenchApi.fetchSessions(signal),
        sessionId ? workbenchApi.fetchSession(sessionId, signal) : Promise.resolve(undefined)
      ])
      if (!active || currentRevision !== revision) return
      catalog.value = nextCatalog
      sessions.value = nextSessions
      snapshot.value = nextSnapshot
      syncError.value = ''
      if (autoSelect && !sessionId && nextSessions.length) selectSession(nextSessions[0].sessionId)
    } catch (error) {
      if (!active || currentRevision !== revision || signal.aborted) return
      syncError.value = error instanceof Error ? error.message : '同步失败，请检查连接后重试。'
      // 重置后的旧会话无法获取，重新读取列表；禁止保留上一会话业务资料。
      try {
        const nextSessions = await workbenchApi.fetchSessions(signal)
        if (currentRevision !== revision) return
        sessions.value = nextSessions
        if (sessionId && !nextSessions.some((item) => item.sessionId === sessionId)) {
          selectSession('')
          snapshot.value = undefined
          syncError.value = '会话已移除或演示已重置，请重新选择。'
        }
      } catch {
        /* 保留首个错误；由下一轮重试恢复 */
      }
    } finally {
      if (currentRevision === revision) {
        syncing.value = false
        loading.value = false
      }
    }
  }

  async function runAction(operation: () => Promise<SessionSnapshot | void>) {
    if (busy.value) return false
    busy.value = true
    actionError.value = ''
    revision++
    controller?.abort()
    try {
      const result = await operation()
      if (result && result.session.sessionId === selectedId.value) snapshot.value = result
      return true
    } catch (error) {
      actionError.value = error instanceof Error ? error.message : '操作失败，请重试。'
      return false
    } finally {
      busy.value = false
      void refresh()
    }
  }

  async function openSession(params: OpenSessionParams) {
    return runAction(async () => {
      const result = await workbenchApi.openSession(params)
      selectSession(result.session.sessionId)
      return result
    })
  }

  async function deliver(message: PendingMessage) {
    pendingMessages.value[message.sessionId] = { ...message, status: 'sending' }
    const success = await runAction(() =>
      message.human
        ? workbenchApi.sendHumanMessage(message)
        : workbenchApi.sendCustomerMessage(message)
    )
    if (success) {
      delete pendingMessages.value[message.sessionId]
      if (drafts.value[message.sessionId]?.trim() === message.content)
        drafts.value[message.sessionId] = ''
    } else pendingMessages.value[message.sessionId] = { ...message, status: 'failed' }
    return success
  }

  function sendMessage(human: boolean) {
    if (busy.value || pending.value || !draft.value.trim() || !selectedId.value) return
    return deliver({
      sessionId: selectedId.value,
      content: draft.value.trim(),
      clientMessageId: crypto.randomUUID(),
      human,
      status: 'sending'
    })
  }

  function sendQuickMessage(content: string) {
    if (busy.value || pending.value || !selectedId.value) return
    return deliver({
      sessionId: selectedId.value,
      content,
      clientMessageId: crypto.randomUUID(),
      human: false,
      status: 'sending'
    })
  }

  function retryMessage() {
    if (busy.value || !pending.value || pending.value.status !== 'failed') return
    return deliver(pending.value)
  }

  function discardFailedMessage() {
    if (pending.value?.status === 'failed') delete pendingMessages.value[selectedId.value]
  }

  const changeMode = (mode: 'HUMAN' | 'AI') => {
    const id = selectedId.value
    return runAction(() => workbenchApi.setReceptionMode(id, mode))
  }
  const updateTicket = (params: UpdateTicketParams) =>
    runAction(() => workbenchApi.updateTicket(params))
  const resetDemo = () =>
    runAction(async () => {
      await workbenchApi.resetDemo()
      selectSession('')
      pendingMessages.value = {}
      drafts.value = {}
    })

  function onStorage(event: StorageEvent) {
    if (event.key === MOCK_STORAGE_KEY) void refresh()
  }
  function onLocalChange() {
    void refresh()
  }
  async function tick() {
    await refresh()
    if (active) timer = setTimeout(tick, 2500)
  }
  function start() {
    if (active) return
    active = true
    window.addEventListener('storage', onStorage)
    window.addEventListener(MOCK_CHANGE_EVENT, onLocalChange)
    void tick()
  }
  function stop() {
    active = false
    revision++
    controller?.abort()
    clearTimeout(timer)
    window.removeEventListener('storage', onStorage)
    window.removeEventListener(MOCK_CHANGE_EVENT, onLocalChange)
  }
  if (autoSelect) {
    try {
      selectedId.value = sessionStorage.getItem('customer-service:selected-session') || ''
    } catch {
      /* 使用列表默认选择 */
    }
  }
  watch(selectedId, () => {
    if (active && !busy.value) void refresh()
  })
  onMounted(start)
  onActivated(start)
  onDeactivated(stop)
  onUnmounted(stop)

  return {
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
    openSession,
    sendMessage,
    sendQuickMessage,
    retryMessage,
    discardFailedMessage,
    changeMode,
    updateTicket,
    resetDemo
  }
}
