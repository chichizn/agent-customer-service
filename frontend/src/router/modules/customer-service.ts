import { AppRouteRecord } from '@/types/router'

/**
 * 🤖 智能客服系统路由配置
 * 
 * 三大核心模块：
 * 1. 对话中心 - 核心对话功能（四栏布局）
 * 2. 知识中心 - 知识库管理（Tab切换）
 * 3. 场景配置 - 意图/提示词/规则配置
 */
export const customerServiceRoutes: AppRouteRecord = {
  path: '/customer-service',
  name: 'CustomerService',
  component: '/index/index',
  redirect: '/customer-service/chat',
  meta: {
    title: '智能客服',
    icon: 'ri:customer-service-2-line',
    roles: ['R_SUPER', 'R_ADMIN', 'R_GUEST']
  },
  children: [
    // ==================== 对话中心（核心模块） ====================
    {
      path: 'chat',
      name: 'ChatCenter',
      component: '/customer-service/chat/index',
      meta: {
        title: '对话中心',
        icon: 'ri:message-3-line',
        keepAlive: true
      }
    },

    // ==================== 知识中心 ====================
    {
      path: 'knowledge',
      name: 'KnowledgeCenter',
      component: '/customer-service/knowledge/index',
      meta: {
        title: '知识中心',
        icon: 'ri:book-open-line',
        keepAlive: true
      }
    },

    // ==================== 场景配置 ====================
    {
      path: 'scene',
      name: 'SceneConfig',
      component: '/customer-service/scene/index',
      meta: {
        title: '场景配置',
        icon: 'ri:settings-4-line',
        keepAlive: true
      }
    }
  ]
}
