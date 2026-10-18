import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { validateDataset } from './validate.mjs'

// Only merchant-shared records are mapped to the existing API, which has no shop scope.
// The complete commerce fixture is consumed by the future A/D loader, not this script.
const args = process.argv.slice(2)
if (args.some(arg => arg !== '--apply')) throw new Error('Usage: node demo-data/scripts/load-current-knowledge.mjs [--apply]')
const apply = args.includes('--apply')
const data = JSON.parse(readFileSync(fileURLToPath(new URL('../dataset.v1.json', import.meta.url)), 'utf8'))
const errors = validateDataset(data)
if (errors.length) throw new Error(errors.join('\n'))
const baseUrl = 'http://localhost:10002'
const products = data.products.map(p => ({
  key: p.productCode, type: 'product', matchField: 'productCode', payload: {
    productName: p.productName, productCode: p.productCode, category: p.category,
    brand: p.brand, price: p.listPriceCents / 100, description: p.description,
    specifications: p.specifications, features: p.features, tags: ['C-DEMO-V1', '演示商品']
  }
}))
const faqs = data.knowledge.filter(k => k.scope === 'MERCHANT' && k.kind === 'FAQ').map(k => ({
  key: `[C-DEMO-V1] ${k.title}`, type: 'faq', matchField: 'question', payload: {
    question: `[C-DEMO-V1] ${k.title}`, answer: k.content,
    category: '通用', relatedQuestions: k.topic === 'TICKET_SCOPE' ? ['申请成功了吗', '退款成功了吗'] : ['什么时候到货', '明天能到吗'],
    tags: ['C-DEMO-V1'], priority: 5
  }
}))
const plan = [...products, ...faqs]
if (!apply) {
  console.log(JSON.stringify({ mode: 'DRY_RUN', baseUrl, note: 'No network requests or database writes. Shop rules/activities and commerce objects excluded.', requests: plan.map(p => ({ key: p.key, method: 'POST', path: `/api/knowledge/${p.type}/create`, body: p.payload })) }, null, 2))
} else {
  const post = async (path, body) => {
    const response = await fetch(baseUrl + path, {
      method: 'POST', redirect: 'error', signal: AbortSignal.timeout(15000),
      headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
    })
    if (!response.ok) throw new Error(`${path}: HTTP ${response.status}`)
    const payload = await response.json()
    if (![0, 200].includes(payload.code)) throw new Error(`${path}: business error ${payload.code}`)
    return payload.data
  }
  const listAll = async type => {
    const all = []
    for (let page = 1; page <= 1000; page++) {
      const result = await post(`/api/knowledge/${type}/list`, { page, size: 100 })
      if (!Array.isArray(result?.records) || !Number.isSafeInteger(result.total) || result.total < 0) throw new Error(`Invalid ${type} list response`)
      all.push(...result.records)
      if (all.length >= result.total) return all
      if (!result.records.length) throw new Error(`Incomplete ${type} pagination`)
    }
    throw new Error('Pagination limit exceeded')
  }
  const existing = { product: await listAll('product'), faq: await listAll('faq') }
  // Precheck every collision before creating anything; never overwrite existing records.
  for (const item of plan) {
    const matches = existing[item.type].filter(row => row[item.matchField] === item.key)
    if (matches.length > 1) throw new Error(`Duplicate existing key ${item.key}; resolve manually`)
    item.existing = matches[0]
    if (item.existing) {
      for (const [key, value] of Object.entries(item.payload)) {
        if (['tags', 'relatedQuestions'].includes(key)) continue
        if (item.existing[key] !== value) throw new Error(`Existing content differs: ${item.key}/${key}; no overwrite performed`)
      }
    }
  }
  for (const item of plan) {
    if (item.existing) {
      console.log(JSON.stringify({ action: 'SKIP_EXISTING', key: item.key, id: item.existing.id }))
      continue
    }
    const created = await post(`/api/knowledge/${item.type}/create`, item.payload)
    if (created?.id === undefined || created?.id === null) throw new Error(`Create returned no ID: ${item.key}`)
    console.log(JSON.stringify({ action: 'CREATED', key: item.key, id: created.id, status: created.status }))
  }
  console.log('DONE: shared baseline knowledge only; no commerce data or shop rules loaded.')
}
