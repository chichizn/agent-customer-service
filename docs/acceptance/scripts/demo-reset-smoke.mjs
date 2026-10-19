import { createHash } from 'node:crypto'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const EXPECTED_DATABASE = 'agent_customer_service_acceptance_20261019'
const BASE_URL = 'http://127.0.0.1:10002'
const PROJECT_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '../../..')
const args = process.argv.slice(2)
const outputArg = args.at(-1)
const database = valueAfter('--database')

if ((!args.includes('--capture-only') && !args.includes('--allow-write')) || database !== EXPECTED_DATABASE || !outputArg) {
  console.error('Usage: node demo-reset-smoke.mjs [--capture-only | --allow-write] --database agent_customer_service_acceptance_20261019 <output.json>')
  process.exit(2)
}

function valueAfter(flag) {
  const index = args.indexOf(flag)
  return index < 0 ? undefined : args[index + 1]
}

async function post(path, body) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  })
  const text = await response.text()
  let json
  try {
    json = JSON.parse(text)
  } catch {
    json = { message: text.slice(0, 500) }
  }
  return { httpStatus: response.status, json }
}

async function required(path, body) {
  const result = await post(path, body)
  if (!result.json || result.httpStatus < 200 || result.httpStatus >= 300 || result.json.code !== 0) {
    throw new Error(`${path} failed (HTTP ${result.httpStatus}, code ${result.json?.code}): ${result.json?.message ?? 'invalid response'}`)
  }
  return result.json.data
}

function sortedBy(items, key) {
  return [...items].sort((left, right) => String(left[key]).localeCompare(String(right[key])))
}

function digest(value) {
  return createHash('sha256').update(JSON.stringify(value)).digest('hex')
}

async function readState() {
  const identity = await required('/api/commerce/demo/identities', {})
  const listedSessions = await required('/api/chat/session/list', { workbench: true })
  const allSessions = await required('/api/chat/session/list', {})
  const sessions = await Promise.all(sortedBy(listedSessions, 'sessionId').map(async (item) => ({
    session: item,
    snapshot: await required('/api/chat/session/snapshot', { sessionId: item.sessionId }),
  })))
  const workbenchIds = new Set(listedSessions.map((item) => item.sessionId))
  const nonWorkbenchSessions = sortedBy(
    allSessions.filter((item) => !workbenchIds.has(item.sessionId)),
    'sessionId',
  )

  const ticketScopes = []
  for (const shop of identity.shops) {
    for (const customer of shop.customers) {
      const tickets = await required('/api/commerce/tickets/query', {
        scope: {
          merchantId: identity.merchantId,
          shopId: shop.shopId,
          customerId: customer.customerId,
        },
      })
      ticketScopes.push({
        shopId: shop.shopId,
        customerId: customer.customerId,
        tickets: sortedBy(tickets, 'ticketId'),
      })
    }
  }

  const knowledgeEndpoints = {
    products: '/api/knowledge/product/list',
    activities: '/api/knowledge/activity/list',
    faqs: '/api/knowledge/faq/list',
    industries: '/api/knowledge/industry/list',
    solutions: '/api/knowledge/solution/list',
  }
  const knowledge = {}
  for (const [name, endpoint] of Object.entries(knowledgeEndpoints)) {
    const page = await required(endpoint, { page: 1, size: 100 })
    if (page.total > page.records.length) throw new Error(`${name} snapshot is incomplete (${page.records.length}/${page.total})`)
    knowledge[name] = { total: page.total, records: sortedBy(page.records, 'id') }
  }

  return {
    identity,
    demoSessions: sessions,
    nonWorkbenchSessions,
    tickets: ticketScopes,
    knowledge,
    scopedKnowledge: [...new Map(sessions.flatMap(({ snapshot }) => snapshot.knowledge)
      .map((item) => [item.knowledgeId, item])).values()]
      .sort((left, right) => left.knowledgeId.localeCompare(right.knowledgeId)),
  }
}

function summarize(state) {
  return {
    demoSessionCount: state.demoSessions.length,
    messageCount: state.demoSessions.reduce((sum, item) => sum + item.snapshot.messages.length, 0),
    executionCount: state.demoSessions.reduce((sum, item) => sum + item.snapshot.executions.length, 0),
    workbenchTicketCount: new Set(state.demoSessions.flatMap(({ snapshot }) => snapshot.tickets.map((ticket) => ticket.ticketId))).size,
    merchantTicketCount: state.tickets.reduce((sum, scope) => sum + scope.tickets.length, 0),
    nonWorkbenchSessionCount: state.nonWorkbenchSessions.length,
    legacyKnowledgeCounts: Object.fromEntries(Object.entries(state.knowledge).map(([name, page]) => [name, page.total])),
    referencedScopedKnowledgeCount: state.scopedKnowledge.length,
  }
}

async function verifyRestoredKnowledge() {
  const fixturePath = resolve(PROJECT_ROOT, 'demo-data/knowledge.integration.v1.json')
  const fixture = JSON.parse(await readFile(fixturePath, 'utf8'))
  const found = new Map()
  const shops = [
    { shopId: 'demo-shop-tb', customerId: 'demo-customer-tb-1' },
    { shopId: 'demo-shop-dy', customerId: 'demo-customer-dy-1' },
  ]
  const prompts = [
    '保温杯规格和容量是什么？',
    '收纳盒尺寸是多少？',
    '本地工单是什么？',
    '物流配送时间说明是什么？',
    '退货期限和退货运费规则是什么？',
    '活动折扣是什么？',
  ]
  for (const shop of shops) {
    const opened = await required('/api/chat/session/open', { ...shop, forceNew: true })
    const sessionId = opened.session.sessionId
    for (const [index, content] of prompts.entries()) {
      const snapshot = await required('/api/chat/message/send', {
        sessionId,
        clientMessageId: `ac22-restore-${shop.shopId}-${index}`,
        content,
      })
      for (const item of snapshot.knowledge) found.set(item.knowledgeId, item)
    }
  }
  const expected = fixture.knowledge.map((item) => ({
    knowledgeId: item.id,
    title: item.title,
    source: item.source,
    content: item.content,
    scope: item.scope,
  })).sort((left, right) => left.knowledgeId.localeCompare(right.knowledgeId))
  const actual = [...found.values()].map(({ knowledgeId, title, source, content, scope }) => ({
    knowledgeId,
    title,
    source,
    content,
    scope,
  })).sort((left, right) => left.knowledgeId.localeCompare(right.knowledgeId))
  return {
    status: digest(actual) === digest(expected) ? 'PASS' : 'FAIL',
    expectedCount: expected.length,
    actualCount: actual.length,
    matchesSeedContent: digest(actual) === digest(expected),
    records: actual,
  }
}

const outputPath = resolve(outputArg)

if (args.includes('--prime-knowledge')) {
  const opened = await required('/api/chat/session/open', {
    shopId: 'demo-shop-tb',
    customerId: 'demo-customer-tb-1',
    forceNew: true,
  })
  const sessionId = opened.session.sessionId
  const prompts = [
    { id: 'demo-knowledge-box', clientMessageId: 'ac22-knowledge-dimensions', content: '收纳盒尺寸是多少？' },
    { id: 'demo-knowledge-ticket', clientMessageId: 'ac22-knowledge-ticket', content: '这个本地工单代表什么？' },
  ]
  const checks = []
  for (const prompt of prompts) {
    const snapshot = await required('/api/chat/message/send', {
      sessionId,
      clientMessageId: prompt.clientMessageId,
      content: prompt.content,
    })
    const knowledge = snapshot.knowledge.filter((item) => item.knowledgeId === prompt.id)
    checks.push({
      name: `read scoped knowledge ${prompt.id}`,
      status: knowledge.length === 1 ? 'PASS' : 'FAIL',
      content: knowledge,
      latestMessage: snapshot.messages.at(-1),
      sessionId,
    })
  }
  const primeReport = {
    generatedAt: new Date().toISOString(),
    baseUrl: BASE_URL,
    database,
    scope: 'ISOLATED_DEMO_KNOWLEDGE_BASELINE_PRIME',
    checks,
    status: checks.every((check) => check.status === 'PASS') ? 'PASS' : 'FAIL',
  }
  await mkdir(dirname(outputPath), { recursive: true })
  await writeFile(outputPath, `${JSON.stringify(primeReport, null, 2)}\n`)
  console.log(JSON.stringify(primeReport, null, 2))
  if (primeReport.status !== 'PASS') process.exit(1)
  process.exit(0)
}

const preReset = await readState()
const preResetSnapshotPath = outputPath.replace(/\.json$/i, '.before.json')
await mkdir(dirname(outputPath), { recursive: true })
await writeFile(preResetSnapshotPath, `${JSON.stringify({ database, capturedAt: new Date().toISOString(), state: preReset }, null, 2)}\n`)

if (args.includes('--capture-only')) {
  const captureReport = {
    generatedAt: new Date().toISOString(),
    baseUrl: BASE_URL,
    database,
    scope: 'READ_ONLY_RESET_BASELINE_CAPTURE',
    snapshot: preResetSnapshotPath,
    status: 'CAPTURE_ONLY',
    counts: summarize(preReset),
  }
  await writeFile(outputPath, `${JSON.stringify(captureReport, null, 2)}\n`)
  console.log(JSON.stringify(captureReport, null, 2))
  process.exit(0)
}

const report = {
  generatedAt: new Date().toISOString(),
  baseUrl: BASE_URL,
  database,
  scope: 'ISOLATED_DEMO_RESET_API_SMOKE; UI_CANCEL_AND_SECOND_TAB_NOT_COVERED',
  preResetSnapshot: preResetSnapshotPath,
  before: summarize(preReset),
  checks: [],
}

const denied = await post('/api/demo/reset', { confirmed: false })
const afterDenied = await readState()
const deniedNoMutation = digest(preReset) === digest(afterDenied)
report.checks.push({
  name: 'unconfirmed reset is rejected without data mutation',
  status: denied.httpStatus >= 400 || denied.json?.code !== 0 ? (deniedNoMutation ? 'PASS' : 'FAIL') : 'FAIL',
  response: { httpStatus: denied.httpStatus, code: denied.json?.code, message: denied.json?.message },
  stateUnchanged: deniedNoMutation,
})

if (!deniedNoMutation || !(denied.httpStatus >= 400 || denied.json?.code !== 0)) {
  report.status = 'FAIL'
  report.failure = 'The unconfirmed reset did not safely reject without mutation; confirmed reset was not attempted.'
  await writeFile(outputPath, `${JSON.stringify(report, null, 2)}\n`)
  process.exit(1)
}

for (const attempt of [1, 2]) {
  const result = await post('/api/demo/reset', { confirmed: true })
  report.checks.push({
    name: `confirmed unified reset ${attempt}`,
    status: result.httpStatus >= 200 && result.httpStatus < 300 && result.json?.code === 0 ? 'PASS' : 'FAIL',
    response: { httpStatus: result.httpStatus, code: result.json?.code, message: result.json?.message },
  })
  if (result.httpStatus < 200 || result.httpStatus >= 300 || result.json?.code !== 0) break
}

const afterTwoResets = await readState()
report.afterTwoResets = summarize(afterTwoResets)
report.checks.push({
  name: 'two consecutive resets clear demo sessions, messages, executions and tickets',
  status: afterTwoResets.demoSessions.length === 0 && afterTwoResets.tickets.every((scope) => scope.tickets.length === 0) ? 'PASS' : 'FAIL',
  counts: summarize(afterTwoResets),
})
report.checks.push({
  name: 'two resets preserve non-workbench sessions',
  status: digest(preReset.nonWorkbenchSessions) === digest(afterTwoResets.nonWorkbenchSessions) ? 'PASS' : 'FAIL',
  beforeCount: preReset.nonWorkbenchSessions.length,
  afterCount: afterTwoResets.nonWorkbenchSessions.length,
  coverage: preReset.nonWorkbenchSessions.length === 0 ? 'No non-workbench sessions existed at baseline; equality is vacuous.' : undefined,
})
report.checks.push({
  name: 'two resets preserve legacy knowledge IDs and content',
  status: digest(preReset.knowledge) === digest(afterTwoResets.knowledge) ? 'PASS' : 'FAIL',
  before: summarize(preReset).legacyKnowledgeCounts,
  after: summarize(afterTwoResets).legacyKnowledgeCounts,
})

try {
  report.restoredScopedKnowledge = await verifyRestoredKnowledge()
  report.checks.push({
    name: 'all scoped knowledge is restored with seed IDs and content',
    status: report.restoredScopedKnowledge.status,
    expectedCount: report.restoredScopedKnowledge.expectedCount,
    actualCount: report.restoredScopedKnowledge.actualCount,
  })
} catch (error) {
  report.restoredScopedKnowledge = { status: 'FAIL', error: error.message }
  report.checks.push({ name: 'all scoped knowledge is restored with seed IDs and content', status: 'FAIL', error: error.message })
}

const cleanupReset = await post('/api/demo/reset', { confirmed: true })
report.checks.push({
  name: 'cleanup reset removes knowledge verification sessions',
  status: cleanupReset.httpStatus >= 200 && cleanupReset.httpStatus < 300 && cleanupReset.json?.code === 0 ? 'PASS' : 'FAIL',
  response: { httpStatus: cleanupReset.httpStatus, code: cleanupReset.json?.code, message: cleanupReset.json?.message },
})
const afterReset = await readState()
report.after = summarize(afterReset)
report.checks.push({
  name: 'isolated demo returns to a clean state after verification',
  status: afterReset.demoSessions.length === 0 && afterReset.tickets.every((scope) => scope.tickets.length === 0) ? 'PASS' : 'FAIL',
  counts: summarize(afterReset),
})
report.checks.push({
  name: 'cleanup reset preserves legacy knowledge IDs and content',
  status: digest(preReset.knowledge) === digest(afterReset.knowledge) ? 'PASS' : 'FAIL',
})

const staleSessionId = preReset.demoSessions[0]?.session.sessionId
if (staleSessionId) {
  const staleSnapshot = await post('/api/chat/session/snapshot', { sessionId: staleSessionId })
  const staleWrite = await post('/api/chat/message/send', {
    sessionId: staleSessionId,
    clientMessageId: `ac22-stale-${Date.now()}`,
    content: '验证重置后旧会话不可继续写入',
  })
  const snapshotRejected = staleSnapshot.httpStatus >= 400 || staleSnapshot.json?.code !== 0
  const writeRejected = staleWrite.httpStatus >= 400 || staleWrite.json?.code !== 0
  report.checks.push({
    name: 'stale demo session cannot be read or written after reset',
    status: snapshotRejected && writeRejected ? 'PASS' : 'FAIL',
    staleSessionId,
    snapshot: { httpStatus: staleSnapshot.httpStatus, code: staleSnapshot.json?.code, message: staleSnapshot.json?.message },
    write: { httpStatus: staleWrite.httpStatus, code: staleWrite.json?.code, message: staleWrite.json?.message },
  })
}

report.status = report.checks.every((check) => check.status === 'PASS') ? 'PASS' : 'FAIL'
await writeFile(outputPath, `${JSON.stringify(report, null, 2)}\n`)
console.log(JSON.stringify({ status: report.status, before: report.before, after: report.after, checks: report.checks }, null, 2))
if (report.status !== 'PASS') process.exit(1)
