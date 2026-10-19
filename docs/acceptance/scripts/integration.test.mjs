import assert from 'node:assert/strict'
import test from 'node:test'
import { seed, profile, knowledge, validateIntegration, validateKnowledge } from '../../../demo-data/scripts/check-integration.mjs'
import { runChecks } from './commerce-readonly.mjs'

test('current commerce seed supports C stories, including cancelled zero payment', () => {
  assert.equal(validateIntegration(seed).status, 'PASS')
})

test('AC-05 has a real customer with zero orders and rejects orders added to that identity', () => {
  assert.equal(profile.expectedCounts.customers, 5)
  assert.equal(validateIntegration(seed).status, 'PASS')
  assert.equal(seed.orders.some(order => order.customerId === profile.actors.TB3.customerId), false)
  const mutated = structuredClone(seed)
  const extraOrder = structuredClone(mutated.orders[0])
  extraOrder.id = 'demo-order-invalid-empty-customer'
  extraOrder.orderNumber = 'TB209901010099'
  extraOrder.customerId = profile.actors.TB3.customerId
  mutated.orders.push(extraOrder)
  const result = validateIntegration(mutated)
  assert.equal(result.status, 'FAIL')
  assert(result.errors.some(error => error.includes('emptyOrders: expected customer without orders')))
})

test('prepared knowledge uses current identities and both cup variants', () => {
  assert.equal(validateKnowledge(knowledge).status, 'PASS')
})

for (const [name, mutate] of [
  ['old merchant', data => { data.knowledge[0].merchantId = 'demo-merchant-01' }],
  ['global shop rule', data => { const row = data.knowledge.find(k => k.kind === 'SHOP_RULE'); row.scope = 'MERCHANT'; row.shopId = null }],
  ['old shop ID', data => { data.knowledge.find(k => k.scope === 'SHOP').shopId = 'demo-shop-taobao' }],
  ['unknown product', data => { data.knowledge[0].productIds = ['demo-product-bag'] }],
  ['missing 750ml variant', data => { data.knowledge[0].specifications.pop() }],
  ['expired activity', data => { data.knowledge.find(k => k.kind === 'ACTIVITY').policy.endsAt = '2026-09-30T00:00:00+08:00' }]
]) test(`reject knowledge regression: ${name}`, () => {
  const data = structuredClone(knowledge)
  mutate(data)
  assert.equal(validateKnowledge(data).status, 'FAIL')
})

for (const [name, mutate] of [
  ['cross-customer ownership', data => { data.orders[0].customerId = profile.actors.TB2.customerId }],
  ['payment over item subtotal', data => { data.orders[0].actualPaid = 5900 }],
  ['fractional cent payment', data => { data.orders[0].actualPaid = 58.999 }],
  ['reversed logistics timeline', data => { data.orders[0].logistics.nodes.reverse() }],
  ['invalid calendar date', data => { data.orders[0].orderedAt = '2026-02-31T09:00:00' }],
  ['duplicate order', data => { data.orders.push(structuredClone(data.orders[0])) }],
  ['missing items', data => { delete data.orders[0].items }]
]) test(`reject seed regression: ${name}`, () => {
  const data = structuredClone(seed)
  mutate(data)
  assert.equal(validateIntegration(data).status, 'FAIL')
})

test('reject C story drift to another customer or a non-delayed order', () => {
  const config = structuredClone(profile)
  config.scenarios.return.actor = 'DY2'
  config.scenarios.handoff = { actor: 'DY2', orderId: 'demo-order-dy-06' }
  const result = validateIntegration(seed, config)
  assert(result.errors.some(e => e.includes('scenario ownership')))
  assert(result.errors.some(e => e.includes('must be delayed')))
})

// In-memory transport tests this checker only; it is not backend acceptance evidence.
const ok = data => ({ httpStatus: 200, payload: { code: 0, data: structuredClone(data) } })
function fixtureTransport() {
  const calls = []
  const request = async (path, body) => {
    calls.push(path)
    if (path === '/demo/identities') return ok({ merchantId: profile.merchantId, shops: seed.shops.map(s => ({ shopId: s.id, platform: s.platform, customers: seed.customers.filter(c => c.shopId === s.id).map(c => ({ customerId: c.id, name: c.name, platformUserId: c.platformUserId })) })) })
    assert.equal(body.scope.merchantId, profile.merchantId)
    const owned = seed.orders.filter(o => o.shopId === body.scope.shopId && o.customerId === body.scope.customerId)
    if (path === '/tickets/query') return ok([])
    if (path === '/orders/query') {
      const orders = owned.filter(o => (!body.orderId || o.id === body.orderId) && (!body.orderNumber || o.orderNumber === body.orderNumber) && (!body.productClue || o.items.some(i => i.productName.includes(body.productClue))))
        .map(o => ({ ...o, orderId: o.id, merchantId: profile.merchantId, platform: seed.shops.find(s => s.id === o.shopId).platform, totalQuantity: o.items.reduce((sum, i) => sum + i.quantity, 0) }))
      return ok({ matchStatus: orders.length > 1 ? 'NEED_SELECTION' : orders.length ? 'MATCHED' : 'NOT_FOUND', orders })
    }
    if (path === '/logistics/query') {
      const order = owned.find(o => o.id === body.orderId)
      if (!order) return { httpStatus: 404, payload: { code: 404, data: { errorCode: 'NOT_FOUND' } } }
      return ok({ orderId: order.id, available: Boolean(order.logistics), ...(order.logistics || { carrier: null, trackingNumber: null, status: null, nodes: [], unavailableReason: '尚无物流' }) })
    }
    assert.fail(`Unexpected request: ${path}`)
  }
  return { request, calls }
}

test('checker covers all seed orders and uses only four query endpoints', async () => {
  const { request, calls } = fixtureTransport()
  const result = await runChecks(request)
  assert.equal(result.status, 'PASS', JSON.stringify(result.checks.filter(c => c.status !== 'PASS')))
  assert.equal(result.checks.length, 34)
  assert.deepEqual([...new Set(calls)].sort(), ['/demo/identities', '/logistics/query', '/orders/query', '/tickets/query'])
})

test('unavailable service is BLOCKED; business rejection is FAIL', async () => {
  assert.equal((await runChecks(async () => { throw new TypeError('fetch failed') })).status, 'BLOCKED')
  assert.equal((await runChecks(async () => ({ httpStatus: 503, payload: { code: 503 } }))).status, 'FAIL')
})

test('wrong merchant identity stops before querying customer resources', async () => {
  const { request, calls } = fixtureTransport()
  const result = await runChecks(async (path, body) => {
    const response = await request(path, body)
    response.payload.data.merchantId = 'another-merchant'
    return response
  })
  assert.equal(result.status, 'FAIL')
  assert.deepEqual(calls, ['/demo/identities'])
})

for (const name of ['foreign order leak', 'wrong item specification', 'zero cancellation amount changed', 'foreign ticket', 'fabricated logistics']) test(`checker detects ${name}`, async () => {
  const { request } = fixtureTransport()
  const result = await runChecks(async (path, body) => {
    const response = await request(path, body)
    const data = response.payload.data
    if (name === 'foreign order leak' && path === '/orders/query' && body.orderId === 'demo-order-dy-01') { data.matchStatus = 'MATCHED'; data.orders.push({ orderId: body.orderId }) }
    if (name === 'wrong item specification' && path === '/orders/query' && data.orders.length) data.orders[0].items[0].specification = '5000ml'
    if (name === 'zero cancellation amount changed' && path === '/orders/query') for (const order of data.orders) if (order.status === 'CANCELLED') order.actualPaid = 19
    if (name === 'foreign ticket' && path === '/tickets/query') data.push({ merchantId: 'another-merchant' })
    if (name === 'fabricated logistics' && path === '/logistics/query' && data.available === false) data.trackingNumber = 'FAKE-123'
    return response
  })
  assert.equal(result.status, 'FAIL')
})

test('equivalent Jackson wall-time serialization is accepted', async () => {
  const { request } = fixtureTransport()
  const result = await runChecks(async (path, body) => {
    const response = await request(path, body)
    const data = response.payload.data
    if (path === '/orders/query') for (const order of data.orders) order.orderedAt = order.orderedAt.replace('T', ' ')
    if (path === '/logistics/query' && data.nodes) for (const node of data.nodes) node.occurredAt = node.occurredAt.replace(/:00$/, '')
    return response
  })
  assert.equal(result.status, 'PASS')
})
