# Vue 列表页面生成 Prompt

## 使用说明

用于生成标准的 Vue 列表页面，包含搜索、表格、操作按钮等常用功能。

## Prompt 模板

### 创建新的列表页面

```
请为 [模块名称] 创建一个完整的 Vue 列表页面，需要包含：

### 基础要求：
1. **技术栈**：Vue 3 + TypeScript + Composition API + Element Plus
2. **文件位置**：src/views/[模块名称]/index.vue
3. **样式**：使用 SCSS + UnoCSS

### 功能要求：
1. **数据展示**：
   - 使用 El-Table 展示数据列表
   - 包含分页功能（如果需要）
   - 支持加载状态显示

2. **搜索功能**：
   - 包含搜索表单
   - 支持多条件筛选
   - 使用 ArtSearchBar 组件（如果适用）

3. **操作功能**：
   - 新增按钮（打开新增弹窗/抽屉）
   - 编辑按钮（打开编辑弹窗/抽屉）
   - 删除按钮（确认删除操作）
   - 其他自定义操作按钮

4. **状态管理**：
   - 使用 ref/reactive 管理组件状态
   - 包含 loading 状态
   - 列表数据的响应式管理

### 数据结构：
[在此描述数据字段和结构]

### 表格列配置：
[在此描述需要显示的表格列]

### 搜索条件：
[在此描述搜索条件字段]

### 操作按钮：
[在此描述需要的操作按钮]

请遵循项目现有的代码规范，包括：
- 使用 Composition API script setup 语法
- 合理的组件拆分和复用
- 完善的错误处理和用户体验
- 统一的代码风格和命名规范
```

### 更新现有列表页面

```
请更新 [模块名称] 的列表页面，需要：

1. **新增功能**：
   [描述需要新增的功能]

2. **修改现有功能**：
   [描述需要修改的功能]

3. **优化建议**：
   [描述需要优化的地方]

请保持与现有代码风格一致。
```

## 代码结构模板

```vue
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { [相关图标] } from '@element-plus/icons-vue'
import { [ModuleName]Service } from '@/api/[moduleName]Api'
import { [ModuleName], [ModuleName]Status } from '@/api/model/[moduleName]Model'

// 响应式数据
const loading = ref(false)
const dataList = ref<[ModuleName][]>([])

// 查询参数
const queryParams = reactive({
  // 定义查询参数
})

// 获取数据列表
const getDataList = async () => {
  loading.value = true
  try {
    const { data } = await [ModuleName]Service.get[ModuleName]List(queryParams)
    dataList.value = data || []
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 其他方法...

onMounted(() => {
  getDataList()
})
</script>

<template>
  <!-- 页面模板 -->
</template>

<style lang="scss" scoped>
/* 页面样式 */
</style>
```

## 最佳实践

1. **性能优化**：使用 v-loading 指令显示加载状态
2. **用户体验**：操作前显示确认对话框
3. **错误处理**：合理的 try-catch 和错误提示
4. **代码复用**：抽取公共逻辑为 composables
5. **类型安全**：充分利用 TypeScript 类型检查
