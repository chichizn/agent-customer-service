import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { root } from '../../../demo-data/scripts/check-integration.mjs'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'

// Run only after independently verifying the backend's actual datasource and model configuration.
const args = process.argv.slice(2)
const options = { baseUrl: 'http://127.0.0.1:10002' }
const positional = []
for (let i = 0; i < args.length; i++) {
  if (args[i] === '--allow-write') options.allowWrite = true
  else if (['--base-url', '--database'].includes(args[i])) {
    const flag = args[i], value = args[++i]
    assert(value && !value.startsWith('--'), `${flag} requires a value`)
    options[flag === '--base-url' ? 'baseUrl' : 'database'] = value
  } else if (args[i].startsWith('--')) throw new Error(`Unknown option: ${args[i]}`)
  else positional.push(args[i])
}
assert(options.allowWrite, 'Requires --allow-write: creates demo sessions, tickets and handling history')
assert.equal(options.database, 'agent_customer_service_acceptance_20261019', 'Requires the named isolated acceptance database')
assert(positional.length <= 1, 'Provide at most one report path')
const base = new URL(options.baseUrl)
assert(base.protocol === 'http:' && ['127.0.0.1', 'localhost', '[::1]'].includes(base.hostname) && !base.username && !base.password,
  'Only loopback HTTP backends are allowed')
const checks = [], requests = [], sessions = {}
const scope = (shop = 'dy', customer = 1) => ({ merchantId: 'demo-merchant', shopId: `demo-shop-${shop}`, customerId: `demo-customer-${shop}-${customer}` })
const last = s => s.messages.at(-1).content
// MySQL DATETIME(6) rounds JVM nanoseconds; compare business data at millisecond precision.
const stable = s => JSON.parse(JSON.stringify(s).replace(/(\d{2}:\d{2}:\d{2}\.\d{3})\d+/g, '$1'))
async function post(path, body, status = 200) {
  const start = Date.now()
  const response = await fetch(new URL(path, base), { method: 'POST', redirect: 'error', signal: AbortSignal.timeout(35000),
    headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
  const data = await response.json()
  requests.push({ path, body, status: response.status, elapsedMs: Date.now() - start, response: data })
  assert.equal(response.status, status, `${path}: ${JSON.stringify(data)}`)
  if (status < 300) assert([0, 200].includes(data.code), `${path}: ${JSON.stringify(data)}`)
  return data.data
}
async function open(key, shop = 'tb', customer = 1) {
  const s = await post('/api/chat/session/open', { ...scope(shop, customer), forceNew: true })
  sessions[key] = s.session.sessionId
  return s.session.sessionId
}
const send = (sessionId, content, clientMessageId = crypto.randomUUID()) => post('/api/chat/message/send', { sessionId, content, clientMessageId })
const snapshot = sessionId => post('/api/chat/session/snapshot', { sessionId })
const tickets = (shop = 'dy', customer = 1) => post('/api/commerce/tickets/query', { scope: scope(shop, customer) })
async function check(name, action) {
  try { checks.push({ name, status: 'PASS', evidence: await action() }) }
  catch (e) { checks.push({ name, status: 'FAIL', reason: e.message }) }
  console.log(`${checks.at(-1).status}: ${name}`)
}

await check('explicit nonexistent order is never replaced with owned order candidates', async () => {
  const id = await open('not-found')
  const s = await send(id, '订单 DEMO-NOT-FOUND-000 的物流呢')
  assert.match(last(s), /未找到匹配订单/)
  assert.equal(s.selectedOrderId, null)
  assert.deepEqual(s.messages.at(-1).orderIds, [])
  return { sessionId: id }
})
await check('capacity uses selected order specification and clarifies unselected variants', async () => {
  const id = await open('capacity')
  const unspecified = await send(id, '保温杯容量多大')
  assert.match(last(unspecified), /500ml/i)
  assert.match(last(unspecified), /750ml/i)
  await send(id, 'TB202610110002')
  const black = await send(id, '这个保温杯容量多大')
  assert.match(last(black), /750ml/i)
  assert.doesNotMatch(last(black), /500ml/i)
  await send(id, 'TB202610100001')
  const white = await send(id, '这个保温杯容量多大')
  assert.match(last(white), /500ml/i)
  assert.doesNotMatch(last(white), /750ml/i)
  return { sessionId: id }
})
await check('handoff contains delay node, order, completed queries, compensation and existing tickets', async () => {
  const id = await open('handoff', 'tb', 2)
  const s = await send(id, '订单 TB202610120005，快递一直没动，必须额外赔我500元，找人工')
  assert.equal(s.session.receptionMode, 'WAITING_HUMAN')
  for (const word of ['TB202610120005', '转运异常', '2026-10-14', '500元', '已完成查询', '已有工单：无'])
    assert.ok(s.session.handoffSummary.includes(word), `missing ${word}: ${s.session.handoffSummary}`)
  return { sessionId: id, handoffSummary: s.session.handoffSummary }
})

let ticket
await check('real model draft, explicit confirmation, guard-backed ticket and idempotency', async () => {
  const id = await open('return', 'dy')
  const before = await tickets()
  // Backend reuses open tickets by order/type even when the reason changes. Never silently close/reset them.
  assert.ok(!before.some(t => t.orderId === 'demo-order-dy-01' && t.ticketType === 'RETURN_REQUEST' &&
    ['PENDING_REVIEW', 'PROCESSING'].includes(t.status)), 'Fresh-ticket prerequisite: finish the existing DY01 return through the merchant workflow first')
  const draft = await send(id, '订单 DY202610100001 的杯子漏水，想退货，原因是杯子漏水')
  assert.equal(draft.awaitingConfirmation, true, last(draft))
  assert.equal((await tickets()).length, before.length)
  const key = crypto.randomUUID()
  const created = await send(id, '确认提交退货诉求', key)
  ticket = created.tickets.find(t => t.ticketId === created.messages.at(-1).ticketId)
  assert.ok(ticket, last(created))
  assert.match(last(created), new RegExp(ticket.ticketNumber))
  assert.equal(created.awaitingConfirmation, false)
  const detail = (await tickets()).find(t => t.ticketId === ticket.ticketId)
  assert.equal(detail.ticketType, 'RETURN_REQUEST')
  assert.equal(detail.status, 'PENDING_REVIEW')
  assert.equal(detail.platformAfterSaleStatus, 'NOT_SUBMITTED_TO_PLATFORM')
  const replay = await send(id, '确认提交退货诉求', key)
  assert.deepEqual(stable(replay), stable(created))
  const concurrent = await Promise.all([send(id, '确认提交退货诉求', key), send(id, '确认提交退货诉求', key)])
  concurrent.forEach(s => assert.deepEqual(stable(s), stable(created)))
  await send(id, '订单 DY202610100001 想退货，原因是杯子漏水')
  const reused = await send(id, '确认提交')
  assert.equal(reused.tickets.filter(t => t.ticketId === ticket.ticketId).length, 1)
  assert.equal((await tickets()).length, before.length + (before.some(t => t.ticketId === ticket.ticketId) ? 0 : 1))
  return { sessionId: id, ticket, confirmationMessageId: key }
})
await check('unconfirmed and cancelled requests never create tickets', async () => {
  const before = (await tickets()).length
  const missing = await open('missing-return', 'dy')
  const s = await send(missing, '我想退货')
  assert.equal(s.awaitingConfirmation, false)
  const cancel = await open('cancel-return', 'dy')
  assert.equal((await send(cancel, '想退货 DY202610100001，原因是漏水')).awaitingConfirmation, true)
  const cancelled = await send(cancel, '先别提交')
  assert.equal(cancelled.awaitingConfirmation, false)
  assert.match(last(cancelled), /取消/)
  assert.match(last(await send(cancel, '确认提交')), /没有可提交/)
  assert.equal((await tickets()).length, before)
  return { sessionIds: [missing, cancel], ticketCount: before }
})
await check('order and ticket lookups cannot cross shop or customer scope', async () => {
  assert.ok(ticket, 'ticket prerequisite')
  const id = await open('scope-tb')
  for (const number of ['DY202610100001', 'TB202610120005']) {
    const s = await send(id, `查订单 ${number} 的物流`)
    assert.match(last(s), /未找到匹配订单/)
    assert.equal(s.selectedOrderId, null)
  }
  assert.match(last(await send(id, `查工单 ${ticket.ticketNumber}`)), /未找到匹配工单/)
  const other = await open('scope-dy', 'dy', 2)
  const foreign = await send(other, `查工单 ${ticket.ticketNumber}`)
  assert.match(last(foreign), /未找到匹配工单/)
  assert.equal(foreign.tickets.length, 0)
  assert.deepEqual(await post('/api/commerce/tickets/query', { scope: scope('dy', 2), ticketNumber: ticket.ticketNumber }), [])
  return { sessionIds: [id, other], ticketNumber: ticket.ticketNumber }
})
await check('merchant processing and completion, optimistic conflict, terminal state cannot reopen', async () => {
  assert.ok(ticket, 'ticket prerequisite')
  const update = { merchantId: 'demo-merchant', shopId: 'demo-shop-dy', ticketId: ticket.ticketId,
    expectedStatus: 'PENDING_REVIEW', status: 'PROCESSING', note: '已联系顾客确认漏水情况', operatorId: 'acceptance-merchant' }
  await post('/api/commerce/tickets/update', update)
  const processing = await send(sessions.return, '刚才的申请怎么样了')
  assert.ok(last(processing).includes(ticket.ticketNumber))
  assert.match(last(processing), /处理中.*已联系顾客确认漏水情况/)
  await post('/api/commerce/tickets/update', update, 409)
  await post('/api/commerce/tickets/update', { ...update, expectedStatus: 'PROCESSING', status: 'COMPLETED', note: '本地诉求已处理，平台结果另行确认' })
  const complete = await send(sessions.return, `工单 ${ticket.ticketNumber} 进度`)
  assert.match(last(complete), /已完成.*本地诉求已处理/)
  assert.match(last(complete), /未执行平台退货或退款/)
  await post('/api/commerce/tickets/update', { ...update, expectedStatus: 'COMPLETED' }, 409)
  return { sessionId: sessions.return, ticketNumber: ticket.ticketNumber }
})
await check('human takeover preserves summary, suppresses AI and resumes on next customer message', async () => {
  const id = sessions.handoff
  assert.ok(id)
  const waiting = await snapshot(id)
  const taken = await post('/api/chat/session/reception/update', { sessionId: id, receptionMode: 'HUMAN' })
  assert.equal(taken.session.handoffSummary, waiting.session.handoffSummary)
  let count = taken.messages.length
  for (const text of ['有人吗', '现在处理了吗', '再帮我提交赔偿']) {
    const s = await send(id, text)
    assert.equal(s.messages.length, ++count)
    assert.equal(s.messages.at(-1).role, 'customer')
  }
  const humanBody = { sessionId: id, clientMessageId: crypto.randomUUID(), content: '已收到，我来核实物流' }
  const human = await post('/api/chat/message/human/send', humanBody)
  assert.equal(human.messages.at(-1).role, 'human')
  assert.deepEqual(stable(await post('/api/chat/message/human/send', humanBody)), stable(human))
  const returned = await post('/api/chat/session/reception/update', { sessionId: id, receptionMode: 'AI' })
  assert.deepEqual(stable(await snapshot(id)), stable(returned))
  const resumed = await send(id, '我刚才那单物流现在是什么状态')
  assert.equal(resumed.selectedOrderId, 'demo-order-tb-05')
  assert.match(last(resumed), /转运异常/)
  // Leave a HUMAN session for the independent restart verification.
  await post('/api/chat/session/reception/update', { sessionId: id, receptionMode: 'HUMAN' })
  return { sessionId: id, humanReplyId: human.messages.at(-1).messageId }
})
await check('pending confirmation remains persisted independently of completed ticket session', async () => {
  const id = await open('restart-pending', 'dy', 2)
  const s = await send(id, '想退货 DY202610090004，原因是不喜欢颜色')
  assert.equal(s.awaitingConfirmation, true, last(s))
  assert.deepEqual(stable(await snapshot(id)), stable(s))
  return { sessionId: id }
})

const revision = spawnSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8', windowsHide: true }).stdout.trim()
const workspaceDirty = Boolean(spawnSync('git', ['status', '--porcelain'], { cwd: root, encoding: 'utf8', windowsHide: true }).stdout.trim())
const report = { generatedAt: new Date().toISOString(), revision, workspaceDirty,
  scope: 'ISOLATED_REAL_MODEL_BUSINESS_HTTP_NOT_COMPLETE_AC', baseUrl: base.origin, backendDatabase: options.database,
  model: 'enabled; valid intent required for ticket creation', resetCalled: false, sessions, checks, requests,
  status: checks.some(c => c.status !== 'PASS') ? 'FAIL' : 'PASS' }
if (positional[0]) {
  const path = resolve(positional[0])
  mkdirSync(dirname(path), { recursive: true })
  writeFileSync(path, JSON.stringify(report, null, 2) + '\n')
}
console.log(JSON.stringify({ status: report.status, checks: checks.map(({ name, status, reason }) => ({ name, status, reason })), reportPath: positional[0] }, null, 2))
if (report.status !== 'PASS') process.exitCode = 1
