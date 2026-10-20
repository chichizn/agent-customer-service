import assert from 'node:assert/strict'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname } from 'node:path'

// Run only after independently verifying API10002 uses the isolated MySQL13307 acceptance schema.
// Creates one demo conversation; does not reset data or submit business writes.
const output = process.argv[2]
assert(output, 'An output JSON path is required')
const report = { generatedAt: new Date().toISOString(), baseUrl: 'http://127.0.0.1:10002',
  database: 'agent_customer_service_acceptance_20261019', dbPort: 13307, checks: [], requests: [] }
async function post(path, body) {
  const start = Date.now()
  const response = await fetch(report.baseUrl + path, { method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body), signal: AbortSignal.timeout(55000) })
  const envelope = await response.json()
  assert.equal(response.status, 200)
  assert.equal(envelope.code, 0)
  const s = envelope.data
  report.requests.push({ path, body, elapsedMs: Date.now() - start, session: s.session, reply: s.messages.at(-1),
    analysis: s.analysis, knowledge: s.knowledge, awaitingConfirmation: s.awaitingConfirmation, tickets: s.tickets,
    executions: s.executions.slice(-5) })
  return s
}
async function check(name, work) {
  try { await work(); report.checks.push({ name, status: 'PASS' }) }
  catch (e) { report.checks.push({ name, status: 'FAIL', error: e.message }) }
}
try {
  const opened = await post('/api/chat/session/open', { shopId: 'demo-shop-tb', customerId: 'demo-customer-tb-3', forceNew: true })
  report.sessionId = opened.session.sessionId
  for (const question of ['我有点纠结，陪我选选吧', '平时上下班通勤，想带着喝水，该从哪些方面考虑？', '这个保温杯具体能保温多少小时？']) {
    await check(question, async () => {
      const s = await post('/api/chat/message/send', { sessionId: report.sessionId, clientMessageId: crypto.randomUUID(), content: question })
      assert(['MODEL_GENERATED', 'MODEL_GENERAL'].includes(s.analysis.replySource))
      assert.equal(s.session.receptionMode, 'AI')
      assert.equal(s.awaitingConfirmation, false)
      assert.equal(s.tickets.length, 0)
      assert(s.messages.at(-1).content.trim().length > 0)
      assert(!s.knowledge.some(k => k.knowledgeId.startsWith('demo-knowledge-dy-')))
      if (question.includes('多少小时')) {
        assert.doesNotMatch(s.messages.at(-1).content, /(?:保温|保持温度).{0,8}\d+\s*小时/)
      }
    })
  }
} catch (e) { report.checks.push({ name: 'runtime', status: 'FAIL', error: e.message }) }
report.status = report.checks.every(c => c.status === 'PASS') ? 'PASS' : 'FAIL'
mkdirSync(dirname(output), { recursive: true })
writeFileSync(output, JSON.stringify(report, null, 2) + '\n')
console.log(JSON.stringify({ status: report.status, checks: report.checks.length, failed: report.checks.filter(c => c.status !== 'PASS') }))
if (report.status !== 'PASS') process.exitCode = 1
