/**
 * @file userModel.ts
 * @description 用户认证相关的数据模型定义
 * @date 2025-01-11
 */

// ===== 枚举定义 =====

/**
 * OTP验证码类型枚举
 */
export enum OtpType {
  /** 登录双因子验证 */
  LOGIN_2FA = 'LOGIN_2FA',
  /** 登录手机验证 */
  LOGIN_PHONE = 'LOGIN_PHONE',
  /** 重置密码 */
  RESET_PASSWORD = 'RESET_PASSWORD'
}

// ===== 数据模型接口 =====

/**
 * 用户登录响应数据（匹配后端LoginResponse.java）
 * @interface LoginResponse
 */
export interface LoginResponse {
  /** 访问令牌（Token） */
  token: string
  /** Token类型（默认：Bearer） */
  tokenType: string
  /** Token过期时间（秒） */
  expiresIn: number
  /** 用户ID */
  userId: string
  /** 用户名 */
  username: string
  /** 昵称 */
  nickname: string
  /** 头像 */
  avatar: string | null
  /** 是否需要双因子验证 */
  requiresTwoFactor?: boolean
  /** 临时认证token（双因子认证时使用） */
  preAuthToken?: string
  /** 手机号（双因子认证时使用） */
  phone?: string
}

/**
 * JWT Token解码后的用户信息
 * @interface JwtPayload
 */
export interface JwtPayload {
  /** 用户账号（手机号） */
  acc: string
  /** 用户编号 */
  sub: string
  /** 用户昵称 */
  usn: string
  /** 租户编号（企业编号） */
  tnn: string
  /** 秘钥版本 */
  sver: string
  /** 过期时间（秒级时间戳） */
  exp: number
  /** 颁发时间（秒级时间戳） */
  iat: number
  /** token id */
  jti: string
}

/**
 * 用户基本信息
 * @interface UserBasic
 */
export interface UserBasic {
  /** 昵称 */
  nickName: string
  /** 手机号 */
  phone: string
  /** 邮箱 */
  email: string
  /** 头像链接 */
  avatarLink: string
}

/**
 * 租户成员信息
 * @interface TenantMember
 */
export interface TenantMember {
  /** 用户编号 */
  userNo: string
  /** 账号 */
  account: string
  /** 用户名 */
  userName: string
}

// ===== 请求参数接口 =====

/**
 * 用户登录请求参数（匹配后端LoginRequest.java）
 * @interface LoginParams
 */
export interface LoginParams {
  /** 用户名 */
  username: string
  /** 密码 */
  password: string
}

/**
 * 双因子验证登录请求参数
 * @interface Verify2FAParams
 */
export interface Verify2FAParams {
  /** 临时认证token */
  preAuthToken: string
  /** 用户验证码 */
  otpCode: string
  /** 是否为客户端登录 */
  isClient: boolean
}

/**
 * 发送验证码请求参数
 * @interface SendOtpParams
 */
export interface SendOtpParams {
  /** 临时认证token（双因子认证模式必需） */
  preAuthToken?: string
  /** 手机号 */
  phone: string
  /** 验证码类型 */
  otpType: string
}

/**
 * 手机号验证码登录请求参数
 * @interface SmsLoginParams
 */
export interface SmsLoginParams {
  /** 临时认证token */
  preAuthToken: string
  /** 验证码 */
  otpCode: string
  /** 是否为客户端登录 */
  isClient: boolean
}

/**
 * 用户注册请求参数
 * @interface UserRegisterParams
 */
export interface UserRegisterParams {
  /** 手机号（作为账号） */
  phone: string
  /** 密码 */
  password: string
  /** 确认密码 */
  confirmPassword: string
  /** 昵称 */
  nickName: string
  /** 租户编号（企业编号） */
  tenantNo: string
  /** 头像链接（可选） */
  avatarLink?: string
}

/**
 * 用户基本信息修改参数
 * @interface UserBasicUpdateParams
 */
export interface UserBasicUpdateParams {
  /** 昵称 */
  nickName?: string
  /** 头像链接 */
  avatarLink?: string
}

/**
 * 头像上传请求参数
 * @interface AvatarUploadParams
 */
export interface AvatarUploadParams {
  /** 文件名 */
  originFileName: string
}

/**
 * 修改密码请求参数
 * @interface UpdatePasswordParams
 */
export interface UpdatePasswordParams {
  /** 旧密码 */
  oldPassword: string
  /** 新密码 */
  password: string
}

/**
 * 绑定店铺响应数据
 * @interface BindStoreResponse
 */
export interface BindStoreResponse {
  /** JWT Token */
  token: string
  /** 刷新Token */
  refreshToken: string
}
