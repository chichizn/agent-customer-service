import type {
  CommerceOrder,
  DemoCatalog,
  ExecutionRecord,
  KnowledgeReference,
  OrderLogistics,
  ServiceTicket,
  WorkbenchMessage,
  WorkbenchSession
} from '../../api/customer-service/model/workbenchModel'

export const MOCK_STORAGE_KEY = 'customer-service:b-workbench:v1'
export const MOCK_CHANGE_EVENT = 'customer-service:mock-change'

export interface MockConversation {
  session: WorkbenchSession
  messages: WorkbenchMessage[]
  executions: ExecutionRecord[]
  knowledge: KnowledgeReference[]
  processedRequests: string[]
  receptionVersion: number
  selectedOrderId?: string
  pendingQuery?: 'logistics' | 'return' | 'compensation'
  pendingReason?: string
  pendingTicket?: { orderId: string; type: 'RETURN' | 'COMPENSATION'; reason: string }
}

export interface MockWorkspace {
  version: 1
  generation: string
  catalog: DemoCatalog
  conversations: MockConversation[]
  orders: CommerceOrder[]
  logistics: OrderLogistics[]
  tickets: ServiceTicket[]
}

export function createMockWorkspace(): MockWorkspace {
  const catalog: DemoCatalog = {
    shops: [
      { shopId: 'shop-tb', platform: 'TAOBAO', name: '拾光生活 · 淘宝店' },
      { shopId: 'shop-dy', platform: 'DOUYIN', name: '拾光生活 · 抖店' }
    ],
    customers: [
      { customerId: 'customer-tb-1', shopId: 'shop-tb', name: '林小满' },
      { customerId: 'customer-tb-2', shopId: 'shop-tb', name: '陈一可' },
      { customerId: 'customer-dy-1', shopId: 'shop-dy', name: '周雨晴' },
      { customerId: 'customer-dy-2', shopId: 'shop-dy', name: '许知远' }
    ]
  }
  const orders: CommerceOrder[] = catalog.customers.flatMap((customer, customerIndex) =>
    Array.from(
      { length: 3 },
      (_, index): CommerceOrder => ({
        orderId: `${customer.shopId === 'shop-tb' ? 'TB' : 'DY'}202610${customerIndex + 1}0${index + 1}`,
        shopId: customer.shopId,
        customerId: customer.customerId,
        items: [
          {
            name:
              index === 2 ? '桌面收纳盒' : '拾光陶瓷杯 · ' + (index === 0 ? '奶油白' : '雾蓝色'),
            quantity: index === 2 ? 2 : 1
          }
        ],
        paidAmount: index === 2 ? 59.8 : 39.9,
        status:
          index === 0
            ? 'SHIPPED'
            : index === 1
              ? 'DELIVERED'
              : customerIndex % 2 === 0
                ? 'PENDING_SHIPMENT'
                : 'CANCELLED',
        createdAt: `2026-10-${14 + index}T09:20:00`
      })
    )
  )
  const logistics: OrderLogistics[] = orders.map((order) => ({
    orderId: order.orderId,
    carrier: ['SHIPPED', 'DELIVERED'].includes(order.status) ? '演示快递' : undefined,
    trackingNumber: ['SHIPPED', 'DELIVERED'].includes(order.status)
      ? `DEMO${order.orderId}`
      : undefined,
    status:
      order.status === 'SHIPPED'
        ? '运输中 · 暂无预计送达时间'
        : order.status === 'DELIVERED'
          ? '已签收'
          : '暂无物流',
    nodes: ['SHIPPED', 'DELIVERED'].includes(order.status)
      ? [
          { time: '2026-10-16T10:20:00', description: '快件已揽收' },
          { time: '2026-10-17T14:30:00', description: '已到达目的地转运中心' },
          ...(order.status === 'DELIVERED'
            ? [{ time: '2026-10-18T09:10:00', description: '顾客已签收' }]
            : [])
        ]
      : []
  }))
  const conversations: MockConversation[] = catalog.customers.slice(0, 3).map((customer, index) => {
    const shop = catalog.shops.find((item) => item.shopId === customer.shopId)!
    const sessionId = `demo-session-${index + 1}`
    const content =
      index === 1
        ? '顾客申请规则外补偿，等待商家核实。'
        : '您好，这里是前端本地模拟会话。可以查询订单，或打开顾客入口发送消息。'
    return {
      session: {
        sessionId,
        shopId: shop.shopId,
        shopName: shop.name,
        platform: shop.platform,
        customerId: customer.customerId,
        customerName: customer.name,
        receptionMode: index === 1 ? 'WAITING_HUMAN' : 'AI',
        status: 'active',
        lastMessage: content,
        updatedAt: `2026-10-18T10:${20 + index}:00`,
        handoffSummary:
          index === 1
            ? '【预置模拟摘要】顾客希望获得规则外补偿；尚未创建工单；请核实具体订单和诉求。'
            : undefined
      },
      messages: [
        {
          messageId: `welcome-${index}`,
          sessionId,
          role: 'system',
          content,
          createdAt: `2026-10-18T10:${20 + index}:00`
        }
      ],
      executions: [],
      knowledge: [],
      processedRequests: [],
      receptionVersion: 0
    }
  })
  return {
    version: 1,
    generation: crypto.randomUUID(),
    catalog,
    conversations,
    orders,
    logistics,
    tickets: []
  }
}

export function shopKnowledge(shopId: string): KnowledgeReference {
  return {
    knowledgeId: `policy-${shopId}`,
    title: '演示店铺售后与物流规则',
    source: shopId === 'shop-tb' ? '淘宝演示店铺规则' : '抖店演示店铺规则',
    scope: shopId === 'shop-tb' ? '仅淘宝店适用' : '仅抖店适用',
    content:
      shopId === 'shop-tb'
        ? '演示规则：签收后 7 天内可记录退货诉求，商品需保持完好。破损需提供说明，由商家审核；不承诺自动退款。'
        : '演示规则：签收后 15 天内可记录退货诉求，需说明原因并由商家核实。规则外补偿交给人工，不承诺自动退款。'
  }
}
