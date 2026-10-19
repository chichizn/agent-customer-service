import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { profile, knowledge, root, writeReport } from '../../../demo-data/scripts/check-integration.mjs'

// This probe creates sessions/messages, and never resets data or changes activity policies.
const args = process.argv.slice(2)
const options = {}
const positional = []
for (let i = 0; i < args.length; i++) {
  if (args[i] === '--allow-write') options.allowWrite = true
  else if (args[i] === '--database') options.database = args[++i]
  else if (args[i].startsWith('--')) throw new Error(`Unknown option: ${args[i]}`)
  else positional.push(args[i])
}
assert.ok(options.allowWrite && options.database === 'agent_customer_service_acceptance_20261019',
  'Requires --allow-write --database agent_customer_service_acceptance_20261019 and a verified isolated backend')
const [expectedAt, outputPath] = positional
assert.ok(expectedAt && Number.isFinite(Date.parse(expectedAt)) && outputPath,
  'Usage: demo-time-smoke.mjs --allow-write --database agent_customer_service_acceptance_20261019 expected-offset-time output.json')
const baseUrl = 'http://127.0.0.1:10002'

async function post(path, body) {
  const response = await fetch(new URL(path, baseUrl), {
    method: 'POST', redirect: 'error', signal: AbortSignal.timeout(10000),
    headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
  })
  assert.equal(response.status, 200, `${path}: HTTP ${response.status}`)
  const result = await response.json()
  assert.ok([0, 200].includes(result.code), `${path}: business code ${result.code}`)
  return result.data
}

const checks = []
try {
  const identities = await post('/api/commerce/demo/identities', {})
  assert.deepEqual(identities.businessTime, { mode: 'fixed', effectiveAt: expectedAt, timeZone: 'Asia/Shanghai' })
  checks.push({ name: 'business time metadata', status: 'PASS', businessTime: identities.businessTime })
  for (const actor of [profile.actors.TB3, profile.actors.DY2]) {
    const session = await post('/api/chat/session/open', { ...actor, forceNew: true })
    const clientMessageId = `time-${crypto.randomUUID()}`
    const snapshot = await post('/api/chat/message/send', {
      sessionId: session.session.sessionId, clientMessageId, content: '店铺有什么优惠活动？'
    })
    const activities = knowledge.knowledge.filter(k => k.shopId === actor.shopId && k.kind === 'ACTIVITY')
    const expected = activities.filter(k => Date.parse(k.policy.startsAt) <= Date.parse(expectedAt) &&
      Date.parse(expectedAt) <= Date.parse(k.policy.endsAt)).map(k => k.id).sort()
    assert.deepEqual(snapshot.knowledge.map(k => k.knowledgeId).sort(), expected)
    const reply = snapshot.messages.at(-1).content
    for (const activity of activities) {
      if (expected.includes(activity.id)) assert.ok(reply.includes(activity.content))
      else assert.ok(!reply.includes(activity.content), 'Expired activity appeared in reply')
    }
    const message = snapshot.messages.find(m => m.clientMessageId === clientMessageId)
    // Operational timestamps must stay near actual wall time even with a future business reference.
    assert.ok(message)
    assert.match(message.createdAt, /(?:Z|[+-]\d{2}:\d{2})$/, 'Message timestamp must include its UTC offset')
    assert.ok(Math.abs(Date.now() - Date.parse(message.createdAt)) < 60000,
      'Customer message timestamp must use real Asia/Shanghai time')
    assert.equal(snapshot.tickets.length, 0)
    checks.push({ name: `${actor.shopId} activity visibility`, status: 'PASS', sessionId: session.session.sessionId,
      expectedKnowledgeIds: expected, actualKnowledgeIds: snapshot.knowledge.map(k => k.knowledgeId),
      messageCreatedAt: message.createdAt, lastReply: reply, ticketCount: 0 })
  }
} catch (error) {
  checks.push({ name: 'demo time HTTP probe', status: 'FAIL', reason: error.message })
}
const git = (...args) => spawnSync('git', args, { cwd: root, encoding: 'utf8', windowsHide: true, timeout: 3000 }).stdout?.trim()
const report = { generatedAt: new Date().toISOString(), revision: git('rev-parse', 'HEAD'),
  workspaceDirty: Boolean(git('status', '--porcelain')), scope: 'ISOLATED_MYSQL_DEMO_ACTIVITY_CLOCK_HTTP_NOT_FULL_AC',
  baseUrl, backendDatabase: options.database, model: 'disabled', expectedAt, resetCalled: false,
  status: checks.some(c => c.status === 'FAIL') ? 'FAIL' : 'PASS', checks }
writeReport(report, outputPath)
process.exitCode = report.status === 'PASS' ? 0 : 1
