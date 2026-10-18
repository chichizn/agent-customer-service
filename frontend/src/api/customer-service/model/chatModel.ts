/**
 * 💬 对话管理类型定义
 */

/** 会话项 */
export interface SessionItem {
  sessionId: string
  userId: string
  userName?: string
  channel?: string
  status: number
  statusText?: string
  sceneCode?: string
  intent?: string
  emotion?: string
  messageCount: number
  firstMessageTime?: string
  lastMessageTime?: string
  createTime: string
}

/** 会话详情 */
export interface SessionDetail extends SessionItem {
  agentId?: string
  agentName?: string
  transferReason?: string
  rating?: number
  feedback?: string
  endTime?: string
  extProperties?: string
  recentMessages?: MessageItem[]
}

/** 会话创建参数 */
export interface SessionCreateParams {
  userId: string
  userName?: string
  channel?: string
  sceneCode?: string
  extProperties?: string
}

/** 会话查询参数 */
export interface SessionQueryParams {
  userId?: string
  channel?: string
  status?: number
  sceneCode?: string
  startTime?: string
  endTime?: string
}

/** 消息项 */
export interface MessageItem {
  messageId: string
  sessionId: string
  role: 'user' | 'assistant' | 'system'
  content: string
  contentType?: string
  senderId?: string
  senderName?: string
  intent?: string
  confidence?: number
  responseTime?: number
  userFeedback?: string
  createTime: string
}

/** 消息发送参数 */
export interface MessageSendParams {
  sessionId: string
  content: string
  contentType?: string
  senderId?: string
  senderName?: string
  stream?: boolean
}

/** 历史会话项 */
export interface HistorySessionItem {
  sessionId: string
  userId: string
  userName?: string
  channel?: string
  intent?: string
  messageCount: number
  rating?: number
  transferred: boolean
  createTime: string
  endTime?: string
  duration?: number
}

/** 历史详情 */
export interface HistoryDetail extends HistorySessionItem {
  agentId?: string
  agentName?: string
  transferReason?: string
  feedback?: string
  messages: MessageItem[]
}

/** 评价项 */
export interface FeedbackItem {
  sessionId: string
  userId: string
  userName?: string
  rating: number
  content?: string
  tags?: string
  channel?: string
  createTime: string
}

/** 评价统计 */
export interface FeedbackStats {
  totalCount: number
  averageRating: number
  positiveRate: number
  ratingDistribution: Record<number, number>
  hotTags: Record<string, number>
}

/** 转人工结果 */
export interface TransferResult {
  success: boolean
  message?: string
  queuePosition?: number
  estimatedWaitTime?: number
  agentId?: string
  agentName?: string
}

