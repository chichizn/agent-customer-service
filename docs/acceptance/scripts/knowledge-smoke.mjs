import assert from 'node:assert/strict'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname } from 'node:path'

// Creates dedicated demo conversations; never resets data or submits a ticket.
const mode = process.argv[2]
assert(['--disabled', '--live'].includes(mode), 'Use --disabled or --live')
const disabled = mode === '--disabled'
const baseUrl = `http://127.0.0.1:${disabled ? 10005 : 10002}`
const output = process.argv[3]
assert(output, 'An output JSON path is required')
const report = { generatedAt: new Date().toISOString(), mode, baseUrl,
  database: 'agent_customer_service_acceptance_20261019', dbPort: 13307,
  scope: 'REAL_HTTP_KNOWLEDGE_WITHOUT_RESET_OR_TICKET_SUBMISSION', sessions: {}, checks: [], requests: [] }
async function post(path, body) {
  const started = Date.now()
  const response = await fetch(baseUrl + path, { method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body), signal: AbortSignal.timeout(55000) })
  const envelope = await response.json()
  const s = envelope.data
  report.requests.push({ path, body, httpStatus: response.status, code: envelope.code, elapsedMs: Date.now() - started,
    snapshot: s?.session ? { session: s.session, reply: s.messages.at(-1), analysis: s.analysis,
      knowledge: s.knowledge, selectedOrderId: s.selectedOrderId, awaitingConfirmation: s.awaitingConfirmation,
      tickets: s.tickets, executions: s.executions.slice(-4) } : s })
  assert.equal(response.status, 200)
  assert.equal(envelope.code, 0)
  return s
}
async function send(sessionId, content) {
  return post('/api/chat/message/send', { sessionId, content, clientMessageId: crypto.randomUUID() })
}
const last = s => s.messages.at(-1).content
async function check(name, work) {
  try { await work(); report.checks.push({ name, status: 'PASS' }) }
  catch (e) { report.checks.push({ name, status: 'FAIL', error: e.message }) }
}
try {
  for (const shop of ['tb', 'dy']) {
    const s = await post('/api/chat/session/open', { shopId: `demo-shop-${shop}`, customerId: `demo-customer-${shop}-1`, forceNew: true })
    report.sessions[shop] = s.session.sessionId
  }
  const common = disabled ? [
    ['你好，在吗？', /我在/], ['保温杯怎么清洗？', /中性清洁剂/], ['收纳盒怎么清洗？', /软布/],
    ['毛巾第一次用要怎么洗？', /洗标/], ['支持开发票吗？', /税号/], ['可以用什么付款？', /支付/],
    ['有没有货？', /实时库存/], ['保温杯多少钱？', /结算页/], ['怎么下单？', /收货地址/],
    ['优惠券怎么用？', /有效期/], ['包邮吗？', /包邮范围/], ['什么时候发货？', /发货时间/],
    ['发什么快递？', /承运快递/], ['怎么修改地址？', /修改收货地址/], ['怎么取消订单？', /取消订单/],
    ['怎么退货？', /准备凭证/], ['退款多久到账？', /支付渠道/], ['退货地址在哪？', /退货地址/],
    ['杯盖漏水怎么办？', /清晰照片/], ['漏发了怎么办？', /外包装/], ['工单是什么？', /只记录诉求/],
    ['人工客服几点上班？', /值班时间/], ['不用转人工', /常见商品/], ['谢谢', /不客气/]
  ] : [ ['麻烦讲讲保温杯日常怎样打理', /清洗|清洁|中性清洁剂/], ['开电子发票要准备啥资料', /税号/], ['怎么退货？', /准备凭证/] ]
  for (const [shop, sessionId] of Object.entries(report.sessions)) {
    for (const [question, expected] of common) await check(`${shop}: ${question}`, async () => {
      const s = await send(sessionId, question)
      assert.match(last(s), expected)
      assert.equal(s.session.receptionMode, 'AI')
      assert.equal(s.awaitingConfirmation, false)
      assert.equal(s.tickets.length, 0)
      assert(s.knowledge.length > 0)
      if (disabled) {
        assert.equal(s.analysis.analysisSource, 'RULE')
        assert.equal(s.analysis.replySource, 'RULE_KNOWLEDGE')
      }
    })
    await check(`${shop}: shop return policy`, async () => {
      const s = await send(sessionId, '退货期限几天？')
      assert.match(last(s), new RegExp(shop === 'tb' ? '15天' : '7天'))
      assert(!s.knowledge.some(k => k.shopId && k.shopId !== `demo-shop-${shop}`))
    })
    await check(`${shop}: product discount`, async () => {
      const s = await send(sessionId, shop === 'tb' ? '收纳盒有什么优惠？' : '保温杯有什么优惠？')
      assert.match(last(s), new RegExp(shop === 'tb' ? '9折' : '95折'))
      assert.doesNotMatch(last(s), new RegExp(shop === 'tb' ? '95折' : '单件9折'))
    })
  }
  if (disabled) {
    const tb = report.sessions.tb
    await check('product followup uses prior product', async () => {
      await send(tb, '保温杯有什么颜色？')
      const s = await send(tb, '怎么清洗？')
      assert.match(last(s), /杯盖/)
      assert.doesNotMatch(last(s), /毛巾|收纳盒/)
    })
    await check('unknown question asks for clarification', async () => {
      const s = await send(tb, '火星基地在哪里？')
      assert.equal(s.session.receptionMode, 'AI')
      assert.equal(s.analysis.replySource, 'CLARIFICATION')
      assert.match(last(s), /补充具体问题/)
      assert.doesNotMatch(last(s), /联系人工/)
    })
    await check('logistics still uses real order tool', async () => {
      const s = await send(tb, '查订单 TB202610100001 的物流')
      assert.match(last(s), /杭州/)
      assert.equal(s.selectedOrderId, 'demo-order-tb-01')
    })
    await check('degraded write still hands off without submission', async () => {
      const s = await send(tb, '我想退货 TB202610100001，原因是漏水')
      assert.equal(s.session.receptionMode, 'WAITING_HUMAN')
      assert.equal(s.tickets.length, 0)
    })
    await check('explicit human request still hands off', async () => {
      const s = await send(report.sessions.dy, '我要转人工')
      assert.equal(s.session.receptionMode, 'WAITING_HUMAN')
      assert.equal(s.tickets.length, 0)
    })
  } else await check('real provider selected scoped evidence', async () => {
    const selections = report.requests.filter(r => r.snapshot?.analysis?.replySource === 'MODEL_GENERATED')
    assert(selections.length > 0, 'No successful real model reply generation observed')
  })
} catch (e) { report.checks.push({ name: 'runtime', status: 'FAIL', error: e.message }) }
report.status = report.checks.every(c => c.status === 'PASS') ? 'PASS' : 'FAIL'
mkdirSync(dirname(output), { recursive: true })
writeFileSync(output, JSON.stringify(report, null, 2) + '\n')
console.log(JSON.stringify({ status: report.status, checks: report.checks.length, sessions: report.sessions,
  failed: report.checks.filter(c => c.status !== 'PASS'), modelGenerated: report.requests.filter(r => r.snapshot?.analysis?.replySource === 'MODEL_GENERATED').length }))
if (report.status !== 'PASS') process.exitCode = 1
