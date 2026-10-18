/**
 * 系统管理模块类型定义
 *
 * @module api/model/systemManageModel
 */

import type { PaginatedResponse, CommonSearchParams } from '@/types/common/response'

// ========== 用户相关 ==========

/** 用户列表响应 */
export type UserList = PaginatedResponse<UserListItem>

/** 用户列表项 */
export interface UserListItem {
  id: number
  avatar: string
  status: string
  userName: string
  userGender: string
  nickName: string
  userPhone: string
  userEmail: string
  userRoles: string[]
  createBy: string
  createTime: string
  updateBy: string
  updateTime: string
}

/** 用户搜索参数 */
export type UserSearchParams = Partial<
  Pick<UserListItem, 'id' | 'userName' | 'userGender' | 'userPhone' | 'userEmail' | 'status'> &
    CommonSearchParams
>

/** 创建用户参数 */
export interface CreateUserParams {
  userName: string
  password: string
  nickName: string
  userEmail: string
  userPhone: string
  userGender: string
  roleIds: number[]
}

/** 更新用户参数 */
export type UpdateUserParams = Partial<Omit<CreateUserParams, 'password'>> & {
  status?: string
}

// ========== 角色相关 ==========

/** 角色列表响应 */
export type RoleList = PaginatedResponse<RoleListItem>

/** 角色列表项 */
export interface RoleListItem {
  roleId: number
  roleName: string
  roleCode: string
  description: string
  enabled: boolean
  createTime: string
}

/** 角色搜索参数 */
export type RoleSearchParams = Partial<
  Pick<RoleListItem, 'roleId' | 'roleName' | 'roleCode' | 'description' | 'enabled'> &
    CommonSearchParams
>

/** 创建角色参数 */
export interface CreateRoleParams {
  roleName: string
  roleCode: string
  description?: string
  menuIds: number[]
}

/** 更新角色参数 */
export type UpdateRoleParams = Partial<CreateRoleParams>

