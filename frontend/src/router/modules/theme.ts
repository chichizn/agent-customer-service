import { AppRouteRecord } from '@/types/router'

export const themeRoutes: AppRouteRecord = {
  name: 'Theme',
  path: '/theme',
  component: '/theme/index',
  meta: {
    title: '主题管理',
    icon: 'ri:palette-line',
  }
}
