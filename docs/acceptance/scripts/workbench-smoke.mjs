import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { root, writeReport } from '../../../demo-data/scripts/check-integration.mjs'

const args = process.argv.slice(2)
const options = { baseUrl: 'http://127.0.0.1:10002' }
const positional = []
for (let index = 0; index < args.length; index++) {
  const arg = args[index]
  if (arg === '--allow-write') options.allowWrite = true
  else if (['--base-url', '--database'].includes(arg)) {
    const value = args[++index]
    if (!value || value.startsWith('--')) throw new Error(`${arg} requires a value`)
    options[arg === '--base-url' ? 'baseUrl' : 'database'] = value
  } else if (arg.startsWith('--')) throw new Error(`Unknown option: ${arg}`)
  else positional.push(arg)
}
if (!options.allowWrite) {
  throw new Error('This smoke test creates persistent demo sessions and messages. Pass --allow-write only for an isolated acceptance database.')
}
if (options.database !== 'agent_customer_service_acceptance_20261019') {
  throw new Error('This evidence script is pinned to the isolated agent_customer_service_acceptance_20261019 database.')
}
const baseUrl = new URL(options.baseUrl)
if (baseUrl.protocol !== 'http:' || !['127.0.0.1', 'localhost', '[::1]'].includes(baseUrl.hostname) || baseUrl.username || baseUrl.password) {
  throw new Error('Only a loopback HTTP backend is allowed')
}
if (positional.length > 1) throw new Error('Provide at most one report path')
const reportPath = positional[0]
const checks = []
const sessions = {}
const generatedIds = new Set()

async function post(path, body, expectedStatus = 200) {
  const response = await fetch(new URL(path, baseUrl), {
    method: 'POST', redirect: 'error', signal: AbortSignal.timeout(10000),
    headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
  })
  const text = await response.text()
  let payload
  try { payload = text ? JSON.parse(text) : null } catch { throw new Error(`${path}: HTTP ${response.status} returned non-JSON`) }
  assert.equal(response.status, expectedStatus, `${path}: ${JSON.stringify(payload)}`)
  if (expectedStatus < 300) assert([0, 200].includes(payload?.code), `${path}: business code ${payload?.code}`)
  return payload?.data
}

function clientId(prefix) {
  const id = `${prefix}-${crypto.randomUUID()}`
  generatedIds.add(id)
  return id
}

async function open(key, shopId, customerId) {
  const snapshot = await post('/api/chat/session/open', { shopId, customerId, forceNew: true })
  assert.ok(snapshot?.session?.sessionId)
  sessions[key] = snapshot.session.sessionId
  return snapshot
}

async function send(sessionId, content, id = clientId('smoke')) {
  return post('/api/chat/message/send', { sessionId, clientMessageId: id, content })
}

async function check(name, work) {
  try {
    const evidence = await work()
    checks.push({ name, status: 'PASS', evidence })
  } catch (error) {
    checks.push({ name, status: 'FAIL', reason: error.message })
  }
}

await check('TB3 valid identity has no orders or logistics', async () => {
  const s = await open('TB3', 'demo-shop-tb', 'demo-customer-tb-3')
  assert.equal(s.orders.length, 0)
  const result = await send(s.session.sessionId, '查一下我的订单物流')
  assert.equal(result.orders.length, 0)
  assert.equal(result.logistics.length, 0)
  assert.ok(result.messages.at(-1).content.includes('未找到匹配订单'))
  assert.ok(result.executions.some(e => e.action === 'get_order' && e.status === 'SUCCESS'))
  return { sessionId: s.session.sessionId, orderCount: result.orders.length, logisticsCount: result.logistics.length }
})

await check('same customer message id is idempotent and immutable', async () => {
  const sessionId = sessions.TB3
  assert.ok(sessionId, 'TB3 prerequisite session was not created')
  const clientMessageId = clientId('idempotency')
  const first = await send(sessionId, '查一下我的订单物流', clientMessageId)
  const count = first.messages.length
  const duplicate = await send(sessionId, '查一下我的订单物流', clientMessageId)
  assert.equal(duplicate.messages.length, count)
  assert.equal(duplicate.messages.filter(m => m.clientMessageId === clientMessageId).length, 1)
  await post('/api/chat/message/send', { sessionId, clientMessageId, content: '换一个问题' }, 409)
  const snapshot = await post('/api/chat/session/snapshot', { sessionId })
  assert.equal(snapshot.messages.length, count)
  return { sessionId, messageCount: count, changedContentStatus: 409 }
})

await check('multi-order clarification, owned logistics and scope isolation', async () => {
  const s = await open('TB1', 'demo-shop-tb', 'demo-customer-tb-1')
  const ambiguous = await send(s.session.sessionId, '我的杯子到哪了')
  assert.equal(ambiguous.selectedOrderId, null)
  assert.ok(ambiguous.messages.at(-1).content.includes('多笔'))
  assert.deepEqual(ambiguous.messages.at(-1).orderIds.sort(), ['demo-order-tb-01', 'demo-order-tb-02'])
  const selected = await send(s.session.sessionId, 'TB202610100001')
  assert.equal(selected.selectedOrderId, 'demo-order-tb-01')
  assert.ok(selected.messages.at(-1).content.includes('DEMO-TB-0001'))
  assert.ok(selected.messages.at(-1).content.includes('杭州'))
  const otherShop = await send(s.session.sessionId, '查询 DY202610100001 物流')
  assert.ok(otherShop.messages.at(-1).content.includes('未找到匹配订单'))
  assert.ok(!JSON.stringify(otherShop).includes('DEMO-DY-0001'))
  const otherCustomer = await send(s.session.sessionId, '查询 TB202610120005 物流')
  assert.ok(otherCustomer.messages.at(-1).content.includes('未找到匹配订单'))
  assert.ok(!JSON.stringify(otherCustomer).includes('demo-order-tb-05'))
  return { sessionId: s.session.sessionId, clarifiedOrders: ambiguous.messages.at(-1).orderIds,
    selectedOrderId: selected.selectedOrderId, logistics: 'DEMO-TB-0001 / 杭州', crossShop: 'not found', crossCustomer: 'not found' }
})

await check('return policy knowledge stays within each shop', async () => {
  const tb = await open('policy-tb', 'demo-shop-tb', 'demo-customer-tb-3')
  const tbAnswer = await send(tb.session.sessionId, '退货期限几天')
  assert.ok(tbAnswer.messages.at(-1).content.includes('15天'))
  assert.ok(!tbAnswer.messages.at(-1).content.includes('7天'))
  const dy = await open('policy-dy', 'demo-shop-dy', 'demo-customer-dy-2')
  const dyAnswer = await send(dy.session.sessionId, '退货期限几天')
  assert.ok(dyAnswer.messages.at(-1).content.includes('7天'))
  assert.ok(!dyAnswer.messages.at(-1).content.includes('15天'))
  return { taobao: '15天', douyin: '7天' }
})

await check('model-disabled return request hands off without creating a ticket', async () => {
  const s = await open('handoff', 'demo-shop-dy', 'demo-customer-dy-1')
  const result = await send(s.session.sessionId, '我想退货 DY202610100001，原因是不喜欢颜色')
  assert.equal(result.session.receptionMode, 'WAITING_HUMAN')
  assert.equal(result.awaitingConfirmation, false)
  assert.equal(result.tickets.length, 0)
  assert.ok(result.session.handoffSummary.includes('我想退货 DY202610100001，原因是不喜欢颜色'))
  assert.ok(result.session.handoffSummary.includes('诉求：RETURN'))
  assert.ok(result.executions.some(e => e.action === 'handoff_to_human' && e.status === 'SUCCESS'))
  return { sessionId: s.session.sessionId, receptionMode: result.session.receptionMode,
    handoffSummary: result.session.handoffSummary, ticketCount: result.tickets.length }
})

await check('human takeover, manual message idempotency and explicit AI return', async () => {
  const sessionId = sessions.handoff
  assert.ok(sessionId, 'handoff prerequisite session was not created')
  const waiting = await post('/api/chat/session/snapshot', { sessionId })
  const taken = await post('/api/chat/session/reception/update', { sessionId, receptionMode: 'HUMAN' })
  assert.equal(taken.session.handoffSummary, waiting.session.handoffSummary)
  assert.equal(taken.awaitingConfirmation, false)
  const before = taken.messages.length
  const customer = await send(sessionId, '查询退货规则')
  assert.equal(customer.session.receptionMode, 'HUMAN')
  assert.equal(customer.messages.length, before + 1)
  assert.equal(customer.messages.at(-1).role, 'customer')
  const humanId = clientId('human')
  const humanBody = { sessionId, clientMessageId: humanId, content: '商家正在核实' }
  const human = await post('/api/chat/message/human/send', humanBody)
  const humanCount = human.messages.filter(m => m.clientMessageId === humanId).length
  assert.equal(humanCount, 1)
  const duplicate = await post('/api/chat/message/human/send', humanBody)
  assert.equal(duplicate.messages.filter(m => m.clientMessageId === humanId).length, 1)
  await post('/api/chat/message/human/send', { ...humanBody, content: '变更后的人工消息' }, 409)
  const returned = await post('/api/chat/session/reception/update', { sessionId, receptionMode: 'AI' })
  assert.ok(returned.messages.at(-1).content.includes('下一条顾客消息'))
  const count = returned.messages.length
  assert.equal((await post('/api/chat/session/snapshot', { sessionId })).messages.length, count)
  const resumed = await send(sessionId, '退货期限几天')
  assert.equal(resumed.session.receptionMode, 'AI')
  assert.ok(resumed.messages.at(-1).content.includes('7天'))
  const restored = await post('/api/chat/session/open', { shopId: 'demo-shop-dy', customerId: 'demo-customer-dy-1', forceNew: false })
  assert.equal(restored.session.sessionId, sessionId)
  assert.equal(restored.messages.length, resumed.messages.length)
  return { sessionId, preservedHandoffSummary: taken.session.handoffSummary,
    customerMessageRole: customer.messages.at(-1).role,
    humanMessageCount: 1, aiReturn: 'next customer message only', restored: true }
})

const revision = spawnSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8', windowsHide: true, timeout: 3000 }).stdout?.trim()
const workspaceDirty = Boolean(spawnSync('git', ['status', '--porcelain'], { cwd: root, encoding: 'utf8', windowsHide: true, timeout: 3000 }).stdout?.trim())
const report = {
  generatedAt: new Date().toISOString(), revision, workspaceDirty, scope: 'ISOLATED_MYSQL_WORKBENCH_HTTP_SMOKE_WRITES_SESSIONS_AND_MESSAGES',
  baseUrl: baseUrl.origin, backendDatabase: options.database,
  model: 'disabled', resetCalled: false, createdSessionIds: sessions, generatedMessageIds: [...generatedIds],
  status: checks.some(check => check.status === 'FAIL') ? 'FAIL' : 'PASS', checks,
}
writeReport(report, reportPath)
if (report.status !== 'PASS') process.exitCode = 1
