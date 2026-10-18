import type { MockConversation, MockWorkspace } from './fixtures'
import { createMockWorkspace, MOCK_CHANGE_EVENT, MOCK_STORAGE_KEY, shopKnowledge } from './fixtures'
import type {
  MessageRole,
  SendWorkbenchMessageParams,
  SessionSnapshot,
  TicketStatus,
  WorkbenchApi
} from '../../api/customer-service/model/workbenchModel'

export const ticketTransitions: Record<TicketStatus, TicketStatus[]> = {
  PENDING_REVIEW: ['PROCESSING', 'REJECTED'],
  PROCESSING: ['COMPLETED'],
  COMPLETED: [],
  REJECTED: []
}

/** 仅存储到独立前端演示键；不访问真实平台或项目后端。 */
export function createMockWorkbenchApi(storage: Storage = localStorage): WorkbenchApi {
  function read(): MockWorkspace {
    const raw = storage.getItem(MOCK_STORAGE_KEY)
    if (raw) {
      const workspace = JSON.parse(raw) as MockWorkspace
      if (workspace.version !== 1 || !Array.isArray(workspace.conversations)) {
        throw new Error('本地演示数据版本不匹配，请通过演示重置恢复。')
      }
      return workspace
    }
    const workspace = createMockWorkspace()
    storage.setItem(MOCK_STORAGE_KEY, JSON.stringify(workspace))
    return workspace
  }

  async function mutate<T>(operation: (workspace: MockWorkspace) => T): Promise<T> {
    const execute = () => {
      const workspace = read()
      const result = operation(workspace)
      storage.setItem(MOCK_STORAGE_KEY, JSON.stringify(workspace))
      window.dispatchEvent(new Event(MOCK_CHANGE_EVENT))
      return result
    }
    // 两个标签页同时回复/接管时，避免整份 localStorage 的读改写互相覆盖。
    if (navigator.locks) return navigator.locks.request(MOCK_STORAGE_KEY, execute)
    return execute()
  }

  function conversation(workspace: MockWorkspace, sessionId: string) {
    const item = workspace.conversations.find((entry) => entry.session.sessionId === sessionId)
    if (!item) throw new Error('会话不存在，演示可能已重置，请重新选择身份。')
    return item
  }

  function scopedOrders(workspace: MockWorkspace, item: MockConversation) {
    return workspace.orders.filter(
      (order) =>
        order.shopId === item.session.shopId && order.customerId === item.session.customerId
    )
  }

  function snapshot(workspace: MockWorkspace, sessionId: string): SessionSnapshot {
    const item = conversation(workspace, sessionId)
    const orders = scopedOrders(workspace, item)
    return {
      session: item.session,
      messages: item.messages,
      orders,
      logistics: workspace.logistics.filter((entry) =>
        orders.some((order) => order.orderId === entry.orderId)
      ),
      tickets: workspace.tickets.filter(
        (ticket) =>
          ticket.shopId === item.session.shopId && ticket.customerId === item.session.customerId
      ),
      knowledge: item.knowledge,
      executions: item.executions,
      selectedOrderId: item.selectedOrderId,
      awaitingConfirmation: !!item.pendingTicket
    }
  }

  function append(
    item: MockConversation,
    role: MessageRole,
    content: string,
    extra: Partial<SessionSnapshot['messages'][number]> = {}
  ) {
    const createdAt = new Date().toISOString()
    item.messages.push({
      ...extra,
      messageId: crypto.randomUUID(),
      sessionId: item.session.sessionId,
      role,
      content,
      createdAt
    })
    item.session.lastMessage = content
    item.session.updatedAt = createdAt
  }

  function record(
    item: MockConversation,
    action: string,
    summary: string,
    relatedId?: string,
    status: 'SUCCESS' | 'FAILED' = 'SUCCESS'
  ) {
    item.executions.push({
      executionId: crypto.randomUUID(),
      action,
      summary,
      relatedId,
      status,
      createdAt: new Date().toISOString()
    })
  }

  function respond(workspace: MockWorkspace, item: MockConversation, content: string) {
    const orders = scopedOrders(workspace, item)
    const specifiedId = content.match(/(?:TB|DY)\d+/i)?.[0]?.toUpperCase()
    if (specifiedId && !orders.some((order) => order.orderId === specifiedId)) {
      record(item, '查询订单', '当前店铺与顾客范围内未找到订单', undefined, 'FAILED')
      append(item, 'ai', '未找到属于当前店铺和顾客的订单，请核对订单号。')
      return
    }
    if (/人工|超额|额外补偿|赔偿.*1000/.test(content)) {
      item.pendingTicket = undefined
      item.pendingQuery = undefined
      item.pendingReason = undefined
      item.session.receptionMode = 'WAITING_HUMAN'
      item.receptionVersion++
      item.session.handoffSummary = `【本地模拟交接】${item.session.customerName}：${content}；关联订单：${item.selectedOrderId || '尚未确认'}；已有工单：${
        workspace.tickets
          .filter((ticket) => ticket.sessionId === item.session.sessionId)
          .map((ticket) => ticket.ticketId)
          .join('、') || '无'
      }；待办：商家核实诉求。`
      record(item, '转交人工', '会话已进入等待人工状态')
      append(item, 'system', '已进入等待人工，后续消息由商家处理。')
      return
    }
    if (/^(确认提交|确认申请|确认)$/.test(content.trim()) && item.pendingTicket) {
      const pending = item.pendingTicket
      const existing = workspace.tickets.find(
        (ticket) =>
          ticket.orderId === pending.orderId &&
          ticket.shopId === item.session.shopId &&
          ticket.customerId === item.session.customerId &&
          ticket.type === pending.type &&
          ['PENDING_REVIEW', 'PROCESSING'].includes(ticket.status)
      )
      const now = new Date().toISOString()
      const ticket = existing || {
        ticketId: `LOCAL-${crypto.randomUUID().slice(0, 8).toUpperCase()}`,
        sessionId: item.session.sessionId,
        orderId: pending.orderId,
        shopId: item.session.shopId,
        customerId: item.session.customerId,
        type: pending.type,
        reason: pending.reason,
        status: 'PENDING_REVIEW' as const,
        processingNote: '',
        createdAt: now,
        updatedAt: now
      }
      if (!existing) workspace.tickets.push(ticket)
      item.pendingTicket = undefined
      item.pendingQuery = undefined
      item.pendingReason = undefined
      record(
        item,
        existing ? '查询已有工单' : '创建本地工单',
        '前端模拟数据已保存，待商家审核',
        ticket.ticketId
      )
      append(
        item,
        'ai',
        `已记录诉求，待商家审核。本地模拟工单号：${ticket.ticketId}。这不代表平台受理或资金退款。`,
        { ticketId: ticket.ticketId }
      )
      return
    }
    if (/取消申请|暂不提交/.test(content)) {
      item.pendingTicket = undefined
      item.pendingQuery = undefined
      item.pendingReason = undefined
      append(item, 'ai', '已取消本次待提交诉求，没有新建工单。')
      return
    }
    if (/进度|工单|申请怎么样/.test(content)) {
      const tickets = workspace.tickets.filter(
        (ticket) =>
          ticket.customerId === item.session.customerId &&
          ticket.shopId === item.session.shopId &&
          (!item.selectedOrderId || ticket.orderId === item.selectedOrderId)
      )
      const requestedTicketId = content.match(/LOCAL-[A-Z0-9]+/i)?.[0]?.toUpperCase()
      const ticket = requestedTicketId
        ? tickets.find((entry) => entry.ticketId === requestedTicketId)
        : tickets.at(-1)
      record(
        item,
        '查询工单',
        ticket ? `读取当前本地工单状态：${ticket.status}` : '未找到本地工单',
        ticket?.ticketId
      )
      append(
        item,
        'ai',
        ticket
          ? `本地工单 ${ticket.ticketId} 当前状态：${ticket.status}。${ticket.processingNote || '等待商家处理。'}`
          : '当前顾客暂未找到该本地工单，可先说明订单和诉求。',
        { ticketId: ticket?.ticketId }
      )
      return
    }
    if (/规则|政策|几天/.test(content)) {
      const knowledge = shopKnowledge(item.session.shopId)
      item.knowledge = [knowledge]
      record(item, '检索演示知识', knowledge.title, knowledge.knowledgeId)
      append(item, 'ai', `【本地规则模拟回复】${knowledge.content}`)
      return
    }
    const query = /退货|退回/.test(content)
      ? 'return'
      : /破损|坏了|补偿/.test(content)
        ? 'compensation'
        : /物流|到哪|快递/.test(content)
          ? 'logistics'
          : undefined
    if (query) {
      item.pendingTicket = undefined
      item.pendingQuery = query
      item.pendingReason = content
    }
    if (specifiedId) {
      if (item.selectedOrderId !== specifiedId) item.pendingTicket = undefined
      item.selectedOrderId = specifiedId
    }
    if (query || specifiedId || /订单/.test(content)) {
      let matches = orders
      if (!specifiedId && /杯/.test(content))
        matches = orders.filter((order) =>
          order.items.some((product) => product.name.includes('杯'))
        )
      if (!item.selectedOrderId && matches.length === 1) item.selectedOrderId = matches[0].orderId
      record(
        item,
        '查询订单',
        `在当前顾客范围内找到 ${matches.length} 笔订单`,
        item.selectedOrderId
      )
      const selected = orders.find((order) => order.orderId === item.selectedOrderId)
      if (!selected) {
        append(
          item,
          'ai',
          matches.length
            ? '找到多笔订单，请点击订单编号确认本次要咨询的订单。'
            : '没有找到匹配订单，请补充订单号。',
          { orderIds: matches.map((order) => order.orderId) }
        )
        return
      }
      if (item.pendingQuery === 'logistics') {
        item.pendingQuery = undefined
        item.pendingReason = undefined
        const logistics = workspace.logistics.find((entry) => entry.orderId === selected.orderId)
        record(item, '查询物流', logistics?.status || '暂无物流', selected.orderId)
        append(
          item,
          'ai',
          `订单 ${selected.orderId}：${logistics?.status || '暂无物流'}。${logistics?.nodes.at(-1)?.description || '尚无物流节点。'}`,
          { orderIds: [selected.orderId] }
        )
      } else if (item.pendingQuery === 'return' || item.pendingQuery === 'compensation') {
        item.pendingTicket = {
          orderId: selected.orderId,
          type: item.pendingQuery === 'return' ? 'RETURN' : 'COMPENSATION',
          reason: item.pendingReason || content
        }
        append(
          item,
          'ai',
          `将为订单 ${selected.orderId} 记录${item.pendingTicket.type === 'RETURN' ? '退货' : '异常/补偿'}诉求，原因：${item.pendingTicket.reason}。请点击“确认提交”或回复“暂不提交”。提交后仅生成本地模拟工单，待商家审核。`,
          { orderIds: [selected.orderId] }
        )
      } else {
        append(item, 'ai', `已选择订单 ${selected.orderId}，可继续询问物流或说明售后诉求。`, {
          orderIds: [selected.orderId]
        })
      }
      return
    }
    const knowledge = shopKnowledge(item.session.shopId)
    item.knowledge = [knowledge]
    record(item, '检索演示知识', knowledge.title, knowledge.knowledgeId)
    append(
      item,
      'ai',
      `【本地规则模拟回复】${knowledge.content} 如需查物流或申请售后，请说明具体诉求；也可输入“转人工”。`
    )
  }

  function validateContent(params: SendWorkbenchMessageParams) {
    if (!params.content.trim() || params.content.length > 2000 || !params.clientMessageId)
      throw new Error('请输入 1～2000 字的消息，并保留发送标识。')
  }

  return {
    async fetchCatalog() {
      return read().catalog
    },
    async fetchSessions() {
      return read()
        .conversations.map((item) => item.session)
        .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
    },
    async fetchSession(sessionId) {
      return snapshot(read(), sessionId)
    },
    async openSession(params) {
      return mutate((workspace) => {
        const customer = workspace.catalog.customers.find(
          (item) => item.customerId === params.customerId && item.shopId === params.shopId
        )
        const shop = workspace.catalog.shops.find((item) => item.shopId === params.shopId)
        if (!customer || !shop) throw new Error('店铺或顾客身份无效。')
        const existing = workspace.conversations
          .filter(
            (item) =>
              item.session.customerId === customer.customerId &&
              item.session.shopId === shop.shopId &&
              item.session.status === 'active'
          )
          .at(-1)
        if (existing && !params.forceNew) return snapshot(workspace, existing.session.sessionId)
        const sessionId = crypto.randomUUID()
        const item: MockConversation = {
          session: {
            sessionId,
            shopId: shop.shopId,
            shopName: shop.name,
            platform: shop.platform,
            customerId: customer.customerId,
            customerName: customer.name,
            receptionMode: 'AI',
            status: 'active',
            lastMessage: '',
            updatedAt: new Date().toISOString()
          },
          messages: [],
          executions: [],
          knowledge: [],
          processedRequests: [],
          receptionVersion: 0
        }
        append(item, 'system', '本地前端模拟已开启；可查询物流、记录本地工单，或转人工。')
        workspace.conversations.push(item)
        return snapshot(workspace, sessionId)
      })
    },
    async sendCustomerMessage(params) {
      validateContent(params)
      const started = await mutate((workspace) => {
        const item = conversation(workspace, params.sessionId)
        const key = `customer:${params.clientMessageId}`
        if (item.processedRequests.includes(key)) return undefined
        item.processedRequests.push(key)
        append(item, 'customer', params.content.trim(), { clientMessageId: params.clientMessageId })
        return {
          generation: workspace.generation,
          version: item.receptionVersion,
          canReply: item.session.receptionMode === 'AI'
        }
      })
      if (!started?.canReply) return snapshot(read(), params.sessionId)
      await new Promise((resolve) => setTimeout(resolve, 650))
      return mutate((workspace) => {
        if (workspace.generation !== started.generation)
          throw new Error('演示已重置，本次处理已停止。')
        const item = conversation(workspace, params.sessionId)
        if (item.session.receptionMode === 'AI' && item.receptionVersion === started.version)
          respond(workspace, item, params.content.trim())
        return snapshot(workspace, params.sessionId)
      })
    },
    async sendHumanMessage(params) {
      validateContent(params)
      return mutate((workspace) => {
        const item = conversation(workspace, params.sessionId)
        if (item.session.receptionMode !== 'HUMAN')
          throw new Error('请先接管会话，再发送人工回复。')
        const key = `human:${params.clientMessageId}`
        if (!item.processedRequests.includes(key)) {
          item.processedRequests.push(key)
          append(item, 'human', params.content.trim(), { clientMessageId: params.clientMessageId })
        }
        return snapshot(workspace, params.sessionId)
      })
    },
    async setReceptionMode(sessionId, mode) {
      return mutate((workspace) => {
        const item = conversation(workspace, sessionId)
        if (item.session.receptionMode !== mode) {
          item.pendingTicket = undefined
          item.pendingQuery = undefined
          item.pendingReason = undefined
          item.session.receptionMode = mode
          item.receptionVersion++
          append(
            item,
            'system',
            mode === 'HUMAN'
              ? '商家已接管会话，由人工回复。'
              : '已交回 AI，下一条顾客消息将触发处理。'
          )
          record(item, mode === 'HUMAN' ? '人工接管' : '交回 AI', '接待模式更新成功')
        }
        return snapshot(workspace, sessionId)
      })
    },
    async updateTicket(params) {
      return mutate((workspace) => {
        const item = conversation(workspace, params.sessionId)
        const ticket = workspace.tickets.find(
          (entry) =>
            entry.ticketId === params.ticketId &&
            entry.shopId === item.session.shopId &&
            entry.customerId === item.session.customerId
        )
        if (!ticket || !ticketTransitions[ticket.status].includes(params.status))
          throw new Error('工单不存在或状态已变化，请刷新后重试。')
        if (!params.processingNote.trim()) throw new Error('请填写处理说明。')
        ticket.status = params.status
        ticket.processingNote = params.processingNote.trim()
        ticket.updatedAt = new Date().toISOString()
        record(item, '更新本地工单', params.processingNote.trim(), ticket.ticketId)
        return snapshot(workspace, params.sessionId)
      })
    },
    async resetDemo() {
      const reset = () => {
        storage.setItem(MOCK_STORAGE_KEY, JSON.stringify(createMockWorkspace()))
        window.dispatchEvent(new Event(MOCK_CHANGE_EVENT))
      }
      if (navigator.locks) await navigator.locks.request(MOCK_STORAGE_KEY, reset)
      else reset()
    }
  }
}
