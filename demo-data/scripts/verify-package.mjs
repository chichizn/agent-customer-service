import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { validateDataset } from './validate.mjs'

// Offline checks for the C deliverables; no application server or model is called.
const fixturePath = fileURLToPath(new URL('../dataset.v1.json', import.meta.url))
const raw = readFileSync(fixturePath)
const data = JSON.parse(raw.toString('utf8'))
assert.deepEqual(validateDataset(data), [])
const mutations = [
  ['wrong customer ownership', d => { d.orders[0].customerId = 'demo-customer-dy-01' }],
  ['wrong amount', d => { d.orders[0].paidAmountCents++ }],
  ['duplicate order', d => { d.orders.push(structuredClone(d.orders[0])) }],
  ['cross-shop knowledge scope', d => { d.knowledge.find(k => k.scope === 'SHOP').merchantId = 'demo-missing-merchant' }],
  ['tracking timeline reversal', d => { d.logistics[0].nodes.reverse() }]
]
const negativeChecks = mutations.map(([name, mutate]) => {
  const broken = structuredClone(data)
  mutate(broken)
  assert(validateDataset(broken).length > 0, `Validator did not reject: ${name}`)
  return { name, status: 'PASS' }
})

const originalArgs = process.argv
const originalFetch = globalThis.fetch
const originalLog = console.log
const rows = { product: [], faq: [] }
let writes = 0
let networkCalls = 0
let run = 0
let outputLines = []
let loaderChecks
try {
  console.log = value => outputLines.push(value)
  globalThis.fetch = async () => { throw new Error('Dry run must not call fetch') }
  process.argv = [originalArgs[0], 'load-current-knowledge.mjs']
  await import(`./load-current-knowledge.mjs?verification=${++run}`)
  const plan = JSON.parse(outputLines[0])
  assert.equal(plan.mode, 'DRY_RUN')
  assert.equal(plan.requests.length, 6)
  assert(plan.requests.every(r => r.method === 'POST' && /^\/api\/knowledge\/(product|faq)\/create$/.test(r.path)))
  globalThis.fetch = async (url, options) => {
    networkCalls++
    assert.equal(new URL(url).origin, 'http://localhost:10002')
    assert.equal(options.redirect, 'error')
    const match = /\/api\/knowledge\/(product|faq)\/(list|create)$/.exec(new URL(url).pathname)
    assert(match)
    const [, type, action] = match
    const body = JSON.parse(options.body)
    let result
    if (action === 'list') {
      result = { total: rows[type].length, records: rows[type].slice((body.page - 1) * body.size, body.page * body.size) }
    } else {
      writes++
      result = { ...body, id: writes, status: '草稿' }
      rows[type].push(result)
    }
    return { ok: true, status: 200, json: async () => ({ code: 200, data: result }) }
  }
  process.argv = [originalArgs[0], 'load-current-knowledge.mjs', '--apply']
  outputLines = []
  await import(`./load-current-knowledge.mjs?verification=${++run}`)
  assert.equal(writes, 6)
  assert.equal(rows.product.length, 4)
  assert.equal(rows.faq.length, 2)
  outputLines = []
  await import(`./load-current-knowledge.mjs?verification=${++run}`)
  assert.equal(writes, 6)
  assert.equal(outputLines.filter(line => String(line).includes('SKIP_EXISTING')).length, 6)
  rows.product[0].description = 'deliberately conflicting content'
  await assert.rejects(import(`./load-current-knowledge.mjs?verification=${++run}`), /Existing content differs/)
  assert.equal(writes, 6)
  rows.product[0].description = data.products[0].description
  rows.product.push(structuredClone(rows.product[0]))
  await assert.rejects(import(`./load-current-knowledge.mjs?verification=${++run}`), /Duplicate existing key/)
  assert.equal(writes, 6)
  globalThis.fetch = async () => { throw new Error('simulated server unavailable') }
  await assert.rejects(import(`./load-current-knowledge.mjs?verification=${++run}`), /simulated server unavailable/)
  assert.equal(writes, 6)
  loaderChecks = { status: 'PASS', mode: 'IN_MEMORY_FETCH_SUBSTITUTE_NO_REAL_NETWORK', dryRunRequests: 6, firstRunCreates: 6, secondRunCreates: 0, contentConflictCreates: 0, duplicateKeyCreates: 0, unavailableServerCreates: 0, simulatedHttpCalls: networkCalls }
} finally {
  process.argv = originalArgs
  globalThis.fetch = originalFetch
  console.log = originalLog
}

const report = {
  generatedAt: new Date().toISOString(), scope: 'OFFLINE_C_DELIVERABLE_CHECKS_NOT_BUSINESS_ACCEPTANCE',
  datasetSha256: createHash('sha256').update(raw).digest('hex'),
  dataset: { status: 'PASS', counts: Object.fromEntries(['merchants', 'shops', 'customers', 'products', 'orders', 'logistics', 'knowledge'].map(k => [k, data[k].length])) },
  negativeChecks, loaderChecks
}
if (process.argv[2]) {
  const output = resolve(process.argv[2])
  mkdirSync(dirname(output), { recursive: true })
  writeFileSync(output, JSON.stringify(report, null, 2) + '\n')
}
console.log(JSON.stringify(report, null, 2))
