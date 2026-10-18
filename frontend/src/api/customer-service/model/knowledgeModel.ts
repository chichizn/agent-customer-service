/**
 * 📚 知识管理类型定义
 */

/** 知识列表项 */
export interface KnowledgeItem {
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
export interface KnowledgeDetail extends KnowledgeItem {
  content: string
  source?: number
  externalId?: string
  extProperties?: string
  vectorId?: string
  vectorizeTime?: string
  createBy?: string
  updateBy?: string
}

/** 知识查询参数 */
export interface KnowledgeQueryParams {
  keyword?: string
  categoryId?: number
  type?: number
  status?: number
  vectorizeStatus?: number
  tag?: string
}

/** 知识创建参数 */
export interface KnowledgeCreateParams {
  title: string
  content: string
  summary?: string
  type: number
  categoryId?: number
  tags?: string
  priority?: number
  extProperties?: string
}

/** 知识更新参数 */
export interface KnowledgeUpdateParams {
  title?: string
  content?: string
  summary?: string
  type?: number
  categoryId?: number
  status?: number
  tags?: string
  priority?: number
  extProperties?: string
}

/** 分类项 */
export interface CategoryItem {
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
export interface CategoryTreeItem extends CategoryItem {
  children?: CategoryTreeItem[]
}

/** 导入结果 */
export interface ImportResult {
  totalCount: number
  successCount: number
  failCount: number
  failDetails: string[]
}

/** 向量化结果 */
export interface VectorizeResult {
  knowledgeId: number
  success: boolean
  message: string
  vectorId?: string
}

/** 向量化状态 */
export interface VectorizeStatus {
  knowledgeId: number
  status: number
  statusText: string
  vectorizeTime?: string
}

