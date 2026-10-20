export type Platform = 'TAOBAO' | 'DOUYIN'
export type ReceptionMode = 'AI' | 'WAITING_HUMAN' | 'HUMAN'
export type MessageRole = 'customer' | 'ai' | 'human' | 'system'
export type OrderStatus = 'PENDING_SHIPMENT' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
export type TicketStatus = 'PENDING_REVIEW' | 'PROCESSING' | 'COMPLETED' | 'REJECTED'
export type TicketType = 'RETURN' | 'COMPENSATION'

export interface DemoShop {
  shopId: string
  name: string
  platform: Platform
}

export interface DemoCustomer {
  customerId: string
  shopId: string
  name: string
}

export interface DemoBusinessTime {
  mode: 'fixed' | 'system'
  effectiveAt: string
  timeZone: string
}

export interface DemoCatalog {
  merchantId: string
  businessTime?: DemoBusinessTime
  shops: DemoShop[]
  customers: DemoCustomer[]
}

export interface WorkbenchSession {
  sessionId: string
  shopId: string
  shopName: string
  platform: Platform
  customerId: string
  customerName: string
  receptionMode: ReceptionMode
  status: 'active' | 'closed'
  lastMessage: string
  updatedAt: string
  handoffSummary?: string
}

export interface WorkbenchMessage {
  messageId: string
  sessionId: string
  clientMessageId?: string
  role: MessageRole
  content: string
  createdAt: string
  orderIds?: string[]
  ticketId?: string
}

export interface OrderItem {
  name: string
  quantity: number
}

export interface CommerceOrder {
  orderId: string
  orderNumber?: string
  shopId: string
  customerId: string
  items: OrderItem[]
  paidAmount: number
  status: OrderStatus
  createdAt: string
}

export interface OrderLogistics {
  orderId: string
  carrier?: string
  trackingNumber?: string
  status: string
  nodes: { time: string; description: string }[]
}

export interface ServiceTicket {
  ticketId: string
  ticketNumber?: string
  orderNumber?: string
  sessionId: string
  orderId: string
  shopId: string
  customerId: string
  type: TicketType
  reason: string
  status: TicketStatus
  processingNote: string
  createdAt: string
  updatedAt: string
  history?: TicketHandlingRecord[]
}

export interface TicketHandlingRecord {
  historyId: string
  fromStatus: TicketStatus | null
  toStatus: TicketStatus
  note: string
  operatorId: string
  occurredAt: string
}

export interface KnowledgeReference {
  knowledgeId: string
  title: string
  source: string
  content: string
  scope: string
}

export interface ExecutionRecord {
  executionId: string
  action: string
  status: 'SUCCESS' | 'FAILED'
  summary: string
  relatedId?: string
  createdAt: string
}

export interface SessionSnapshot {
  session: WorkbenchSession
  messages: WorkbenchMessage[]
  orders: CommerceOrder[]
  logistics: OrderLogistics[]
  tickets: ServiceTicket[]
  knowledge: KnowledgeReference[]
  executions: ExecutionRecord[]
  selectedOrderId?: string
  awaitingConfirmation?: boolean
  analysis?: {
    scene: string
    intentName: string
    emotion: string
    analysisSource: 'MODEL' | 'RULE'
    replySource:
      | 'MODEL_GENERATED'
      | 'MODEL_GENERAL'
      | 'MODEL_KNOWLEDGE'
      | 'RULE_KNOWLEDGE'
      | 'BUSINESS_TOOL'
      | 'CLARIFICATION'
  }
}

export interface OpenSessionParams {
  shopId: string
  customerId: string
  forceNew: boolean
}

export interface SendWorkbenchMessageParams {
  sessionId: string
  clientMessageId: string
  content: string
}

export interface UpdateTicketParams {
  merchantId?: string
  shopId?: string
  sessionId: string
  ticketId: string
  expectedStatus?: TicketStatus
  status: TicketStatus
  processingNote: string
}

/** A/D 后端与本地模拟实现使用同一契约；真实接口失败时不会切换到模拟数据。 */
export interface WorkbenchApi {
  fetchCatalog(signal?: AbortSignal): Promise<DemoCatalog>
  fetchSessions(signal?: AbortSignal): Promise<WorkbenchSession[]>
  fetchSession(sessionId: string, signal?: AbortSignal): Promise<SessionSnapshot>
  openSession(params: OpenSessionParams): Promise<SessionSnapshot>
  sendCustomerMessage(params: SendWorkbenchMessageParams): Promise<SessionSnapshot>
  sendHumanMessage(params: SendWorkbenchMessageParams): Promise<SessionSnapshot>
  setReceptionMode(sessionId: string, mode: 'HUMAN' | 'AI'): Promise<SessionSnapshot>
  updateTicket(params: UpdateTicketParams): Promise<SessionSnapshot>
  resetDemo(): Promise<void>
}
