# API Model 类型定义目录

## 目录说明

此目录存放所有 API 接口的 TypeScript 类型定义文件。每个 Model 文件与对应的 API 文件配对。

## 命名规范

### 文件命名

Model 文件命名遵循以下规则：

| API 文件 | Model 文件 | 说明 |
|---------|-----------|------|
| `auth.ts` | `authModel.ts` | 认证相关 |
| `user.ts` | `userModel.ts` | 用户管理 |
| `role.ts` | `roleModel.ts` | 角色管理 |
| `menu.ts` | `menuModel.ts` | 菜单管理 |
| `system-manage.ts` | `systemManageModel.ts` | 系统管理 |
| `dashboard.ts` | `dashboardModel.ts` | 仪表盘 |

**转换规则：**
1. API 文件使用 kebab-case（如 `system-manage.ts`）
2. Model 文件使用 camelCase + Model 后缀（如 `systemManageModel.ts`）
3. 转换方法：将 kebab-case 转为 camelCase，然后添加 `Model` 后缀

### 类型命名

使用 PascalCase 命名所有导出的类型：

```typescript
// ✅ 推荐
export interface UserListItem {}
export type UserList = PaginatedResponse<UserListItem>
export type UserSearchParams = {}

// ❌ 不推荐
export interface userListItem {}
export type user_list = {}
```

## 文件结构

### 基本结构

```typescript
/**
 * [模块名]模块类型定义
 *
 * @module api/model/[模块名]Model
 */

import type { PaginatedResponse, CommonSearchParams } from '@/types/common/response'

// ========== [资源名]相关 ==========

/** [资源名]列表响应 */
export type [资源名]List = PaginatedResponse<[资源名]ListItem>

/** [资源名]列表项 */
export interface [资源名]ListItem {
  // 字段定义...
}

/** [资源名]搜索参数 */
export type [资源名]SearchParams = Partial<
  Pick<[资源名]ListItem, 'field1' | 'field2'> &
    CommonSearchParams
>

/** 创建[资源名]参数 */
export interface Create[资源名]Params {
  // 字段定义...
}

/** 更新[资源名]参数 */
export type Update[资源名]Params = Partial<Create[资源名]Params>
```

### 示例：用户管理

```typescript
// userModel.ts
import type { PaginatedResponse, CommonSearchParams } from '@/types/common/response'

/** 用户列表响应 */
export type UserList = PaginatedResponse<UserListItem>

/** 用户列表项 */
export interface UserListItem {
  id: number
  userName: string
  email: string
  // ...其他字段
}

/** 用户搜索参数 */
export type UserSearchParams = Partial<
  Pick<UserListItem, 'userName' | 'email'> &
    CommonSearchParams
>

/** 创建用户参数 */
export interface CreateUserParams {
  userName: string
  password: string
  email: string
}

/** 更新用户参数 */
export type UpdateUserParams = Partial<Omit<CreateUserParams, 'password'>>
```

## 使用方式

### 在 API 文件中导入

```typescript
// src/api/user.ts
import request from '@/utils/http'
import type {
  UserList,
  UserListItem,
  UserSearchParams,
  CreateUserParams,
  UpdateUserParams
} from './model/userModel'

export function fetchGetUserList(params: UserSearchParams) {
  return request.get<UserList>({
    url: '/api/user/list',
    params
  })
}
```

### 在组件中导入

```vue
<script setup lang="ts">
import { fetchGetUserList } from '@/api/user'
import type { UserListItem, UserSearchParams } from '@/api/model/userModel'

const tableData = ref<UserListItem[]>([])
const searchParams = reactive<UserSearchParams>({
  current: 1,
  size: 20
})
</script>
```

## 类型定义规范

### 1. 使用 export 导出

所有类型必须使用 `export` 导出：

```typescript
// ✅ 正确
export interface UserInfo {}
export type UserList = PaginatedResponse<UserInfo>

// ❌ 错误（不导出无法被使用）
interface UserInfo {}
type UserList = PaginatedResponse<UserInfo>
```

### 2. 继承通用类型

列表响应和搜索参数应继承通用类型：

```typescript
import type { PaginatedResponse, CommonSearchParams } from '@/types/common/response'

// 列表响应
export type UserList = PaginatedResponse<UserListItem>

// 搜索参数
export type UserSearchParams = Partial<
  Pick<UserListItem, 'field1' | 'field2'> &
    CommonSearchParams
>
```

### 3. 参数类型复用

使用 TypeScript 工具类型避免重复定义：

```typescript
// 基础参数
export interface CreateUserParams {
  userName: string
  password: string
  email: string
}

// 更新参数（复用创建参数，但密码字段可选或移除）
export type UpdateUserParams = Partial<Omit<CreateUserParams, 'password'>>

// 详情（继承列表项）
export interface UserDetail extends UserListItem {
  permissions: string[]
  roles: RoleInfo[]
}
```

### 4. 添加注释

为每个类型添加 JSDoc 注释：

```typescript
/** 用户列表项 */
export interface UserListItem {
  /** 用户 ID */
  id: number
  /** 用户名 */
  userName: string
  /** 状态：1-启用 2-禁用 */
  status: '1' | '2'
}
```

## 注意事项

### 1. 避免循环依赖

Model 文件只导入通用类型，不导入其他 API 或 Model：

```typescript
// ✅ 正确
import type { PaginatedResponse, CommonSearchParams } from '@/types/common/response'

// ❌ 错误（避免导入其他业务模块）
import type { RoleInfo } from './roleModel'
import { fetchGetRoleList } from '../role'
```

### 2. 类型隔离

每个模块的类型定义独立，避免类型污染：

```typescript
// userModel.ts - 只定义用户相关类型
export interface UserInfo {}
export type UserList = {}

// roleModel.ts - 只定义角色相关类型
export interface RoleInfo {}
export type RoleList = {}
```

### 3. 保持简单

Model 文件只定义类型，不包含业务逻辑：

```typescript
// ✅ 正确 - 只定义类型
export interface UserInfo {
  id: number
  userName: string
}

// ❌ 错误 - 包含业务逻辑
export interface UserInfo {
  id: number
  userName: string
}

export function transformUserInfo(data: any): UserInfo {
  // 业务逻辑应该放在其他地方
  return { ... }
}
```

## 常见类型定义

### 列表响应

```typescript
export type [资源名]List = PaginatedResponse<[资源名]ListItem>
```

### 搜索参数

```typescript
export type [资源名]SearchParams = Partial<
  Pick<[资源名]ListItem, 'field1' | 'field2'> &
    CommonSearchParams
>
```

### 创建参数

```typescript
export interface Create[资源名]Params {
  field1: string
  field2: number
  // ...
}
```

### 更新参数

```typescript
export type Update[资源名]Params = Partial<Create[资源名]Params>
```

### 详情信息

```typescript
export interface [资源名]Detail extends [资源名]ListItem {
  // 额外的详情字段
}
```

## 迁移指南

### 从全局 api.d.ts 迁移

如果项目中存在全局的 `api.d.ts` 文件，可以按以下步骤迁移：

1. 为每个模块创建对应的 Model 文件
2. 将类型从 `declare namespace Api` 中提取出来
3. 使用 `export` 导出类型
4. 更新 API 文件的导入语句
5. 更新组件中的类型引用
6. 验证类型检查通过
7. 删除旧的全局类型定义

### 示例

```typescript
// 旧方式 - api.d.ts
declare namespace Api {
  namespace Auth {
    interface LoginParams {
      userName: string
      password: string
    }
  }
}

// 新方式 - authModel.ts
export interface LoginParams {
  userName: string
  password: string
}
```

```typescript
// 旧的导入方式
// 无需导入，直接使用 Api.Auth.LoginParams

// 新的导入方式
import type { LoginParams } from '@/api/model/authModel'
```

## 最佳实践

1. ✅ **一一对应**：每个 API 文件对应一个 Model 文件
2. ✅ **类型安全**：为所有参数和返回值定义完整类型
3. ✅ **代码复用**：使用 TypeScript 工具类型避免重复定义
4. ✅ **清晰注释**：为类型和字段添加详细注释
5. ✅ **避免依赖**：Model 文件只导入通用类型
6. ✅ **命名规范**：遵循统一的命名规则
7. ✅ **保持简单**：只定义类型，不包含业务逻辑

