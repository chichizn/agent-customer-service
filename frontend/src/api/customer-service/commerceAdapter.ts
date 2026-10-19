import type {
  CommerceOrder,
  DemoCatalog,
  DemoBusinessTime,
  OrderLogistics,
  Platform,
  ServiceTicket,
  TicketHandlingRecord,
  TicketType
} from './model/workbenchModel'

export interface CommerceIdentities {
  merchantId: string
  merchantName: string
  businessTime?: DemoBusinessTime
  shops: {
    shopId: string
    name: string
    platform: Platform
    customers: { customerId: string; name: string }[]
  }[]
}

export interface CommerceOrderRecord {
  orderId: string
  orderNumber: string
  shopId: string
  customerId: string
  items: { productName: string; specification?: string; quantity: number }[]
  actualPaid: number
  status: CommerceOrder['status']
  orderedAt: string
}

export interface CommerceLogisticsRecord {
  orderId: string
  available: boolean
  unavailableReason?: string | null
  carrier?: string | null
  trackingNumber?: string | null
  status?: string | null
  nodes: { occurredAt: string; description: string }[]
}

export interface CommerceTicketRecord {
  ticketId: string
  ticketNumber: string
  orderNumber: string
  sessionId: string
  orderId: string
  shopId: string
  customerId: string
  ticketType: 'RETURN_REQUEST' | 'EXCEPTION_COMPENSATION'
  reason: string
  status: ServiceTicket['status']
  createdAt: string
  updatedAt: string
  history: TicketHandlingRecord[]
}

export function toDemoCatalog(identities: CommerceIdentities): DemoCatalog {
  return {
    merchantId: identities.merchantId,
    ...(identities.businessTime ? { businessTime: identities.businessTime } : {}),
    shops: identities.shops.map(({ shopId, name, platform }) => ({ shopId, name, platform })),
    customers: identities.shops.flatMap((shop) =>
      shop.customers.map((customer) => ({ ...customer, shopId: shop.shopId }))
    )
  }
}

export function toWorkbenchOrder(order: CommerceOrderRecord): CommerceOrder {
  return {
    orderId: order.orderId,
    orderNumber: order.orderNumber,
    shopId: order.shopId,
    customerId: order.customerId,
    items: order.items.map(({ productName, specification, quantity }) => ({
      name: specification ? `${productName} · ${specification}` : productName,
      quantity
    })),
    paidAmount: Number(order.actualPaid),
    status: order.status,
    createdAt: order.orderedAt
  }
}

export function toWorkbenchLogistics(logistics: CommerceLogisticsRecord): OrderLogistics {
  return {
    orderId: logistics.orderId,
    carrier: logistics.carrier || undefined,
    trackingNumber: logistics.trackingNumber || undefined,
    status: logistics.available
      ? logistics.status || '物流状态未知'
      : logistics.unavailableReason || '暂无可用物流信息',
    nodes: logistics.nodes.map(({ occurredAt, description }) => ({
      time: occurredAt,
      description
    }))
  }
}

export function toWorkbenchTicket(ticket: CommerceTicketRecord): ServiceTicket {
  const type: TicketType = ticket.ticketType === 'RETURN_REQUEST' ? 'RETURN' : 'COMPENSATION'
  return {
    ticketId: ticket.ticketId,
    ticketNumber: ticket.ticketNumber,
    orderNumber: ticket.orderNumber,
    sessionId: ticket.sessionId,
    orderId: ticket.orderId,
    shopId: ticket.shopId,
    customerId: ticket.customerId,
    type,
    reason: ticket.reason,
    status: ticket.status,
    processingNote:
      [...ticket.history].reverse().find((record) => record.fromStatus !== null)?.note || '',
    createdAt: ticket.createdAt,
    updatedAt: ticket.updatedAt,
    history: ticket.history
  }
}
