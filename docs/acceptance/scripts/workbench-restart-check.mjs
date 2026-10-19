import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { readFileSync } from 'node:fs'
import { writeReport } from '../../../demo-data/scripts/check-integration.mjs'

// Query only: capture before a deliberate backend restart, then verify afterwards.
const [mode, inputPath, outputPath] = process.argv.slice(2)
assert.ok(['capture', 'verify'].includes(mode) && inputPath && outputPath,
  'Usage: node workbench-restart-check.mjs capture|verify input.json output.json')
const input = JSON.parse(readFileSync(inputPath, 'utf8'))
const baseUrl = new URL(input.baseUrl)
assert.ok(baseUrl.protocol === 'http:' && ['localhost', '127.0.0.1', '[::1]'].includes(baseUrl.hostname), 'Loopback backend required')
const sessions = mode === 'capture' ? input.createdSessionIds : Object.fromEntries(input.sessions.map(s => [s.name, s.sessionId]))
assert.ok(sessions && Object.keys(sessions).length > 0, 'Input must contain captured smoke sessions')

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical)
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])]))
  return value
}

const results = []
for (const [name, sessionId] of Object.entries(sessions)) {
  const response = await fetch(new URL('/api/chat/session/snapshot', baseUrl), {
    method: 'POST', redirect: 'error', signal: AbortSignal.timeout(10000),
    headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sessionId }),
  })
  assert.equal(response.status, 200, `${name}: HTTP ${response.status}`)
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code))
  const s = payload.data
  assert.equal(s.session.sessionId, sessionId)
  results.push({ name, sessionId, snapshotSha256: createHash('sha256').update(JSON.stringify(canonical(s))).digest('hex'),
    messageCount: s.messages.length, executionCount: s.executions.length,
    humanMessageCount: s.messages.filter(m => m.role === 'human').length,
    receptionMode: s.session.receptionMode, selectedOrderId: s.selectedOrderId, ticketCount: s.tickets.length })
}
let status = 'CAPTURED'
if (mode === 'verify') {
  assert.deepEqual(results, input.sessions, 'Persisted snapshots changed across the restart')
  status = 'PASS'
}
writeReport({ generatedAt: new Date().toISOString(), baseUrl: baseUrl.origin,
  scope: 'READ_ONLY_WORKBENCH_SNAPSHOT_RESTART_COMPARISON_NOT_FULL_AC', status, sessions: results }, outputPath)
