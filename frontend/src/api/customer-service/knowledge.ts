/**
 * 知识管理 API
 * 路径与后端 KnowledgeController (ApiConstants.Knowledge) 完全对齐
 */
import request from '@/utils/http'

// ==================== 知识总览 ====================

/**
 * 获取知识总览
 * POST /api/knowledge/overview
 */
export function fetchGetOverview() {
  return request.post<any>({
    url: '/api/knowledge/overview',
    data: {}
  })
}

// ==================== 商品知识 ====================

export function fetchListProducts(data?: { keyword?: string; category?: string; status?: string; page?: number; size?: number }) {
  return request.post<any>({
    url: '/api/knowledge/product/list',
    data: data || {}
  })
}

export function fetchCreateProduct(data: any) {
  return request.post<any>({
    url: '/api/knowledge/product/create',
    data
  })
}

export function fetchUpdateProduct(data: any) {
  return request.post<any>({
    url: '/api/knowledge/product/update',
    data
  })
}

export function fetchDeleteProduct(id: number) {
  return request.post<void>({
    url: '/api/knowledge/product/delete',
    data: { id }
  })
}

export function fetchGetProductDetail(id: number) {
  return request.post<any>({
    url: '/api/knowledge/product/detail',
    data: { id }
  })
}

// ==================== 活动知识 ====================

export function fetchListActivities(data?: { keyword?: string; activityType?: string; status?: string; page?: number; size?: number }) {
  return request.post<any>({
    url: '/api/knowledge/activity/list',
    data: data || {}
  })
}

export function fetchCreateActivity(data: any) {
  return request.post<any>({
    url: '/api/knowledge/activity/create',
    data
  })
}

export function fetchUpdateActivity(data: any) {
  return request.post<any>({
    url: '/api/knowledge/activity/update',
    data
  })
}

export function fetchDeleteActivity(id: number) {
  return request.post<void>({
    url: '/api/knowledge/activity/delete',
    data: { id }
  })
}

export function fetchGetActivityDetail(id: number) {
  return request.post<any>({
    url: '/api/knowledge/activity/detail',
    data: { id }
  })
}

// ==================== FAQ问答 ====================

export function fetchListFaqs(data?: { keyword?: string; category?: string; status?: string; page?: number; size?: number }) {
  return request.post<any>({
    url: '/api/knowledge/faq/list',
    data: data || {}
  })
}

export function fetchCreateFaq(data: any) {
  return request.post<any>({
    url: '/api/knowledge/faq/create',
    data
  })
}

export function fetchUpdateFaq(data: any) {
  return request.post<any>({
    url: '/api/knowledge/faq/update',
    data
  })
}

export function fetchDeleteFaq(id: number) {
  return request.post<void>({
    url: '/api/knowledge/faq/delete',
    data: { id }
  })
}

export function fetchGetFaqDetail(id: number) {
  return request.post<any>({
    url: '/api/knowledge/faq/detail',
    data: { id }
  })
}

// ==================== 行业知识 ====================

export function fetchListIndustries(data?: { keyword?: string; industryType?: string; status?: string; page?: number; size?: number }) {
  return request.post<any>({
    url: '/api/knowledge/industry/list',
    data: data || {}
  })
}

export function fetchCreateIndustry(data: any) {
  return request.post<any>({
    url: '/api/knowledge/industry/create',
    data
  })
}

export function fetchUpdateIndustry(data: any) {
  return request.post<any>({
    url: '/api/knowledge/industry/update',
    data
  })
}

export function fetchDeleteIndustry(id: number) {
  return request.post<void>({
    url: '/api/knowledge/industry/delete',
    data: { id }
  })
}

// ==================== 场景解决方案 ====================

export function fetchListSolutions(data?: { keyword?: string; sceneType?: string; status?: string; page?: number; size?: number }) {
  return request.post<any>({
    url: '/api/knowledge/solution/list',
    data: data || {}
  })
}

export function fetchCreateSolution(data: any) {
  return request.post<any>({
    url: '/api/knowledge/solution/create',
    data
  })
}

export function fetchUpdateSolution(data: any) {
  return request.post<any>({
    url: '/api/knowledge/solution/update',
    data
  })
}

export function fetchDeleteSolution(id: number) {
  return request.post<void>({
    url: '/api/knowledge/solution/delete',
    data: { id }
  })
}

export function fetchGetSolutionDetail(id: number) {
  return request.post<any>({
    url: '/api/knowledge/solution/detail',
    data: { id }
  })
}

// ==================== 知识向量化 ====================

export function fetchVectorize(data: { knowledgeType: string; knowledgeId: number }) {
  return request.post<any>({
    url: '/api/knowledge/vectorize',
    data
  })
}

export function fetchGetVectorizeStatus(taskId: string) {
  return request.post<any>({
    url: '/api/knowledge/vectorize/status',
    data: { taskId }
  })
}

// ==================== 导入导出 ====================

export function fetchImportProducts(data: { knowledgeType: string; fileUrl: string }) {
  return request.post<any>({
    url: '/api/knowledge/product/import',
    data
  })
}

export function fetchExportProducts(data: { knowledgeType: string; ids?: number[] }) {
  return request.post<any>({
    url: '/api/knowledge/product/export',
    data
  })
}

export function fetchImportFaqs(data: { knowledgeType: string; fileUrl: string }) {
  return request.post<any>({
    url: '/api/knowledge/faq/import',
    data
  })
}
