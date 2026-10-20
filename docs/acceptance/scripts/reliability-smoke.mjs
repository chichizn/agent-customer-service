import assert from 'node:assert/strict'
import { mkdirSync, writeFileSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { execFileSync } from 'node:child_process'

// Paired exclusively with AcceptanceRuntime.java (loopback + verified isolated MySQL).
assert(process.argv.includes('--allow-reset'), 'Explicit --allow-reset required: backs up, then resets isolated demo data')
const output = process.argv.find((v, i) => i > 1 && !v.startsWith('--'))
assert(output, 'Provide report path')
const api = 'http://127.0.0.1:10003', control = 'http://127.0.0.1:10004'
const requests = [], checks = [], sessions = {}
async function request(base, path, body, status = 200, options = {}) {
  const start = Date.now()
  const r = await fetch(base + path, { method: body ? 'POST' : 'GET', redirect: 'error',
    signal: AbortSignal.timeout(25000), ...options,
    headers: { 'Content-Type': 'application/json' }, ...(body ? { body: JSON.stringify(body) } : {}) })
  const result = await r.json()
  requests.push({ path, body, status: r.status, elapsedMs: Date.now() - start, response: result })
  assert.equal(r.status, status, `${path}: ${JSON.stringify(result)}`)
  if (base === api && status < 300) assert.equal(result.code, 0)
  return base === api ? result.data : result
}
const post = (path, body, status) => request(api, path, body, status)
const ctl = (path, body = {}) => request(control, path, body)
const scope = (shop = 'dy', customer = 1) => ({ merchantId: 'demo-merchant', shopId: `demo-shop-${shop}`, customerId: `demo-customer-${shop}-${customer}` })
const last = s => s.messages.at(-1).content
const send = (sessionId, content, clientMessageId = crypto.randomUUID(), status) => post('/api/chat/message/send', { sessionId, content, clientMessageId }, status)
const snapshot = sessionId => post('/api/chat/session/snapshot', { sessionId })
const reception = (sessionId, receptionMode) => post('/api/chat/session/reception/update', { sessionId, receptionMode })
const queryTickets = (shop = 'dy', customer = 1) => post('/api/commerce/tickets/query', { scope: scope(shop, customer) })
async function open(key, shop = 'dy', customer = 1) {
  const s = await post('/api/chat/session/open', { ...scope(shop, customer), forceNew: true })
  return sessions[key] = s.session.sessionId
}
async function mode(value) { return ctl('/control', { mode: value }) }
async function waitFor(action, condition) {
  const deadline = Date.now() + 18000
  while (Date.now() < deadline) {
    const value = await action()
    if (condition(value)) return value
    await new Promise(r => setTimeout(r, 80))
  }
  throw new Error('Timed out waiting for observable acceptance state')
}
const waitGate = point => waitFor(() => request(control, '/state'), s => s.entered === point)
async function update(t, status, note = '验收商家处理', expectedStatus = t.status, httpStatus = 200) {
  return post('/api/commerce/tickets/update', { merchantId: t.merchantId, shopId: t.shopId, ticketId: t.ticketId,
    expectedStatus, status, note, operatorId: 'acceptance-operator' }, httpStatus)
}
async function closeOpen() {
  for (let t of await queryTickets()) {
    if (t.status === 'PENDING_REVIEW') t = await update(t, 'PROCESSING')
    if (t.status === 'PROCESSING') await update(t, 'COMPLETED')
  }
}
async function draft(key) {
  await closeOpen()
  const id = await open(key)
  const s = await send(id, '订单 DY202610100001 的杯子漏水，想退货，原因是杯子漏水')
  assert.equal(s.awaitingConfirmation, true, last(s))
  return id
}
async function check(name, ac, action) {
  try { checks.push({ name, ac, status: 'PASS', evidence: await action() }) }
  catch (e) { checks.push({ name, ac, status: 'FAIL', reason: e.stack }) }
  finally { await ctl('/release'); await ctl('/fault', { fault: 'clear' }); await mode('valid') }
  console.log(`${checks.at(-1).status}: ${name}`)
}

const initial = await request(control, '/state')
assert.equal(initial.database, 'agent_customer_service_acceptance_20261019')
assert.equal(initial.dbPort, 13307)
const backup = await ctl('/backup')
const sentinels = await ctl('/sentinel')
await post('/api/demo/reset', { confirmed: true })
await mode('valid')
try {
  await check('first concurrent confirmation and identical open request reuse', ['AC-08', 'AC-10'], async () => {
    const id = await draft('concurrent'), key = crypto.randomUUID()
    await mode('hold-model')
    const first = send(id, '确认提交', key)
    await waitGate('model-request')
    const conflict = await send(id, '确认提交', key, 409)
    await ctl('/release')
    const s = await first
    const ticket = s.tickets.find(t => t.ticketId === s.messages.at(-1).ticketId)
    assert(ticket)
    assert.equal((await queryTickets()).filter(t => t.orderId === ticket.orderId && t.status === 'PENDING_REVIEW').length, 1)
    assert.equal((await send(id, '确认提交', key)).messages.at(-1).ticketId, ticket.ticketId)
    await mode('valid')
    await send(id, '订单 DY202610100001 的杯子漏水，想退货，原因是杯子漏水')
    assert.equal((await send(id, '确认提交')).messages.at(-1).ticketId, ticket.ticketId)
    return { sessionId: id, key, ticket, firstConcurrentStatus: 409, conflict }
  })
  await check('lost HTTP response original key retry produces one ticket', ['AC-10'], async () => {
    const id = await draft('lost-response'), key = crypto.randomUUID(), abort = new AbortController()
    await mode('hold-model')
    const pending = sendRaw(id, key, abort.signal).catch(e => ({ aborted: e.name }))
    await waitGate('model-request')
    abort.abort()
    const disconnected = await pending
    await ctl('/release')
    const done = await waitFor(() => snapshot(id), s => !s.awaitingConfirmation && s.tickets.some(t => t.sessionId === id))
    const retry = await send(id, '确认提交', key)
    assert.equal(retry.messages.at(-1).ticketId, done.messages.at(-1).ticketId)
    assert.equal(retry.messages.filter(m => m.clientMessageId === key).length, 1)
    return { sessionId: id, key, disconnected, ticketId: retry.messages.at(-1).ticketId }
  })
  for (const [testMode, point] of [['hold-model', 'model-request'], ['hold-before-finish', 'before-finish']]) {
    await check(`takeover at ${point} cancels old turn and prevents ticket`, ['AC-17'], async () => {
      const id = await draft(`takeover-${point}`), before = await snapshot(id)
      const ticketCount = (await queryTickets()).length
      await mode(testMode)
      const pending = send(id, '确认提交')
      await waitGate(point)
      const start = Date.now(), taken = await reception(id, 'HUMAN'), takeoverMs = Date.now() - start
      await ctl('/release'); await pending
      const after = await snapshot(id)
      assert.equal(after.session.receptionMode, 'HUMAN')
      assert.equal(after.messages.filter(m => m.role === 'ai').length, before.messages.filter(m => m.role === 'ai').length)
      assert.equal((await queryTickets()).length, ticketCount)
      assert.equal(after.awaitingConfirmation, false)
      return { sessionId: id, point, takeoverMs, takenAt: taken.executions.at(-1).createdAt, after }
    })
  }
  await check('create failure rolls back ticket submission history and reply, original key recovers', ['AC-20'], async () => {
    const id = await draft('create-failure'), key = crypto.randomUUID()
    const before = await request(control, '/state')
    await ctl('/fault', { fault: 'create', sessionId: id })
    await send(id, '确认提交', key, 503)
    await ctl('/fault', { fault: 'clear' })
    const failed = await snapshot(id), after = await request(control, '/state')
    for (const field of ['commerce_service_ticket', 'commerce_ticket_submission', 'commerce_ticket_history']) assert.equal(after[field], before[field], field)
    assert(failed.executions.some(e => e.status === 'FAILED'))
    assert.equal(failed.awaitingConfirmation, true)
    assert(!failed.messages.some(m => m.ticketId))
    const recovered = await send(id, '确认提交', key)
    assert(recovered.messages.at(-1).ticketId)
    return { sessionId: id, key, before, after, failed, recovered }
  })
  await check('merchant update rollback, failure record, retry, rejection and terminal/stale conflicts', ['AC-12', 'AC-20'], async () => {
    const id = await draft('update-failure'), created = await send(id, '确认提交')
    const t = (await queryTickets()).find(t => t.ticketId === created.messages.at(-1).ticketId)
    await ctl('/fault', { fault: 'update', sessionId: id })
    await update(t, 'REJECTED', '凭证待补充', t.status, 503)
    await ctl('/fault', { fault: 'clear' })
    const failed = (await queryTickets()).find(v => v.ticketId === t.ticketId)
    assert.equal(failed.status, t.status); assert.equal(failed.history.length, 1)
    assert((await snapshot(id)).executions.some(e => e.action === 'update_service_ticket' && e.status === 'FAILED'))
    const rejected = await update(t, 'REJECTED', '凭证待补充')
    await update(rejected, 'PROCESSING', '非法重开', 'REJECTED', 409)
    await update(t, 'PROCESSING', '旧状态重放', t.status, 409)
    const result = await send(id, '刚才的申请怎么样了')
    assert.match(last(result), /已拒绝/); assert.match(last(result), /凭证待补充/)
    assert(result.executions.some(e => e.action === 'update_service_ticket' && e.status === 'SUCCESS'))
    return { sessionId: id, failed, rejected, result }
  })
  await check('query tool failure is recorded and same message recovers', ['AC-20'], async () => {
    const id = await open('query-failure', 'tb'), key = crypto.randomUUID()
    await ctl('/fault', { fault: 'query' })
    await send(id, '查订单 TB202610100001 的物流', key, 503)
    await ctl('/fault', { fault: 'clear' })
    const failed = await snapshot(id)
    assert(failed.executions.some(e => e.status === 'FAILED'))
    const recovered = await send(id, '查订单 TB202610100001 的物流', key)
    assert.match(last(recovered), /杭州/)
    return { sessionId: id, key, failed, recovered }
  })
  for (const failureMode of ['disabled', '500', 'timeout', 'bad-json', 'unknown-intent']) {
    await check(`${failureMode}: real query, safe write handoff, manual/history/ticket availability`, ['AC-19'], async () => {
      const id = await open(`failure-${failureMode}`, 'tb')
      await mode(failureMode)
      const start = Date.now(), queried = await send(id, '查订单 TB202610100001 的物流'), elapsedMs = Date.now() - start
      assert(elapsedMs < 7000); assert.match(last(queried), /模型不可用/); assert.match(last(queried), /杭州/)
      const write = await send(id, '订单 TB202610100001 想退货，原因是漏水')
      assert.equal(write.session.receptionMode, 'WAITING_HUMAN')
      await reception(id, 'HUMAN')
      const manual = await post('/api/chat/message/human/send', { sessionId: id, clientMessageId: crypto.randomUUID(), content: '已收到，我来核实物流' })
      assert.equal(manual.messages.at(-1).role, 'human')
      assert.equal(last(await snapshot(id)), '已收到，我来核实物流')
      await queryTickets('tb')
      return { sessionId: id, elapsedMs, readTimeoutMs: 1000, queried, write, manual }
    })
  }
  await check('logistics single/multiple/empty and ownership isolation', ['AC-03', 'AC-04', 'AC-05', 'AC-06', 'AC-07'], async () => {
    const id = await open('orders', 'tb'), multiple = await send(id, '我的杯子到哪了')
    assert.equal(multiple.selectedOrderId, null); assert.match(last(multiple), /TB202610100001/); assert.match(last(multiple), /TB202610110002/)
    assert.match(last(await send(id, '选择 TB202610100001')), /杭州/)
    assert.match(last(await send(id, '那这个现在到哪里了')), /杭州/)
    assert.match(last(await send(id, '订单 TB202610110002 的物流')), /尚未发货/)
    assert.match(last(await send(id, '订单 TB202610080004 的物流')), /已取消/)
    for (const number of ['DEMO-NOT-FOUND-000', 'DY202610100001', 'TB202610120005'])
      assert.match(last(await send(id, `订单 ${number} 的物流`)), /未找到/)
    const emptyId = await open('empty', 'tb', 3), empty = await send(emptyId, '查一下我的订单物流')
    assert.equal(empty.orders.length, 0); assert.match(last(empty), /未找到/)
    const delayId = await open('delay', 'tb', 2), delay = await send(delayId, '我的保温杯物流到哪了')
    assert.match(last(delay), /转运异常/); assert.equal(delay.selectedOrderId, 'demo-order-tb-05')
    const foreign = (await queryTickets())[0]
    assert.equal((await post('/api/commerce/tickets/query', { scope: scope('tb'), ticketNumber: foreign.ticketNumber })).length, 0)
    assert.equal((await post('/api/commerce/tickets/query', { scope: scope('dy', 2), ticketNumber: foreign.ticketNumber })).length, 0)
    return { sessionId: id, emptyId, delayId, multiple, empty, delay }
  })
  await check('pending cancellation and no-information writes do not create tickets', ['AC-09'], async () => {
    const id = await open('cancel'), count = (await queryTickets()).length
    await send(id, '我想退货')
    assert.equal((await queryTickets()).length, count)
    await send(id, '订单 DY202610100001 想退货，原因是漏水')
    assert.equal((await send(id, '先别提交')).awaitingConfirmation, false)
    assert.match(last(await send(id, '确认提交')), /没有可提交/)
    assert.equal((await queryTickets()).length, count)
    return { sessionId: id, count }
  })
  await check('rules and activities use scoped sources and variants; missing knowledge has no guarantee', ['AC-13', 'AC-14'], async () => {
    const evidence = []
    for (const shop of ['tb', 'dy']) {
      const id = await open(`knowledge-${shop}`, shop)
      const rule = await send(id, '签收后几天可以申请退货？寄回运费怎么办？')
      assert.match(last(rule), shop === 'tb' ? /15天/ : /7天/)
      assert(rule.knowledge.length > 0)
      const activity = await send(id, '现在杯子和收纳盒有什么优惠')
      assert.match(last(activity), shop === 'tb' ? /9折/ : /95折/)
      const missing = await send(id, '保温杯保温48小时有检测报告吗，今天下单保证明天到吗')
      assert.match(last(missing), /未提供|缺少|无法|不能/)
      evidence.push({ sessionId: id, rule, activity, missing })
    }
    return evidence
  })
  await check('handoff summary, no AI during human service, human idempotency and return to AI', ['AC-15', 'AC-16', 'AC-18'], async () => {
    const id = await open('human', 'tb', 2), handoff = await send(id, '订单 TB202610120005，快递一直没动，必须额外赔我500元，找人工')
    for (const part of ['TB202610120005', '转运异常', '500元', '已完成查询']) assert(handoff.session.handoffSummary.includes(part))
    await reception(id, 'HUMAN')
    const before = await snapshot(id), count = before.messages.filter(m => m.role === 'ai').length
    for (const content of ['有人吗', '现在处理了吗', '再帮我提交赔偿']) await send(id, content)
    assert.equal((await snapshot(id)).messages.filter(m => m.role === 'ai').length, count)
    const message = { sessionId: id, clientMessageId: crypto.randomUUID(), content: '已收到，我来核实物流' }
    await post('/api/chat/message/human/send', message); await post('/api/chat/message/human/send', message)
    assert.equal((await snapshot(id)).messages.filter(m => m.clientMessageId === message.clientMessageId).length, 1)
    const ai = await reception(id, 'AI')
    assert.equal(ai.messages.filter(m => m.role === 'ai').length, count)
    assert.match(last(await send(id, '我刚才那单物流现在是什么状态')), /转运异常/)
    return { sessionId: id, handoff, before, ai }
  })
  await check('reset declines without changes, resets twice and preserves all sentinel fields', ['AC-22'], async () => {
    const before = await request(control, '/state')
    await post('/api/demo/reset', { confirmed: false }, 400)
    const cancelled = await request(control, '/state')
    for (const key of Object.keys(before).filter(k => k.startsWith('cs_') || k.startsWith('commerce_'))) assert.equal(cancelled[key], before[key], key)
    const id = sessions.human
    await post('/api/demo/reset', { confirmed: true })
    const first = await request(control, '/state')
    await post('/api/demo/reset', { confirmed: true })
    const second = await request(control, '/state')
    for (const counts of [first, second]) {
      for (const [key, value] of Object.entries({ commerce_merchant: 1, commerce_shop: 2, commerce_customer: 5, commerce_order: 12, commerce_logistics: 7,
        commerce_service_ticket: 0, commerce_ticket_submission: 0, commerce_ticket_history: 0, cs_chat_session: 1, cs_chat_message: 0, cs_chat_execution: 0, cs_scoped_knowledge: 11 })) assert.equal(counts[key], value, key)
    }
    assert.deepEqual(await ctl('/sentinel'), sentinels)
    await post('/api/chat/session/snapshot', { sessionId: id }, 404)
    await send(id, '旧任务不能写入', crypto.randomUUID(), 404)
    return { before, cancelled, first, second, sentinels }
  })
} finally {
  await ctl('/release'); await ctl('/fault', { fault: 'clear' }); await mode('valid')
  const report = { generatedAt: new Date().toISOString(), sourceCommit: execFileSync('git', ['rev-parse', 'HEAD'], { encoding: 'utf8' }).trim(),
    workspaceDirty: true, environment: { api, control, database: initial.database, dbPort: initial.dbPort, model: 'local-fixture (no real provider)',
      disabled: 'test wrapper returns unavailable without model request', beforeFinish: 'pause after model response before finish transaction' },
    backup, checks, requests, sessions, passed: checks.filter(c => c.status === 'PASS').length, failed: checks.filter(c => c.status === 'FAIL').length }
  mkdirSync(dirname(resolve(output)), { recursive: true }); writeFileSync(resolve(output), JSON.stringify(report, null, 2) + '\n')
  console.log(JSON.stringify({ passed: report.passed, failed: report.failed }))
  if (report.failed) process.exitCode = 1
}

async function sendRaw(sessionId, clientMessageId, signal) {
  return fetch(api + '/api/chat/message/send', { method: 'POST', signal, headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId, clientMessageId, content: '确认提交' }) })
}
