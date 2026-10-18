import type {
  OrderStatus,
  Platform,
  ReceptionMode,
  TicketStatus
} from '@/api/customer-service/model/workbenchModel'

export const platformLabels: Record<Platform, string> = { TAOBAO: '淘宝', DOUYIN: '抖店' }
export const modeLabels: Record<ReceptionMode, string> = {
  AI: 'AI 接待',
  WAITING_HUMAN: '等待人工',
  HUMAN: '人工接待'
}
export const orderLabels: Record<OrderStatus, string> = {
  PENDING_SHIPMENT: '待发货',
  SHIPPED: '运输中',
  DELIVERED: '已签收',
  CANCELLED: '已取消'
}
export const ticketLabels: Record<TicketStatus, string> = {
  PENDING_REVIEW: '待审核',
  PROCESSING: '处理中',
  COMPLETED: '已完成',
  REJECTED: '已拒绝'
}
export const ticketTransitions: Record<TicketStatus, TicketStatus[]> = {
  PENDING_REVIEW: ['PROCESSING', 'REJECTED'],
  PROCESSING: ['COMPLETED'],
  COMPLETED: [],
  REJECTED: []
}
export function formatTime(value?: string) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      })
}
