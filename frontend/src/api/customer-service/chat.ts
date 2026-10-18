/**
 * 对话管理 API
 * 路径与后端 ChatController (ApiConstants.Chat) 完全对齐
 */
import request from '@/utils/http'

// ==================== 会话管理 ====================

/**
 * 创建会话
 * POST /api/chat/session/create
 */
export function fetchCreateSession(data: { userId: string; userName?: string; channel?: string }) {
  return request.post<any>({
    url: '/api/chat/session/create',
    data
  })
}

/**
 * 获取会话列表
 * POST /api/chat/session/list
 */
export function fetchGetSessionList(data?: { userId?: string; status?: string }) {
  return request.post<any[]>({
    url: '/api/chat/session/list',
    data: data || {}
  })
}

/**
 * 获取会话详情
 * POST /api/chat/session/get
 */
export function fetchGetSessionDetail(sessionId: string) {
  return request.post<any>({
    url: '/api/chat/session/get',
    data: { sessionId }
  })
}

/**
 * 删除会话
 * POST /api/chat/session/delete
 */
export function fetchDeleteSession(sessionId: string) {
  return request.post<void>({
    url: '/api/chat/session/delete',
    data: { sessionId }
  })
}

// ==================== 消息处理（核心链路） ====================

/**
 * 发送消息
 * POST /api/chat/message/send
 */
export function fetchSendMessage(data: { sessionId: string; content: string; contentType?: string; extra?: Record<string, any> }) {
  return request.post<any>({
    url: '/api/chat/message/send',
    data
  })
}

/**
 * 获取会话消息历史
 * POST /api/chat/message/list
 */
export function fetchGetMessageList(data: { sessionId: string; page?: number; size?: number }) {
  return request.post<any[]>({
    url: '/api/chat/message/list',
    data
  })
}

// ==================== 上下文管理 ====================

/**
 * 更新会话上下文
 * POST /api/chat/context/update
 */
export function fetchUpdateContext(data: {
  sessionId: string
  selectedProducts?: string[]
  selectedActivities?: string[]
  userProfile?: Record<string, any>
}) {
  return request.post<void>({
    url: '/api/chat/context/update',
    data
  })
}

/**
 * 获取会话上下文
 * POST /api/chat/context/get
 */
export function fetchGetContext(sessionId: string) {
  return request.post<any>({
    url: '/api/chat/context/get',
    data: { sessionId }
  })
}
