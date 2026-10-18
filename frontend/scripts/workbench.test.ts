import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createMockWorkbenchApi } from '../src/mock/customer-service/workbench'
import { MOCK_STORAGE_KEY } from '../src/mock/customer-service/fixtures'
import type { WorkbenchApi } from '../src/api/customer-service/model/workbenchModel'

Object.defineProperty(globalThis, 'window', { value: new EventTarget(), configurable: true })
// Node 的 LockManager 与浏览器实现不同；单元测试使用无 Web Locks 的兼容路径。
// 浏览器跨标签场景在本地 UI 中另外验证。
Object.defineProperty(globalThis, 'navigator', { value: {}, configurable: true })

class MemoryStorage implements Storage {
  private values = new Map<string, string>()
  get length() {
    return this.values.size
  }
  clear() {
    this.values.clear()
  }
  getItem(key: string) {
    return this.values.get(key) ?? null
  }
  key(index: number) {
    return [...this.values.keys()][index] ?? null
  }
  removeItem(key: string) {
    this.values.delete(key)
  }
  setItem(key: string, value: string) {
    this.values.set(key, value)
  }
}

const send = (
  api: WorkbenchApi,
  sessionId: string,
  content: string,
  clientMessageId = crypto.randomUUID()
) => api.sendCustomerMessage({ sessionId, content, clientMessageId })

test('按店铺与顾客隔离订单，拒绝引用其他店铺订单', async () => {
  const api = createMockWorkbenchApi(new MemoryStorage())
  const first = await api.fetchSession('demo-session-1')
  const other = await api.fetchSession('demo-session-3')
  assert.equal(first.orders.length, 3)
  assert.ok(
    first.orders.every(
      (order) =>
        order.shopId === first.session.shopId && order.customerId === first.session.customerId
    )
  )
  const result = await send(api, first.session.sessionId, `查询物流 ${other.orders[0].orderId}`)
  assert.match(result.messages.at(-1)!.content, /未找到属于当前店铺和顾客/)
  assert.equal(result.executions.at(-1)?.status, 'FAILED')
  assert.equal(result.selectedOrderId, undefined)
})

test('多笔杯子订单先澄清，选定后显示实际预置物流', async () => {
  const api = createMockWorkbenchApi(new MemoryStorage())
  const first = await send(api, 'demo-session-1', '我的杯子到哪了？')
  assert.equal(first.messages.at(-1)?.orderIds?.length, 2)
  assert.equal(first.selectedOrderId, undefined)
  const result = await send(api, 'demo-session-1', first.orders[0].orderId)
  assert.equal(result.selectedOrderId, first.orders[0].orderId)
  assert.match(result.messages.at(-1)!.content, /运输中/)
})

test('工单必须确认；重试和重复诉求去重，更新状态后查询同一工单', async () => {
  const storage = new MemoryStorage()
  const api = createMockWorkbenchApi(storage)
  const first = await api.fetchSession('demo-session-3')
  const orderId = first.orders[0].orderId
  const pending = await send(api, first.session.sessionId, `我要退货 ${orderId}，原因是不喜欢颜色`)
  assert.equal(pending.tickets.length, 0)
  assert.equal(pending.awaitingConfirmation, true)
  const requestId = crypto.randomUUID()
  const confirmed = await send(api, first.session.sessionId, '确认提交', requestId)
  assert.equal(confirmed.tickets.length, 1)
  const retry = await send(api, first.session.sessionId, '确认提交', requestId)
  assert.equal(retry.messages.length, confirmed.messages.length)
  await send(api, first.session.sessionId, `我要退货 ${orderId}，原因是不喜欢颜色`)
  const repeated = await send(api, first.session.sessionId, '确认提交')
  assert.equal(repeated.tickets.length, 1)
  const ticketId = confirmed.tickets[0].ticketId
  await api.updateTicket({
    sessionId: first.session.sessionId,
    ticketId,
    status: 'PROCESSING',
    processingNote: '已核实，待处理'
  })
  const restoredApi = createMockWorkbenchApi(storage)
  const progress = await send(restoredApi, first.session.sessionId, '刚才的工单进度怎么样了？')
  assert.equal(progress.messages.at(-1)?.ticketId, ticketId)
  assert.match(progress.messages.at(-1)!.content, /已核实，待处理/)
  await assert.rejects(
    api.updateTicket({
      sessionId: first.session.sessionId,
      ticketId,
      status: 'PENDING_REVIEW',
      processingNote: '非法回退'
    })
  )
})

test('接管进行中的回复后再交回 AI，也不会发出迟到回复或创建工单', async () => {
  const api = createMockWorkbenchApi(new MemoryStorage())
  const first = await api.fetchSession('demo-session-1')
  await send(api, first.session.sessionId, `我想退货 ${first.orders[0].orderId}，原因是不喜欢`)
  const inFlight = send(api, first.session.sessionId, '确认提交')
  await api.setReceptionMode(first.session.sessionId, 'HUMAN')
  await api.setReceptionMode(first.session.sessionId, 'AI')
  await inFlight
  const result = await api.fetchSession(first.session.sessionId)
  assert.equal(result.tickets.length, 0)
  assert.equal(result.messages.at(-1)?.role, 'system')
  assert.equal(result.awaitingConfirmation, false)
})

test('完成售后不残留旧诉求，新会话仍可查看并复用同一顾客的已有工单', async () => {
  const api = createMockWorkbenchApi(new MemoryStorage())
  const first = await api.fetchSession('demo-session-3')
  const orderId = first.orders[0].orderId
  await send(api, first.session.sessionId, `我想退货 ${orderId}，原因是不喜欢颜色`)
  const confirmed = await send(api, first.session.sessionId, '确认提交')
  const selected = await send(api, first.session.sessionId, first.orders[1].orderId)
  assert.equal(selected.awaitingConfirmation, false)
  const created = await api.openSession({
    shopId: first.session.shopId,
    customerId: first.session.customerId,
    forceNew: true
  })
  assert.equal(created.tickets[0].ticketId, confirmed.tickets[0].ticketId)
  await send(api, created.session.sessionId, `我想退货 ${orderId}，原因是不喜欢颜色`)
  const repeated = await send(api, created.session.sessionId, '确认提交')
  assert.equal(repeated.tickets.length, 1)
  assert.equal(repeated.messages.at(-1)?.ticketId, confirmed.tickets[0].ticketId)
})

test('切换到物流或取消申请后，旧售后确认失效', async () => {
  const api = createMockWorkbenchApi(new MemoryStorage())
  const first = await api.fetchSession('demo-session-3')
  const orderId = first.orders[0].orderId
  await send(api, first.session.sessionId, `我想退货 ${orderId}，原因是不喜欢颜色`)
  const logistics = await send(api, first.session.sessionId, '查一下物流')
  assert.equal(logistics.awaitingConfirmation, false)
  const selected = await send(api, first.session.sessionId, orderId)
  assert.equal(selected.awaitingConfirmation, false)
  await send(api, first.session.sessionId, `我想退货 ${orderId}，原因是不喜欢颜色`)
  await send(api, first.session.sessionId, '暂不提交')
  const afterCancel = await send(api, first.session.sessionId, orderId)
  assert.equal(afterCancel.awaitingConfirmation, false)
  assert.equal(afterCancel.tickets.length, 0)
})

test('人工模式只保存顾客消息，人工回复持久化且幂等', async () => {
  const storage = new MemoryStorage()
  const api = createMockWorkbenchApi(storage)
  await api.setReceptionMode('demo-session-1', 'HUMAN')
  const result = await send(api, 'demo-session-1', '我的杯子到哪了？')
  assert.equal(result.messages.at(-1)?.role, 'customer')
  const params = {
    sessionId: 'demo-session-1',
    content: '人工正在核实。',
    clientMessageId: crypto.randomUUID()
  }
  const reply = await api.sendHumanMessage(params)
  const retry = await api.sendHumanMessage(params)
  assert.equal(reply.messages.length, retry.messages.length)
  assert.equal(
    (await createMockWorkbenchApi(storage).fetchSession(params.sessionId)).messages.at(-1)?.role,
    'human'
  )
})

test('同一身份恢复会话，显式新建才创建；不同店铺知识范围独立', async () => {
  const api = createMockWorkbenchApi(new MemoryStorage())
  const before = (await api.fetchSessions()).length
  const params = { shopId: 'shop-dy', customerId: 'customer-dy-1', forceNew: false }
  const restored = await api.openSession(params)
  assert.equal(restored.session.sessionId, 'demo-session-3')
  assert.equal((await api.fetchSessions()).length, before)
  const created = await api.openSession({ ...params, forceNew: true })
  assert.notEqual(created.session.sessionId, restored.session.sessionId)
  const tb = await send(api, 'demo-session-1', '店铺退货规则是什么？')
  const dy = await send(api, 'demo-session-3', '店铺退货规则是什么？')
  assert.match(tb.knowledge[0].content, /7 天/)
  assert.match(dy.knowledge[0].content, /15 天/)
  assert.equal(tb.awaitingConfirmation, false)
})

test('重置只影响独立演示键，并阻止重置前的迟到处理', async () => {
  const storage = new MemoryStorage()
  storage.setItem('other-project-data', 'keep')
  const api = createMockWorkbenchApi(storage)
  await api.fetchSessions()
  const firstGeneration = JSON.parse(storage.getItem(MOCK_STORAGE_KEY)!).generation
  const pending = send(api, 'demo-session-1', '我的杯子到哪了？')
  const expected = assert.rejects(pending, /演示已重置/)
  await api.resetDemo()
  await expected
  assert.equal(storage.getItem('other-project-data'), 'keep')
  assert.notEqual(JSON.parse(storage.getItem(MOCK_STORAGE_KEY)!).generation, firstGeneration)
  assert.equal((await api.fetchSession('demo-session-1')).messages.length, 1)
})
