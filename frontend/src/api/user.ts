/**
 * @file userApi.ts
 * @description 用户管理 API 模块
 * @date 2025-01-11
 */

import request from '@/utils/http'
import { BaseResult } from '@/types/axios'
import type { UserInfo } from '@/types/store'
import type {
  LoginParams,
  LoginResponse,
  Verify2FAParams,
  SendOtpParams,
  SmsLoginParams,
  UserBasic,
  UserBasicUpdateParams,
  AvatarUploadParams,
  UserRegisterParams,
  TenantMember,
  UpdatePasswordParams,
  BindStoreResponse
} from './model/userModel'

/**
 * 用户管理服务
 */
export class UserService {
  /**
   * 用户名密码登录
   * @param {LoginParams} data - 登录请求参数
   * @returns {Promise<BaseResult<LoginResponse>>} 登录结果
   */
  static login(data: LoginParams): Promise<BaseResult<LoginResponse>> {
    return request.post<BaseResult<LoginResponse>>({
      url: '/api/auth/login',
      data
    })
  }

  /**
   * 退出登录
   * @returns {Promise<BaseResult<null>>} 退出结果
   */
  static logout(): Promise<BaseResult<null>> {
    return request.post<BaseResult<null>>({
      url: '/api/auth/logout'
    })
  }

  /**
   * 双因子验证登录
   * @param {Verify2FAParams} data - 双因子验证请求参数
   * @returns {Promise<BaseResult<LoginResponse>>} 登录结果
   */
  static verify2FA(data: Verify2FAParams): Promise<BaseResult<LoginResponse>> {
    return request.post<BaseResult<LoginResponse>>({
      url: '/auth/login/verify-2fa',
      data
    })
  }

  /**
   * 发送验证码
   * @param {SendOtpParams} data - 发送验证码请求参数
   * @returns {Promise<BaseResult<null>>} 操作结果
   */
  static sendOtp(data: SendOtpParams): Promise<BaseResult<null>> {
    return request.post<BaseResult<null>>({
      url: '/auth/otp/send',
      data
    })
  }

  /**
   * 手机号验证码登录
   * @param {SmsLoginParams} data - 手机号验证码登录请求参数
   * @returns {Promise<BaseResult<LoginResponse>>} 登录结果
   */
  static smsLogin(data: SmsLoginParams): Promise<BaseResult<LoginResponse>> {
    return request.post<BaseResult<LoginResponse>>({
      url: '/auth/login/verify-2fa',
      data
    })
  }

  /**
   * 登录用户解绑店铺
   * @returns {Promise<BaseResult<null>>} 操作结果
   */
  static unbindStore(): Promise<BaseResult<null>> {
    return request.del<BaseResult<null>>({
      url: '/auth/binding'
    })
  }

  /**
   * 登录用户绑定店铺
   * @param {string} storeNo - 店铺编号
   * @returns {Promise<BaseResult<BindStoreResponse>>} 操作结果
   */
  static bindStore(storeNo: string): Promise<BaseResult<BindStoreResponse>> {
    return request.put<BaseResult<BindStoreResponse>>({
      url: `/auth/binding/${storeNo}`
    })
  }

  /**
   * 获取租户下成员列表
   * @returns {Promise<BaseResult<TenantMember[]>>} 租户成员列表
   */
  static getTenantMemberList(): Promise<BaseResult<TenantMember[]>> {
    return request.get<BaseResult<TenantMember[]>>({
      url: '/auth/tenant/account/list'
    })
  }

  /**
   * 获取用户信息
   * @returns {Promise<BaseResult<UserInfo>>} 用户信息
   */
  static getUserInfo(): Promise<BaseResult<UserInfo>> {
    return request.get<BaseResult<UserInfo>>({
      url: '/auth/user/info'
    })
  }

  /**
   * 验证Token是否有效
   * @returns {Promise<BaseResult<{isLogin: boolean, userId?: string, tokenTimeout?: number}>>} Token验证结果
   */
  static verifyToken(): Promise<
    BaseResult<{ isLogin: boolean; userId?: string; tokenTimeout?: number }>
  > {
    return request.get<BaseResult<{ isLogin: boolean; userId?: string; tokenTimeout?: number }>>({
      url: '/api/auth/verify'
    })
  }

  /**
   * 获取当前登录用户信息
   * @returns {Promise<BaseResult<UserInfo>>} 当前用户信息
   */
  static getCurrentUser(): Promise<BaseResult<UserInfo>> {
    return request.get<BaseResult<UserInfo>>({
      url: '/api/auth/current-user'
    })
  }
  /**
   * 用户注册
   * @param {UserRegisterParams} params - 用户注册请求参数
   * @returns {Promise<BaseResult<string>>} 操作结果
   */
  static register(params: UserRegisterParams): Promise<BaseResult<string>> {
    return request.post<BaseResult<string>>({
      url: '/auth/register',
      data: params
    })
  }

  /**
   * 修改密码
   * @param {UpdatePasswordParams} data - 修改密码请求参数
   * @returns {Promise<BaseResult<null>>} 操作结果
   */
  static updatePassword(data: UpdatePasswordParams): Promise<BaseResult<null>> {
    return request.put<BaseResult<null>>({
      url: '/auth/user/password',
      data
    })
  }
  /**
   * 获取用户基本信息
   * @returns {Promise<BaseResult<UserBasic>>} 用户基本信息
   */
  static getUserBasic(): Promise<BaseResult<UserBasic>> {
    return request.get<BaseResult<UserBasic>>({
      url: '/auth/user/basic'
    })
  }

  /**
   * 修改用户基本信息
   * @param {UserBasicUpdateParams} params - 用户基本信息修改参数
   * @returns {Promise<BaseResult<string>>} 操作结果
   */
  static updateUserBasic(params: UserBasicUpdateParams): Promise<BaseResult<string>> {
    return request.put<BaseResult<string>>({
      url: '/auth/user/basic',
      data: params
    })
  }

  /**
   * 头像上传表单预签名
   * @param {AvatarUploadParams} params - 头像上传请求参数
   * @returns {Promise<BaseResult<string>>} 预签名信息
   */
  static getAvatarUploadSign(params: AvatarUploadParams): Promise<BaseResult<string>> {
    return request.post<BaseResult<string>>({
      url: '/auth/user/avatar',
      data: params
    })
  }
}
