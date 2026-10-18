/**
 * 场景配置 API
 * 路径与后端 SceneController (ApiConstants.Scene) 完全对齐
 */
import request from '@/utils/http'

// ==================== 意图管理 ====================

export function fetchListIntents(data?: { keyword?: string; sceneType?: string; status?: string; page?: number; size?: number }) {
  return request.post<any>({
    url: '/api/scene/intent/list',
    data: data || {}
  })
}

export function fetchCreateIntent(data: any) {
  return request.post<any>({
    url: '/api/scene/intent/create',
    data
  })
}

export function fetchUpdateIntent(data: any) {
  return request.post<any>({
    url: '/api/scene/intent/update',
    data
  })
}

export function fetchDeleteIntent(id: number) {
  return request.post<void>({
    url: '/api/scene/intent/delete',
    data: { id }
  })
}

export function fetchGetIntentDetail(id: number) {
  return request.post<any>({
    url: '/api/scene/intent/detail',
    data: { id }
  })
}

// ==================== 提示词模板 ====================

export function fetchListPrompts(data?: { keyword?: string; sceneType?: string; status?: string; page?: number; size?: number }) {
  return request.post<any>({
    url: '/api/scene/prompt/list',
    data: data || {}
  })
}

export function fetchCreatePrompt(data: any) {
  return request.post<any>({
    url: '/api/scene/prompt/create',
    data
  })
}

export function fetchUpdatePrompt(data: any) {
  return request.post<any>({
    url: '/api/scene/prompt/update',
    data
  })
}

export function fetchDeletePrompt(id: number) {
  return request.post<void>({
    url: '/api/scene/prompt/delete',
    data: { id }
  })
}

export function fetchGetPromptDetail(id: number) {
  return request.post<any>({
    url: '/api/scene/prompt/detail',
    data: { id }
  })
}

export function fetchTestPrompt(data: { promptId: number; variables?: Record<string, string>; testInput?: string }) {
  return request.post<any>({
    url: '/api/scene/prompt/test',
    data
  })
}

// ==================== 场景规则 ====================

export function fetchListRules(data?: { keyword?: string; sceneType?: string; status?: string; page?: number; size?: number }) {
  return request.post<any>({
    url: '/api/scene/rule/list',
    data: data || {}
  })
}

export function fetchCreateRule(data: any) {
  return request.post<any>({
    url: '/api/scene/rule/create',
    data
  })
}

export function fetchUpdateRule(data: any) {
  return request.post<any>({
    url: '/api/scene/rule/update',
    data
  })
}

export function fetchDeleteRule(id: number) {
  return request.post<void>({
    url: '/api/scene/rule/delete',
    data: { id }
  })
}

export function fetchGetRuleDetail(id: number) {
  return request.post<any>({
    url: '/api/scene/rule/detail',
    data: { id }
  })
}

export function fetchEnableRule(id: number) {
  return request.post<void>({
    url: '/api/scene/rule/enable',
    data: { id }
  })
}

export function fetchDisableRule(id: number) {
  return request.post<void>({
    url: '/api/scene/rule/disable',
    data: { id }
  })
}
