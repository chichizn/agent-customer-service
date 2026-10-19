import { readFileSync, mkdirSync, writeFileSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

export const root = fileURLToPath(new URL('../../', import.meta.url))
export const profile = JSON.parse(readFileSync(new URL('../integration.v1.json', import.meta.url), 'utf8'))
export const seedRaw = readFileSync(resolve(root, profile.seedPath))
export const seed = JSON.parse(seedRaw)
export const knowledgeRaw = readFileSync(resolve(root, profile.knowledgePath))
export const knowledge = JSON.parse(knowledgeRaw)

export function validateKnowledge(data, commerce = seed) {
  const errors = []
  const check = (ok, message) => { if (!ok) errors.push(message) }
  const rows = data?.knowledge
  if (!Array.isArray(rows) || rows.some(row => !row)) return { status: 'FAIL', count: 0, errors: ['knowledge must be an array of objects'] }
  const products = new Set(commerce.orders.flatMap(o => o.items.map(i => i.productId)))
  const shopIds = new Set(commerce.shops.map(s => s.id))
  check(data.merchantId === profile.merchantId && data.referenceTime === profile.referenceTime, 'knowledge merchant/reference time mismatch')
  check(rows.length === profile.expectedKnowledgeCount && new Set(rows.map(k => k.id)).size === rows.length, 'knowledge count/duplicate ID')
  for (const row of rows) {
    check(row.merchantId === profile.merchantId, `${row.id}: knowledge merchant mismatch`)
    check(row.scope === 'MERCHANT' ? row.shopId === null : row.scope === 'SHOP' && shopIds.has(row.shopId), `${row.id}: knowledge scope mismatch`)
    check(Array.isArray(row.productIds) && row.productIds.every(id => products.has(id)), `${row.id}: unknown product`)
    check(row.id && row.title && row.content && row.source && row.status === 'ACTIVE', `${row.id}: incomplete knowledge`)
    if (['SHOP_RULE', 'ACTIVITY'].includes(row.kind)) check(row.scope === 'SHOP', `${row.id}: shop policy must not be global`)
    if (row.kind === 'ACTIVITY') {
      const p = row.policy || {}
      const start = Date.parse(p.startsAt), end = Date.parse(p.endsAt), reference = Date.parse(data.referenceTime)
      check(p.discountRate > 0 && p.discountRate < 1 && start <= reference && reference <= end, `${row.id}: activity validity mismatch`)
    }
  }
  const specs = [...new Set(commerce.orders.flatMap(o => o.items.filter(i => i.productId === 'demo-product-cup').map(i => i.specification)))].sort()
  const cup = rows.find(k => k.id === 'demo-knowledge-cup')
  check(JSON.stringify([...(cup?.specifications || [])].sort()) === JSON.stringify(specs), 'cup specifications mismatch')
  for (const [shopId, days] of [['demo-shop-tb', 15], ['demo-shop-dy', 7]]) {
    check(rows.some(k => k.shopId === shopId && k.topic === 'RETURN_WINDOW' && k.policy?.returnWindowDays === days), `${shopId}: missing return policy`)
    check(rows.some(k => k.shopId === shopId && k.topic === 'RETURN_SHIPPING'), `${shopId}: missing shipping policy`)
    check(rows.some(k => k.shopId === shopId && k.kind === 'ACTIVITY'), `${shopId}: missing activity`)
  }
  return { status: errors.length ? 'FAIL' : 'PASS', count: rows.length, errors }
}

export function validateIntegration(data, config = profile) {
  const errors = []
  const check = (ok, message) => { if (!ok) errors.push(message) }
  if (!['shops', 'customers', 'orders'].every(key => Array.isArray(data?.[key]) && data[key].every(row => row && typeof row === 'object'))) {
    return { status: 'FAIL', counts: {}, errors: ['shops, customers and orders must be arrays of objects'] }
  }
  const shops = new Map(data.shops.map(s => [s.id, s]))
  const customers = new Map(data.customers.map(c => [c.id, c]))
  const orders = new Map(data.orders.map(o => [o.id, o]))
  check(shops.size === data.shops.length && customers.size === data.customers.length && orders.size === data.orders.length, 'duplicate ID')
  check(new Set(data.orders.map(o => o.orderNumber)).size === orders.size, 'duplicate order number')
  check(new Set(data.shops.map(s => s.platform)).size === 2 && data.shops.every(s => ['TAOBAO', 'DOUYIN'].includes(s.platform)), 'platform coverage')
  const cents = value => Math.round(value * 100)
  const validTime = value => typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(value) && Number.isFinite(Date.parse(value + 'Z')) && new Date(value + 'Z').toISOString().slice(0, 19) === value
  const validMoney = value => Number.isFinite(value) && Math.abs(value * 100 - cents(value)) < 1e-6
  const tracking = new Set()
  for (const customer of data.customers) check(shops.has(customer.shopId), `${customer.id}: unknown shop`)
  for (const order of data.orders) {
    check(customers.get(order.customerId)?.shopId === order.shopId && shops.has(order.shopId), `${order.id}: ownership mismatch`)
    check(['PENDING_SHIPMENT', 'SHIPPED', 'DELIVERED', 'CANCELLED'].includes(order.status), `${order.id}: invalid status`)
    check(validTime(order.orderedAt), `${order.id}: invalid orderedAt`)
    if (!Array.isArray(order.items) || !order.items.length || order.items.some(i => !i)) {
      errors.push(`${order.id}: invalid items`)
      continue
    }
    check(order.items.every(i => i.productId && i.productName && i.specification && Number.isSafeInteger(i.quantity) && i.quantity > 0 && validMoney(i.unitPrice) && i.unitPrice > 0), `${order.id}: invalid items`)
    const subtotal = order.items.reduce((sum, i) => sum + cents(i.unitPrice) * i.quantity, 0)
    check(validMoney(order.actualPaid) && order.actualPaid >= 0 && cents(order.actualPaid) <= subtotal, `${order.id}: invalid amount`)
    check(Boolean(order.logistics) === ['SHIPPED', 'DELIVERED'].includes(order.status), `${order.id}: logistics/status mismatch`)
    if (order.logistics) {
      check(typeof order.logistics.trackingNumber === 'string' && order.logistics.trackingNumber.startsWith('DEMO-') && !tracking.has(order.logistics.trackingNumber) && order.logistics.carrier && order.logistics.status, `${order.id}: invalid tracking`)
      tracking.add(order.logistics.trackingNumber)
      if (!Array.isArray(order.logistics.nodes) || !order.logistics.nodes.length || order.logistics.nodes.some(n => !n)) {
        errors.push(`${order.id}: invalid logistics nodes`)
        continue
      }
      let previous = order.orderedAt
      for (const node of order.logistics.nodes) {
        check(node.description && validTime(node.occurredAt) && node.occurredAt >= previous, `${order.id}: invalid logistics timeline`)
        previous = node.occurredAt
      }
    }
  }
  const counts = { shops: shops.size, customers: customers.size, products: new Set(data.orders.flatMap(o => Array.isArray(o.items) ? o.items.filter(Boolean).map(i => i.productId) : [])).size, orders: orders.size, logistics: data.orders.filter(o => o.logistics).length }
  for (const [key, expected] of Object.entries(config.expectedCounts)) check(counts[key] === expected, `${key}: expected ${expected}, got ${counts[key]}`)
  for (const shop of data.shops) check(data.orders.filter(o => o.shopId === shop.id).length === 6, `${shop.id}: expected six orders`)
  for (const [key, actor] of Object.entries(config.actors)) check(customers.get(actor.customerId)?.shopId === actor.shopId, `${key}: invalid actor`)
  for (const [key, scenario] of Object.entries(config.scenarios)) {
    const actor = config.actors[scenario.actor]
    const ids = scenario.orderIds || [scenario.orderId]
    check(ids.every(id => orders.get(id)?.customerId === actor?.customerId && orders.get(id)?.shopId === actor?.shopId), `${key}: scenario ownership mismatch`)
    if (scenario.productClue) {
      const matches = data.orders.filter(o => o.customerId === actor?.customerId && o.shopId === actor?.shopId && Array.isArray(o.items) && o.items.some(i => i && `${i.productName} ${i.specification}`.includes(scenario.productClue)))
      check(JSON.stringify(matches.map(o => o.id).sort()) === JSON.stringify([...ids].sort()), `${key}: unexpected matching orders`)
    }
    if (scenario.selectedOrderId) check(ids.includes(scenario.selectedOrderId), `${key}: selection outside candidates`)
  }
  check(orders.get(config.scenarios.return.orderId)?.status === 'DELIVERED', 'return scenario must be delivered')
  check(orders.get(config.scenarios.handoff.orderId)?.logistics?.status?.includes('延迟'), 'handoff scenario must be delayed')
  check(orders.get(config.scenarios.pendingShipment.orderId)?.status === 'PENDING_SHIPMENT', 'pending-shipment scenario mismatch')
  check(orders.get(config.scenarios.cancelled.orderId)?.status === 'CANCELLED', 'cancelled scenario mismatch')
  return { status: errors.length ? 'FAIL' : 'PASS', counts, errors }
}

export function writeReport(report, output) {
  if (output) { const path = resolve(output); mkdirSync(dirname(path), { recursive: true }); writeFileSync(path, JSON.stringify(report, null, 2) + '\n') }
  console.log(JSON.stringify(report, null, 2))
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const result = validateIntegration(seed)
  const preparedKnowledge = validateKnowledge(knowledge)
  const status = result.status === 'PASS' && preparedKnowledge.status === 'PASS' ? 'PASS' : 'FAIL'
  writeReport({ generatedAt: new Date().toISOString(), scope: 'OFFLINE_COMMERCE_SEED_AND_C_SCENARIOS_NOT_BUSINESS_ACCEPTANCE', seedSha256: createHash('sha256').update(seedRaw).digest('hex'), knowledgeSha256: createHash('sha256').update(knowledgeRaw).digest('hex'), ...result, status, preparedKnowledge, remainingGaps: profile.gaps }, process.argv[2])
  if (status !== 'PASS') process.exitCode = 1
}
