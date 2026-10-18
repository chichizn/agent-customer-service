import { AppRouteRecord } from '@/types/router'
import { dashboardRoutes } from './dashboard'
import { systemRoutes } from './system'
import { resultRoutes } from './result'
import { exceptionRoutes } from './exception'
import { themeRoutes } from './theme'
import { customerServiceRoutes } from './customer-service'

/**
 * 导出所有模块化路由
 */
export const routeModules: AppRouteRecord[] = [
  dashboardRoutes,
  customerServiceRoutes,  // 智能客服系统
  systemRoutes,
  resultRoutes,
  exceptionRoutes,
  themeRoutes
]
