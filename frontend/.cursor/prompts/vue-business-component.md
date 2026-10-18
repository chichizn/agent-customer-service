# Vue 业务组件生成 Prompt

## 使用说明

用于生成标准的 Vue 业务组件，包含 Props、Emits、插槽等功能。

## Prompt 模板

### 创建新的业务组件

```
请为 [组件名称] 创建一个 Vue 业务组件，需要包含：

### 基础要求：
1. **技术栈**：Vue 3 + TypeScript + Composition API + Element Plus
2. **文件位置**：
   - 公共组件：src/components/[组件名称]/index.vue
   - 页面组件：src/views/[页面名称]/components/[组件名称].vue
3. **样式**：使用 SCSS + UnoCSS

### 组件功能：
[在此描述组件的具体功能和用途]

### Props 定义：
[在此列出组件需要接收的 props 及其类型]

### Events 定义：
[在此列出组件需要发出的事件]

### 插槽定义：
[如果需要插槽，在此说明插槽的用途]

### 数据处理：
[说明组件内部的数据处理逻辑]

### 样式要求：
[说明组件的样式要求]

请遵循以下规范：
- 使用 defineProps、defineEmits、defineModel
- 组件名称使用 PascalCase
- 合理的类型定义和参数验证
- 完善的 JSDoc 注释
- 响应式设计（如果需要）
```

### 更新现有业务组件

```
请更新 [组件名称] 业务组件，需要：

1. **新增功能**：
   [描述需要新增的功能]

2. **Props 变更**：
   [描述 Props 的变更]

3. **Events 变更**：
   [描述 Events 的变更]

4. **样式调整**：
   [描述样式调整需求]

请保持向后兼容性，避免破坏性变更。
```

## 代码结构模板

```vue
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

// Props 接口定义
interface Props {
  // 定义 props 类型
}

// Props 默认值
const props = withDefaults(defineProps<Props>(), {
  // 设置默认值
})

// Emits 定义
const emit = defineEmits<{
  // 定义 emit 事件类型
}>()

// 组合式函数
const loading = ref(false)

// 计算属性
const computedValue = computed(() => {
  // 计算逻辑
})

// 监听器
watch(
  () => props.someProp,
  (newVal) => {
    // 监听逻辑
  }
)

// 方法
const handleMethod = () => {
  // 处理逻辑
}
</script>

<template>
  <div class="[component-name]">
    <!-- 组件模板 -->
  </div>
</template>

<style lang="scss" scoped>
.[component-name] {
  /* 组件样式 */
}
</style>
```

## 组件类型

### 1. 展示型组件

- **特点**：主要用于数据展示，较少交互
- **示例**：信息卡片、状态展示、数据可视化
- **重点**：Props 设计、样式美观

### 2. 交互型组件

- **特点**：包含用户交互，需要处理事件
- **示例**：表单控件、按钮组、操作面板
- **重点**：Events 设计、状态管理

### 3. 容器型组件

- **特点**：包含其他组件，提供布局和数据
- **示例**：列表容器、表格容器、弹窗容器
- **重点**：插槽设计、子组件通信

### 4. 业务型组件

- **特点**：封装特定业务逻辑
- **示例**：用户选择器、文件上传、搜索组件
- **重点**：业务逻辑封装、API 调用

## 最佳实践

1. **单一职责**：每个组件应该有明确的单一职责
2. **Props 验证**：为所有 Props 提供类型定义和默认值
3. **事件命名**：使用动词形式，如 `change`、`submit`、`cancel`
4. **样式隔离**：使用 `scoped` 避免样式污染
5. **可访问性**：考虑键盘导航和屏幕阅读器支持
6. **文档注释**：为组件和主要方法添加 JSDoc 注释
7. **测试友好**：为关键元素添加 `data-testid` 属性
