import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { profile, seed, root, validateIntegration, writeReport } from '../../../demo-data/scripts/check-integration.mjs'

// Only query endpoints are allowed. No demo/load, restore, ticket write or model call.
const baseUrl = 'http://localhost:10002'
const allowed = new Set(['/demo/identities', '/orders/query', '/logistics/query', '/tickets/query'])
const networkError = error => error instanceof TypeError && error.message === 'fetch failed' || ['TimeoutError', 'AbortError'].includes(error.name)
// Jackson may emit LocalDateTime with T or a space; compare the same wall time.
const wallTime = value => typeof value === 'string' ? value.replace(' ', 'T').replace(/T(\d{2}:\d{2})$/, 'T$1:00') : value
async function localRequest(path, body) {
  if (!allowed.has(path)) throw new Error('Read-only endpoint required')
  const response = await fetch(baseUrl + '/api/commerce' + path, { method: 'POST', redirect: 'error', signal: AbortSignal.timeout(5000), headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
  return { httpStatus: response.status, payload: await response.json() }
}

export async function runChecks(request = localRequest) {
  assert.equal(validateIntegration(seed).status, 'PASS', 'Fix seed/scenario drift first')
  const checks = []
  const success = async (path, body) => {
    const response = await request(path, body)
    assert.equal(response.httpStatus, 200, `HTTP ${response.httpStatus}`)
    assert([0, 200].includes(response.payload?.code), 'Business response must succeed')
    return response.payload.data
  }
  const check = async (name, work) => {
    try { checks.push({ name, status: 'PASS', evidence: await work() }) }
    catch (error) { checks.push({ name, status: networkError(error) ? 'BLOCKED' : 'FAIL', reason: error.cause?.code || error.message }) }
  }
  let identities
  try { identities = await success('/demo/identities', {}) }
  catch (error) {
    const network = networkError(error)
    return { status: network ? 'BLOCKED' : 'FAIL', checks: [{ name: 'demo-identities', status: network ? 'BLOCKED' : 'FAIL', reason: error.cause?.code || error.message }] }
  }
  await check('demo-identities', async () => {
    assert.equal(identities.merchantId, profile.merchantId)
    assert.equal(identities.shops.length, seed.shops.length)
    for (const shop of seed.shops) {
      const live = identities.shops.find(s => s.shopId === shop.id)
      assert.equal(live?.platform, shop.platform)
      assert.deepEqual(live.customers.map(c => c.customerId).sort(), seed.customers.filter(c => c.shopId === shop.id).map(c => c.id).sort())
      for (const expected of seed.customers.filter(c => c.shopId === shop.id)) {
        const customer = live.customers.find(c => c.customerId === expected.id)
        assert.equal(customer.name, expected.name)
        assert.equal(customer.platformUserId, expected.platformUserId)
      }
    }
    return identities
  })
  if (checks[0].status !== 'PASS') return { status: 'FAIL', checks }
  for (const [name, actor] of Object.entries(profile.actors)) {
    const scope = { merchantId: profile.merchantId, ...actor }
    const ownOrders = seed.orders.filter(o => o.customerId === actor.customerId && o.shopId === actor.shopId)
    await check(`${name}: order scope and amounts`, async () => {
      const result = await success('/orders/query', { scope })
      assert.equal(result.matchStatus, ownOrders.length === 0 ? 'NOT_FOUND' : ownOrders.length === 1 ? 'MATCHED' : 'NEED_SELECTION')
      assert.deepEqual(result.orders.map(o => o.orderId).sort(), ownOrders.map(o => o.id).sort())
      for (const expected of ownOrders) {
        const live = result.orders.find(o => o.orderId === expected.id)
        for (const key of ['shopId', 'customerId', 'orderNumber', 'status', 'actualPaid']) assert.equal(live[key], expected[key], `${expected.id}/${key}`)
        assert.equal(wallTime(live.orderedAt), expected.orderedAt, `${expected.id}/orderedAt`)
        assert.equal(live.merchantId, profile.merchantId)
        assert.equal(live.platform, seed.shops.find(s => s.id === expected.shopId).platform)
        assert.equal(live.totalQuantity, expected.items.reduce((sum, item) => sum + item.quantity, 0))
        assert.deepEqual(live.items.map(({ productId, productName, specification, quantity, unitPrice }) => ({ productId, productName, specification, quantity, unitPrice })), expected.items)
      }
      return result
    })
    await check(`${name}: ticket ownership`, async () => {
      const tickets = await success('/tickets/query', { scope })
      assert(Array.isArray(tickets))
      for (const ticket of tickets) {
        assert.equal(ticket.merchantId, scope.merchantId)
        assert.equal(ticket.shopId, scope.shopId)
        assert.equal(ticket.customerId, scope.customerId)
        assert(ownOrders.some(o => o.id === ticket.orderId), 'Ticket must reference an owned order')
        assert.equal(ticket.platformAfterSaleStatus, 'NOT_SUBMITTED_TO_PLATFORM')
      }
      return tickets
    })
    for (const order of ownOrders) await check(`${order.id}: logistics`, async () => {
      const result = await success('/logistics/query', { scope, orderId: order.id })
      assert.equal(result.orderId, order.id)
      assert.equal(result.available, Boolean(order.logistics))
      if (order.logistics) {
        for (const key of ['carrier', 'trackingNumber', 'status']) assert.equal(result[key], order.logistics[key])
        assert.deepEqual(result.nodes.map(n => ({ occurredAt: wallTime(n.occurredAt), description: n.description })), order.logistics.nodes)
      } else {
        assert.equal(result.trackingNumber, null)
        assert.equal(result.carrier, null)
        assert.equal(result.status, null)
        assert.equal(result.nodes.length, 0)
        assert(result.unavailableReason)
      }
      return result
    })
  }
  await check('emptyOrders: customer has no visible orders', async () => {
    const actor = profile.actors[profile.scenarios.emptyOrders.actor]
    const result = await success('/orders/query', { scope: { merchantId: profile.merchantId, ...actor } })
    assert.equal(result.matchStatus, 'NOT_FOUND')
    assert.deepEqual(result.orders, [])
    return result
  })
  for (const name of ['multiOrder', 'singleOrder']) await check(`${name}: product matching`, async () => {
    const scenario = profile.scenarios[name]
    const result = await success('/orders/query', { scope: { merchantId: profile.merchantId, ...profile.actors[scenario.actor] }, productClue: scenario.productClue })
    assert.equal(result.matchStatus, scenario.orderIds.length > 1 ? 'NEED_SELECTION' : 'MATCHED')
    assert.deepEqual(result.orders.map(o => o.orderId).sort(), [...scenario.orderIds].sort())
    return result
  })
  for (const name of ['multiOrder', 'return']) await check(`${name}: exact order number`, async () => {
    const scenario = profile.scenarios[name]
    const order = seed.orders.find(o => o.id === (scenario.selectedOrderId || scenario.orderId))
    const result = await success('/orders/query', { scope: { merchantId: profile.merchantId, ...profile.actors[scenario.actor] }, orderNumber: order.orderNumber })
    assert.equal(result.matchStatus, 'MATCHED')
    assert.deepEqual(result.orders.map(o => o.orderId), [order.id])
    return result
  })
  const scope = { merchantId: profile.merchantId, ...profile.actors.TB1 }
  for (const [name, id] of [['foreign-shop', 'demo-order-dy-01'], ['foreign-customer', 'demo-order-tb-05'], ['missing', 'demo-not-found']]) {
    await check(`${name}: order invisible`, async () => {
      const result = await success('/orders/query', { scope, orderId: id })
      assert.equal(result.matchStatus, 'NOT_FOUND'); assert.deepEqual(result.orders, [])
      return result
    })
    await check(`${name}: logistics inaccessible`, async () => {
      const response = await request('/logistics/query', { scope, orderId: id })
      assert.equal(response.httpStatus, 404)
      assert.equal(response.payload?.code, 404)
      assert.equal(response.payload?.data?.errorCode, 'NOT_FOUND')
      assert.equal(response.payload?.data?.trackingNumber, undefined)
      return response
    })
  }
  return { status: checks.some(c => c.status === 'FAIL') ? 'FAIL' : checks.some(c => c.status === 'BLOCKED') ? 'BLOCKED' : 'PASS', checks }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const revision = spawnSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8', windowsHide: true, timeout: 3000 }).stdout?.trim()
  const report = { generatedAt: new Date().toISOString(), revision, scope: 'READ_ONLY_COMMERCE_HTTP_NOT_CHAT_OR_FULL_ACCEPTANCE', baseUrl, ...(await runChecks()) }
  writeReport(report, process.argv[2])
  if (report.status !== 'PASS') process.exitCode = report.status === 'BLOCKED' ? 2 : 1
}
