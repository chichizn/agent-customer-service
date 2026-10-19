import type { WorkbenchApi } from './model/workbenchModel'
import { createMockWorkbenchApi } from '@/mock/customer-service/workbench'
import {
  toDemoCatalog,
  toWorkbenchLogistics,
  toWorkbenchOrder,
  toWorkbenchTicket,
  type CommerceIdentities,
  type CommerceLogisticsRecord,
  type CommerceOrderRecord,
  type CommerceTicketRecord
} from './commerceAdapter'

export const isWorkbenchMock = import.meta.env.VITE_CUSTOMER_SERVICE_MODE !== 'api'

// 真实接口按契约接入，保持使用项目的请求封装；接口错误原样交给页面呈现。
async function post<T>(url: string, data: unknown = {}, signal?: AbortSignal): Promise<T> {
  const { default: request } = await import('@/utils/http')
  return request.post<T>({ url, data, signal, timeout: 60000, showErrorMessage: false })
}

const remoteApi: WorkbenchApi = {
  fetchCatalog: async (signal) => toDemoCatalog(await loadIdentities(signal)),
  fetchSessions: (signal) => post('/api/chat/session/list', { workbench: true }, signal),
  fetchSession: (sessionId, signal) => loadSession(sessionId, signal),
  openSession: async (params) => hydrateSnapshot(await post('/api/chat/session/open', params)),
  sendCustomerMessage: async (params) =>
    hydrateSnapshot(await post('/api/chat/message/send', { ...params, messageType: 'text' })),
  sendHumanMessage: async (params) =>
    hydrateSnapshot(await post('/api/chat/message/human/send', params)),
  setReceptionMode: async (sessionId, receptionMode) =>
    hydrateSnapshot(await post('/api/chat/session/reception/update', { sessionId, receptionMode })),
  updateTicket: async (params) => {
    if (!params.merchantId || !params.shopId || !params.expectedStatus)
      throw new Error('缺少工单商家、店铺或预期状态，无法安全更新。')
    await post('/api/commerce/tickets/update', {
      merchantId: params.merchantId,
      shopId: params.shopId,
      ticketId: params.ticketId,
      expectedStatus: params.expectedStatus,
      status: params.status,
      note: params.processingNote,
      operatorId: 'demo-operator'
    })
    return loadSession(params.sessionId)
  },
  resetDemo: async () => {
    await post('/api/demo/reset', { confirmed: true })
    identityCache = undefined
  }
}

let identityCache: { value: CommerceIdentities; expiresAt: number } | undefined
let identityRequest: Promise<CommerceIdentities> | undefined

function loadIdentities(signal?: AbortSignal): Promise<CommerceIdentities> {
  if (signal?.aborted) return Promise.reject(new DOMException('请求已取消。', 'AbortError'))
  if (identityCache && identityCache.expiresAt > Date.now())
    return Promise.resolve(identityCache.value)
  if (!identityRequest) {
    identityRequest = post<CommerceIdentities>('/api/commerce/demo/identities')
      .then((value) => {
        identityCache = { value, expiresAt: Date.now() + 30_000 }
        return value
      })
      .finally(() => {
        identityRequest = undefined
      })
  }
  return identityRequest.then((value) => {
    if (signal?.aborted) throw new DOMException('请求已取消。', 'AbortError')
    return value
  })
}

async function loadSession(sessionId: string, signal?: AbortSignal) {
  const [snapshot, identities] = await Promise.all([
    post<Awaited<ReturnType<WorkbenchApi['fetchSession']>>>(
      '/api/chat/session/snapshot',
      { sessionId },
      signal
    ),
    loadIdentities(signal)
  ])
  return hydrateCommerceSnapshot(snapshot, toDemoCatalog(identities), signal)
}

async function hydrateSnapshot(snapshot: Awaited<ReturnType<WorkbenchApi['fetchSession']>>) {
  return hydrateCommerceSnapshot(snapshot, toDemoCatalog(await loadIdentities()))
}

async function hydrateCommerceSnapshot(
  snapshot: Awaited<ReturnType<WorkbenchApi['fetchSession']>>,
  catalog: ReturnType<typeof toDemoCatalog>,
  signal?: AbortSignal
) {
  const session = snapshot.session
  const shop = catalog.shops.find((item) => item.shopId === session.shopId)
  const customer = catalog.customers.find(
    (item) => item.customerId === session.customerId && item.shopId === session.shopId
  )
  if (!shop || !customer)
    throw new Error('会话店铺或顾客不在 commerce 演示身份范围内，已停止读取业务资料。')
  const scope = {
    merchantId: catalog.merchantId,
    shopId: session.shopId,
    customerId: session.customerId
  }
  const [orderMatches, tickets] = await Promise.all([
    post<{ orders: CommerceOrderRecord[] }>('/api/commerce/orders/query', { scope }, signal),
    post<CommerceTicketRecord[]>('/api/commerce/tickets/query', { scope }, signal)
  ])
  const orderRecords = orderMatches.orders || []
  const logistics = await Promise.all(
    orderRecords.map((order) =>
      post<CommerceLogisticsRecord>(
        '/api/commerce/logistics/query',
        { scope, orderId: order.orderId },
        signal
      )
    )
  )
  return {
    ...snapshot,
    session: {
      ...session,
      shopName: shop.name,
      platform: shop.platform,
      customerName: customer.name
    },
    orders: orderRecords.map(toWorkbenchOrder),
    logistics: logistics.map(toWorkbenchLogistics),
    tickets: tickets.map(toWorkbenchTicket)
  }
}

export const workbenchApi: WorkbenchApi = isWorkbenchMock ? createMockWorkbenchApi() : remoteApi
