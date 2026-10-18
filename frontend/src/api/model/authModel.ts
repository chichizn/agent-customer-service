/**
 * 认证模块类型定义
 *
 * @module api/model/authModel
 */

/** 登录参数 */
export interface LoginParams {
  userName: string
  password: string
}

/** 登录响应 */
export interface LoginResponse {
  token: string
  refreshToken: string
}

/** 用户信息 */
export interface UserInfo {
  buttons: string[]
  roles: string[]
  userId: number
  userName: string
  email: string
  avatar?: string
}

