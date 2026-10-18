/**
 * 🤖 智能客服系统类型定义
 */
declare namespace Api {
  namespace CustomerService {
    // ==================== 知识管理 ====================
    
    /** 知识列表项 */
    interface KnowledgeItem {
      id: number
      title: string
      summary?: string
      type: number
      typeName?: string
      categoryId?: number
      categoryName?: string
      status: number
      vectorizeStatus: number
      tags?: string
      priority?: number
      createTime: string
      updateTime: string
    }

    /** 知识详情 */
    interface KnowledgeDetail extends KnowledgeItem {
      content: string
      source?: number
      externalId?: string
      extProperties?: string
      vectorId?: string
      vectorizeTime?: string
      createBy?: string
      updateBy?: string
    }

    /** 分类项 */
    interface CategoryItem {
      id: number
      name: string
      code?: string
      parentId?: number
      path?: string
      level?: number
      sort?: number
      status: number
      icon?: string
      description?: string
      createTime: string
    }

    /** 分类树项 */
    interface CategoryTreeItem extends CategoryItem {
      children?: CategoryTreeItem[]
    }

    // ==================== 对话管理 ====================

    /** 会话项 */
    interface SessionItem {
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
    interface SessionDetail extends SessionItem {
      agentId?: string
      agentName?: string
      transferReason?: string
      rating?: number
      feedback?: string
      endTime?: string
      extProperties?: string
      recentMessages?: MessageItem[]
    }

    /** 消息项 */
    interface MessageItem {
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

    /** 历史会话项 */
    interface HistorySessionItem {
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

    /** 评价项 */
    interface FeedbackItem {
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
    interface FeedbackStats {
      totalCount: number
      averageRating: number
      positiveRate: number
      ratingDistribution: Record<number, number>
      hotTags: Record<string, number>
    }

    // ==================== 场景配置 ====================

    /** 意图项 */
    interface IntentItem {
      id: number
      name: string
      code: string
      description?: string
      samples?: string[]
      status: number
      createTime: string
    }

    /** 提示词模板 */
    interface PromptTemplate {
      id: number
      name: string
      code: string
      content: string
      type: number
      sceneCode?: string
      version: number
      status: number
      createTime: string
      updateTime: string
    }

    /** 场景规则 */
    interface SceneRule {
      id: number
      name: string
      code: string
      condition: string
      action: string
      priority: number
      status: number
      createTime: string
    }

    // ==================== RAG检索 ====================

    /** 检索结果 */
    interface SearchResult {
      knowledgeId: number
      title: string
      content: string
      score: number
      category?: string
      highlights?: string[]
    }

    /** 检索配置 */
    interface RagConfig {
      topK: number
      minScore: number
      enableRerank: boolean
      rerankTopK?: number
      embeddingModel: string
    }

    // ==================== 模型管理 ====================

    /** 模型配置 */
    interface ModelConfig {
      id: number
      name: string
      provider: string
      modelId: string
      apiKey?: string
      endpoint?: string
      maxTokens: number
      temperature: number
      status: number
    }

    /** 调用日志 */
    interface LlmLog {
      id: number
      sessionId?: string
      modelName: string
      prompt: string
      response: string
      tokenUsage: number
      responseTime: number
      success: boolean
      errorMessage?: string
      createTime: string
    }

    // ==================== 人机协作 ====================

    /** 转人工记录 */
    interface TransferRecord {
      id: number
      sessionId: string
      userId: string
      userName?: string
      reason?: string
      agentId?: string
      agentName?: string
      status: number
      queuePosition?: number
      waitTime?: number
      createTime: string
      acceptTime?: string
      completeTime?: string
    }

    /** 坐席 */
    interface Agent {
      id: string
      name: string
      status: number
      currentSessions: number
      maxSessions: number
      skillGroup?: string
      lastActiveTime?: string
    }

    // ==================== 系统监控 ====================

    /** 概览统计 */
    interface OverviewStats {
      todaySessionCount: number
      todayMessageCount: number
      activeSessionCount: number
      avgResponseTime: number
      satisfactionRate: number
      transferRate: number
    }

    /** 趋势数据 */
    interface TrendData {
      date: string
      sessionCount: number
      messageCount: number
      avgResponseTime: number
      satisfactionRate: number
    }

    /** 告警 */
    interface Alert {
      id: number
      type: string
      level: 'info' | 'warning' | 'error'
      message: string
      acknowledged: boolean
      createTime: string
      ackTime?: string
    }
  }
}

