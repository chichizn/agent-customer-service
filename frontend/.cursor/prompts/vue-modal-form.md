# Vue 新增/编辑弹窗生成 Prompt

## 使用说明

用于生成标准的新增/编辑弹窗或抽屉组件，支持表单验证和数据提交。

## Prompt 模板

### 创建新的弹窗/抽屉组件

```
请为 [模块名称] 创建一个新增/编辑弹窗组件，需要包含：

### 基础要求：
1. **技术栈**：Vue 3 + TypeScript + Composition API + Element Plus
2. **文件位置**：src/views/[模块名称]/components/[组件名称].vue
3. **组件类型**：
   - [ ] Dialog 对话框
   - [ ] Drawer 抽屉
4. **样式**：使用 SCSS + UnoCSS

### 功能要求：
1. **双向绑定**：
   - 使用 defineModel 管理 visible 状态
   - 支持外部控制显示/隐藏

2. **表单功能**：
   - 表单字段验证
   - 新增和编辑模式切换
   - 表单重置功能
   - 提交和取消操作

3. **数据处理**：
   - 接收编辑数据（editItem prop）
   - 表单数据初始化和重置
   - API 调用和错误处理

### 表单字段：
[在此描述表单包含的字段及其类型、验证规则]

### 验证规则：
[在此描述各字段的验证规则]

### 特殊功能：
[在此描述特殊功能，如文件上传、级联选择等]

### Props 定义：
- editItem?: [DataType] | null - 编辑的数据项

### Events 定义：
- success: [] - 操作成功事件

请遵循以下规范：
- visible 必须使用 defineModel 双向绑定
- 表单使用 ElForm + ElFormItem 组织
- 包含完整的表单验证
- 操作按钮包含取消和确认
- 合理的 loading 状态管理
```

### 更新现有弹窗组件

```
请更新 [组件名称] 弹窗组件，需要：

1. **新增字段**：
   [描述需要新增的表单字段]

2. **修改验证规则**：
   [描述需要修改的验证规则]

3. **功能调整**：
   [描述需要调整的功能]

4. **样式更新**：
   [描述样式更新需求]

请保持与现有代码风格一致。
```

## 代码结构模板

### Dialog 版本

```vue
<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { ElMessage, ElDialog, ElForm, ElFormItem, ElButton } from 'element-plus'
import { [ModuleName]Service } from '@/api/[moduleName]Api'
import { [ModuleName], [ModuleName]Params } from '@/api/model/[moduleName]Model'

// Props 接口定义
interface Props {
  editItem?: [ModuleName] | null
}

const props = withDefaults(defineProps<Props>(), {
  editItem: null
})

// 双向绑定 visible
const visible = defineModel<boolean>('visible', { default: false })

// Emits
const emit = defineEmits<{
  success: []
}>()

// 响应式数据
const loading = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive<[ModuleName]Params>({
  // 初始化表单数据
})

// 表单验证规则
const formRules = {
  // 定义验证规则
}

// 计算属性
const dialogTitle = computed(() => props.editItem ? '编辑[模块名称]' : '新增[模块名称]')
const isEditMode = computed(() => !!props.editItem)

// 监听编辑数据变化
watch(
  () => props.editItem,
  (newVal) => {
    if (newVal) {
      // 填充编辑数据
      Object.assign(formData, newVal)
    } else {
      // 重置表单数据
      resetForm()
    }
  },
  { immediate: true }
)

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    // 重置为初始值
  })
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

// 处理关闭
const handleClose = () => {
  resetForm()
  visible.value = false
}

// 处理提交
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    if (isEditMode.value) {
      await [ModuleName]Service.update[ModuleName](props.editItem!.id, formData)
      ElMessage.success('编辑成功')
    } else {
      await [ModuleName]Service.add[ModuleName](formData)
      ElMessage.success('新增成功')
    }

    emit('success')
    handleClose()
  } catch (error) {
    console.error('操作失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <ElDialog v-model="visible" :title="dialogTitle" width="600px" @close="handleClose">
    <ElForm ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <!-- 表单项 -->
    </ElForm>

    <template #footer>
      <ElButton @click="handleClose">取消</ElButton>
      <ElButton type="primary" :loading="loading" @click="handleSubmit">
        {{ isEditMode ? '更新' : '确定' }}
      </ElButton>
    </template>
  </ElDialog>
</template>

<style lang="scss" scoped>
/* 弹窗样式 */
</style>
```

### Drawer 版本

```vue
<template>
  <ElDrawer v-model="visible" :title="drawerTitle" size="600px" @close="handleClose">
    <!-- 表单内容 -->

    <template #footer>
      <div class="drawer-footer">
        <ElButton @click="handleClose">取消</ElButton>
        <ElButton type="primary" :loading="loading" @click="handleSubmit">
          {{ isEditMode ? '更新' : '确定' }}
        </ElButton>
      </div>
    </template>
  </ElDrawer>
</template>

<style lang="scss" scoped>
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid var(--el-border-color);
}
</style>
```

## 常用表单字段

### 基础字段

- **文本输入**：ElInput
- **数字输入**：ElInputNumber
- **下拉选择**：ElSelect + ElOption
- **单选框组**：ElRadioGroup + ElRadio
- **复选框组**：ElCheckboxGroup + ElCheckbox
- **日期选择**：ElDatePicker
- **时间选择**：ElTimePicker

### 高级字段

- **文件上传**：ElUpload 或自定义上传组件
- **富文本编辑**：第三方编辑器组件
- **级联选择**：ElCascader
- **标签输入**：ElInputTag
- **开关**：ElSwitch

## 最佳实践

1. **数据管理**：使用 reactive 管理表单数据
2. **验证规则**：为必填项和格式校验添加规则
3. **用户体验**：提供清晰的操作反馈和加载状态
4. **错误处理**：合理处理表单验证和 API 错误
5. **性能优化**：大表单考虑字段懒加载
6. **可访问性**：正确使用 label 和 aria 属性
