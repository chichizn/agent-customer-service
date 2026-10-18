import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { resolve } from 'node:path'

const defaultFile = fileURLToPath(new URL('../dataset.v1.json', import.meta.url))

export function validateDataset(data) {
  const errors = []
  const check = (ok, message) => { if (!ok) errors.push(message) }
  const groups = ['merchants', 'shops', 'customers', 'products', 'orders', 'logistics', 'knowledge']
  for (const key of groups) check(Array.isArray(data[key]), `${key} must be an array`)
  if (errors.length) return errors
  const maps = Object.fromEntries(groups.map(key => [key, new Map(data[key].map(row => [row.id, row]))]))
  for (const key of groups) {
    check(maps[key].size === data[key].length, `${key}: duplicate ID`)
    for (const row of data[key]) {
      check(typeof row.id === 'string' && row.id.startsWith('demo-'), `${key}: invalid demo ID`)
      check(row.demoNamespace === data.meta.demoNamespace, `${key}/${row.id}: invalid namespace`)
    }
  }
  check(data.meta?.schemaVersion === '1.0.0', 'unsupported schemaVersion')
  check(data.merchants.length === 1 && data.shops.length === 2, 'need one merchant and two shops')
  check(new Set(data.shops.map(s => s.platform)).size === 2 && data.shops.every(s => ['TAOBAO', 'DOUYIN'].includes(s.platform)), 'need Taobao and Douyin')
  check(data.products.length >= 3 && data.products.length <= 5, 'need 3–5 catalog products')
  check(data.orders.length === 12 && data.knowledge.length === 10, 'need 12 orders and 10 knowledge records')
  check(['sessions', 'messages', 'serviceTickets', 'executionRecords'].every(k => Array.isArray(data[k]) && data[k].length === 0), 'runtime records must start empty')
  const validTime = value => typeof value === 'string' && /\+08:00$/.test(value) && Number.isFinite(Date.parse(value))
  check(validTime(data.meta.referenceTime), 'referenceTime must have +08:00 offset')
  for (const shop of data.shops) {
    check(maps.merchants.has(shop.merchantId), `${shop.id}: unknown merchant`)
    check(data.customers.filter(c => c.shopId === shop.id).length >= 2, `${shop.id}: need at least two customers`)
    const orders = data.orders.filter(o => o.shopId === shop.id)
    check(orders.length === 6, `${shop.id}: need six orders`)
    check(['PENDING_SHIPMENT', 'SHIPPED', 'DELIVERED', 'CANCELLED'].every(s => orders.some(o => o.status === s)), `${shop.id}: incomplete order states`)
    check(data.customers.some(c => c.shopId === shop.id && !orders.some(o => o.customerId === c.id)), `${shop.id}: missing customer with no orders`)
  }
  for (const c of data.customers) check(maps.shops.get(c.shopId)?.merchantId === c.merchantId, `${c.id}: customer scope mismatch`)
  for (const p of data.products) check(Number.isSafeInteger(p.listPriceCents) && p.listPriceCents > 0, `${p.id}: invalid price`)
  check(new Set(data.products.map(p => p.productCode)).size === data.products.length, 'duplicate productCode')
  check(new Set(data.orders.map(o => o.orderNumber)).size === data.orders.length, 'duplicate orderNumber')
  for (const o of data.orders) {
    const shop = maps.shops.get(o.shopId)
    const customer = maps.customers.get(o.customerId)
    check(shop?.merchantId === o.merchantId && shop?.platform === o.platform && customer?.shopId === o.shopId && customer?.merchantId === o.merchantId, `${o.id}: order ownership mismatch`)
    check(['PENDING_SHIPMENT', 'SHIPPED', 'DELIVERED', 'CANCELLED'].includes(o.status), `${o.id}: invalid status`)
    check(validTime(o.orderedAt) && Date.parse(o.orderedAt) <= Date.parse(data.meta.referenceTime), `${o.id}: invalid orderedAt`)
    check(o.currency === 'CNY' && Array.isArray(o.items) && o.items.length > 0, `${o.id}: invalid order items/currency`)
    let subtotal = 0
    for (const item of o.items) {
      check(maps.products.get(item.productId)?.productName === item.productName, `${o.id}: product reference mismatch`)
      check(Number.isSafeInteger(item.quantity) && item.quantity > 0 && Number.isSafeInteger(item.unitPriceCents) && item.unitPriceCents > 0, `${o.id}: invalid quantity/price`)
      subtotal += item.unitPriceCents * item.quantity
    }
    check(Number.isSafeInteger(o.discountCents) && o.discountCents >= 0 && Number.isSafeInteger(o.paidAmountCents) && o.paidAmountCents > 0 && o.paidAmountCents === subtotal - o.discountCents, `${o.id}: amount mismatch`)
    const shipments = data.logistics.filter(l => l.orderId === o.id)
    check(shipments.length === (['SHIPPED', 'DELIVERED'].includes(o.status) ? 1 : 0), `${o.id}: wrong shipment count`)
  }
  for (const l of data.logistics) {
    const o = maps.orders.get(l.orderId)
    check(o && o.shopId === l.shopId && o.customerId === l.customerId && o.merchantId === l.merchantId, `${l.id}: logistics scope mismatch`)
    check((o?.status === 'DELIVERED' && l.status === 'DELIVERED') || (o?.status === 'SHIPPED' && ['IN_TRANSIT', 'DELAYED'].includes(l.status)), `${l.id}: shipment status mismatch`)
    check(l.trackingNumber.startsWith('DEMO-') && l.estimatedDeliveryAt === null, `${l.id}: tracking/ETA must be explicitly simulated`)
    check(l.nodes.length > 0, `${l.id}: missing tracking nodes`)
    let previous = Date.parse(o?.orderedAt)
    for (const n of l.nodes) {
      const current = Date.parse(n.occurredAt)
      check(validTime(n.occurredAt) && current >= previous && current <= Date.parse(data.meta.referenceTime), `${l.id}: tracking timeline out of order`)
      previous = current
    }
  }
  check(data.logistics.some(l => l.status === 'DELAYED'), 'missing delayed logistics case')
  check(data.customers.some(c => data.orders.filter(o => o.customerId === c.id && o.items.some(i => i.productId === 'demo-product-cup')).length >= 2), 'missing multiple cup orders')
  for (const k of data.knowledge) {
    check(maps.merchants.has(k.merchantId) && ['MERCHANT', 'SHOP'].includes(k.scope), `${k.id}: invalid knowledge scope`)
    check(k.scope === 'SHOP' ? maps.shops.get(k.shopId)?.merchantId === k.merchantId : k.shopId === null, `${k.id}: invalid shop scope`)
    check(k.title && k.content && k.source && k.status === 'ACTIVE', `${k.id}: incomplete knowledge`)
    check(k.productIds.every(id => maps.products.has(id)), `${k.id}: unknown product`)
  }
  const returns = data.knowledge.filter(k => k.topic === 'RETURN_WINDOW')
  check(returns.length === 2 && new Set(returns.map(k => k.shopId)).size === 2 && new Set(returns.map(k => k.policy.returnWindowDays)).size === 2, 'missing distinct shop return rules')
  return errors
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const data = JSON.parse(readFileSync(process.argv[2] || defaultFile, 'utf8'))
    const errors = validateDataset(data)
    console.log(JSON.stringify({ status: errors.length ? 'FAIL' : 'PASS', counts: Object.fromEntries(['merchants', 'shops', 'customers', 'products', 'orders', 'logistics', 'knowledge'].map(k => [k, data[k]?.length])), errors }, null, 2))
    if (errors.length) process.exitCode = 1
  } catch (error) { console.error(error.message); process.exitCode = 1 }
}
