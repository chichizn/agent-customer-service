# API 接口模块生成 Prompt

## 使用说明

用于生成或更新 API 接口模块，包括接口定义和类型定义。遵循项目统一的 API 开发规范。

## Prompt 模板

### 创建新的 API 接口模块

```
请为 [模块名称] 创建完整的 API 接口模块，需要包含：

1. **API 接口文件** (src/api/[模块名称].ts)：
   - 使用函数导出方式
   - 函数命名统一使用 `fetch` 前缀：
     * GET 请求：fetchGet + 资源名（如 fetchGetUserList、fetchGetUserInfo）
     * POST 创建：fetchCreate + 资源名（如 fetchCreateUser）
     * PUT 更新：fetchUpdate + 资源名（如 fetchUpdateUser）
     * DELETE 删除：fetchDelete + 资源名（如 fetchDeleteUser）
     * 其他操作：fetch + 动词 + 资源名（如 fetchEnableUser、fetchBatchDeleteUsers）
   - 每个函数添加完整的 JSDoc 注释（包含 @param 和 @returns）
   - 使用 request.get/post/put/delete 方法
   - 正确使用泛型指定返回类型
   - 从对应的 Model 文件导入类型定义

2. **类型定义文件** (src/api/model/[模块名称]Model.ts)：
   - 与 API 文件同名，添加 Model 后缀（如 auth.ts -> authModel.ts）
   - 使用 export interface 或 export type 导出类型
   - 定义接口类型（如 UserListItem）
   - 定义列表响应类型（如 UserList = Api.Common.PaginatedResponse<UserListItem>）
   - 定义搜索参数类型（如 UserSearchParams）
   - 定义创建参数类型（如 CreateUserParams）
   - 定义更新参数类型（如 UpdateUserParams）
   - 每个类型添加 JSDoc 注释说明

### 业务需求：
[在此描述具体的业务需求，包括字段、功能等]

### 接口规范：
- 查询列表：GET /[模块]/list
- 查询详情：GET /[模块]/{id}
- 创建：POST /[模块]/create
- 更新：PUT /[模块]/{id}
- 删除：DELETE /[模块]/{id}
- 批量删除：DELETE /[模块]/batch

### 示例代码结构：

**API 文件示例 (src/api/user.ts)：**
```typescript
import request from '@/utils/http'
import type { UserList, UserListItem, UserSearchParams, CreateUserParams, UpdateUserParams } from './model/userModel'

/**
 * 获取用户列表
 * @param params 搜索参数
 * @returns 用户列表数据
 */
export function fetchGetUserList(params: UserSearchParams) {
  return request.get<UserList>({
    url: '/api/user/list',
    params
  })
}

/**
 * 创建用户
 * @param data 用户信息
 * @returns void
 */
export function fetchCreateUser(data: CreateUserParams) {
  return request.post<void>({
    url: '/api/user/create',
    data
  })
}

/**
 * 更新用户
 * @param id 用户 ID
 * @param data 用户信息
 * @returns void
 */
export function fetchUpdateUser(id: number, data: UpdateUserParams) {
  return request.put<void>({
    url: `/api/user/${id}`,
    data
  })
}
```

**类型定义示例 (src/api/model/userModel.ts)：**
```typescript
import type { PaginatedResponse, CommonSearchParams } from '@/types/common/response'

/** 列表响应 */
export type UserList = PaginatedResponse<UserListItem>

/** 用户列表项 */
export interface UserListItem {
  id: number
  userName: string
  nickName: string
  email: string
  phone: string
  avatar?: string
  status: '1' | '2' // 1-启用 2-禁用
  userGender: '1' | '2' // 1-男 2-女
  userRoles: string[]
  createBy: string
  createTime: string
  updateBy: string
  updateTime: string
}

/** 搜索参数 */
export type UserSearchParams = Partial<
  Pick<UserListItem, 'userName' | 'email' | 'phone' | 'status'> &
    CommonSearchParams
>

/** 创建用户参数 */
export interface CreateUserParams {
  userName: string
  password: string
  nickName: string
  email: string
  phone: string
  userGender: '1' | '2'
  roleIds: number[]
}

/** 更新用户参数 */
export type UpdateUserParams = Partial<Omit<CreateUserParams, 'password'>> & {
  status?: '1' | '2'
}
```

请严格遵循项目现有的代码规范和文件命名规则。
```

### 更新现有 API 接口模块

```
请更新 [模块名称] 的 API 接口模块，需要：

1. **更新 API 接口文件** (src/api/[模块名称].ts)：
   [描述需要更新的具体内容]
   - 新增接口：[列出需要新增的接口]
   - 修改接口：[列出需要修改的接口及修改内容]
   - 删除接口：[列出需要删除的接口]

2. **更新类型定义** (src/api/models/[模块名称]Model.ts)：
   [描述类型定义的更新内容]
   - 新增字段：[列出需要新增的字段]
   - 修改字段：[列出需要修改的字段]
   - 删除字段：[列出需要删除的字段]

请保持与现有代码风格一致，确保类型定义完整准确。
```

### 添加特殊功能接口

```
请为 [模块名称] 添加以下特殊功能接口：

1. **文件上传**（如需要）：
   - 函数名：fetchUpload[资源名]
   - 使用 FormData 传递文件
   - 设置 Content-Type: multipart/form-data

2. **文件下载**（如需要）：
   - 函数名：fetchDownload[资源名]
   - 设置 responseType: 'blob'

3. **批量操作**（如需要）：
   - 函数名：fetchBatch[操作][资源名]
   - 接收 ID 数组参数

4. **状态切换**（如需要）：
   - 函数名：fetch[Enable/Disable/Toggle][资源名]

请根据实际业务需求添加相应的接口和类型定义。
```

## 代码规范要点

### 命名规范

1. **文件命名**：
   - API 文件：使用 kebab-case（如 `system-manage.ts`、`user.ts`）
   - Model 文件：使用 camelCase + Model 后缀（如 `systemManageModel.ts`、`userModel.ts`）
2. **函数命名**：
   - 统一使用 `fetch` 前缀
   - 使用 camelCase 驼峰命名
   - GET 请求：`fetchGet` + 资源名
   - POST 创建：`fetchCreate` + 资源名
   - PUT 更新：`fetchUpdate` + 资源名
   - DELETE 删除：`fetchDelete` + 资源名
   - 其他操作：`fetch` + 动词 + 资源名
3. **类型命名**：使用 PascalCase（如 `UserListItem`、`CreateUserParams`）

### 文件结构

1. **API 文件位置**：`src/api/[模块名].ts`
2. **类型定义位置**：`src/api/model/[模块名]Model.ts`（与 API 文件对应）
3. **导入顺序**：
   - 第三方库
   - @/ 别名导入
   - 相对路径导入（Model 文件）

### 类型定义规范

1. **使用 export 导出**：每个类型使用 `export interface` 或 `export type` 导出
2. **列表响应类型**：使用 `PaginatedResponse<T>` 泛型
3. **搜索参数类型**：继承 `CommonSearchParams`
4. **参数类型复用**：使用 `Partial`、`Pick`、`Omit` 等工具类型
5. **避免循环依赖**：Model 文件只导入通用类型，不导入其他 API 或 Model

### 请求方法使用

1. **GET 请求**：使用 `params` 传递查询参数
2. **POST 请求**：使用 `data` 传递请求体
3. **PUT 请求**：使用 `data` 传递请求体
4. **DELETE 请求**：使用 `data` 传递请求体（如需要）
5. **泛型指定**：明确指定返回类型泛型 `request.get<T>()`

### 注释规范

1. **函数注释**：使用 JSDoc 格式
   - `@param` 参数说明
   - `@returns` 返回值说明
2. **类型注释**：使用 `/** 注释内容 */` 格式
3. **行内注释**：使用 `//` 对特殊字段进行说明

### 错误处理

1. **API 层**：只定义接口，不处理错误
2. **全局拦截器**：统一处理 HTTP 错误和业务错误
3. **组件层**：处理特定的业务逻辑错误

## 最佳实践

1. **统一命名**：所有 API 函数使用 `fetch` 前缀
2. **类型安全**：为所有请求参数和响应数据定义完整类型
3. **代码复用**：使用 TypeScript 工具类型避免重复定义
4. **清晰注释**：为接口和类型添加详细的 JSDoc 注释
5. **模块划分**：按业务模块组织 API 文件，Model 文件与 API 文件一一对应
6. **类型隔离**：每个模块的类型定义独立在 Model 文件中，避免类型污染
7. **RESTful 风格**：遵循 RESTful API 设计规范
8. **避免循环依赖**：Model 文件只导入通用类型，不导入业务模块类型
