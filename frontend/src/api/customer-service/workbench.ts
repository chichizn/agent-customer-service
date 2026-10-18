import type { WorkbenchApi } from './model/workbenchModel'
import { createMockWorkbenchApi } from '@/mock/customer-service/workbench'

export const isWorkbenchMock = import.meta.env.VITE_CUSTOMER_SERVICE_MODE !== 'api'

// 真实接口按契约接入，保持使用项目的请求封装；接口错误原样交给页面呈现。
async function post<T>(url: string, data: unknown = {}, signal?: AbortSignal): Promise<T> {
  const { default: request } = await import('@/utils/http')
  return request.post<T>({ url, data, signal, timeout: 60000, showErrorMessage: false })
}

const remoteApi: WorkbenchApi = {
  fetchCatalog: (signal) => post('/api/demo/catalog', {}, signal),
  fetchSessions: (signal) => post('/api/chat/session/list', {}, signal),
  fetchSession: (sessionId, signal) => post('/api/chat/session/snapshot', { sessionId }, signal),
  openSession: (params) => post('/api/chat/session/open', params),
  sendCustomerMessage: (params) =>
    post('/api/chat/message/send', { ...params, messageType: 'text' }),
  sendHumanMessage: (params) => post('/api/chat/message/human/send', params),
  setReceptionMode: (sessionId, receptionMode) =>
    post('/api/chat/session/reception/update', { sessionId, receptionMode }),
  updateTicket: (params) => post('/api/commerce/ticket/update', params),
  resetDemo: () => post('/api/demo/reset')
}

export const workbenchApi: WorkbenchApi = isWorkbenchMock ? createMockWorkbenchApi() : remoteApi
