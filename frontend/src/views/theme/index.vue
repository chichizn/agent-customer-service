<template>
  <div class="theme-preview-container">
    <el-scrollbar class="scrollbar-wrapper">
      <div class="preview-content">
        <div class="page-header">
          <h1 class="page-title">Element UI 主题预览</h1>
          <div class="size-selector">
            <el-radio-group v-model="currentSize">
              <el-radio label="large">大号</el-radio>
              <el-radio label="default">默认</el-radio>
              <el-radio label="small">小号</el-radio>
            </el-radio-group>
          </div>
        </div>

        <el-config-provider :size="currentSize">
          <!-- 按钮组件 -->
          <div class="section">
            <h2 class="section-title">按钮 Buttons</h2>
            <div class="component-group">
              <h3>基础按钮</h3>
              <div class="button-row">
                <el-button>默认按钮</el-button>
                <el-button type="primary">主要按钮</el-button>
                <el-button type="success">成功按钮</el-button>
                <el-button type="info">信息按钮</el-button>
                <el-button type="warning">警告按钮</el-button>
                <el-button type="danger">危险按钮</el-button>
              </div>

              <h3>朴素按钮</h3>
              <div class="button-row">
                <el-button plain>朴素按钮</el-button>
                <el-button type="primary" plain>主要按钮</el-button>
                <el-button type="success" plain>成功按钮</el-button>
                <el-button type="info" plain>信息按钮</el-button>
                <el-button type="warning" plain>警告按钮</el-button>
                <el-button type="danger" plain>危险按钮</el-button>
              </div>

              <h3>图标按钮</h3>
              <div class="button-row">
                <el-button :icon="Edit" circle></el-button>
                <el-button type="primary" :icon="Search" circle></el-button>
                <el-button type="success" :icon="Check" circle></el-button>
                <el-button type="info" :icon="Message" circle></el-button>
                <el-button type="warning" :icon="Star" circle></el-button>
                <el-button type="danger" :icon="Delete" circle></el-button>
              </div>

              <h3>链接按钮</h3>
              <div class="button-row">
                <el-button link>朴素按钮</el-button>
                <el-button type="primary" link>主要按钮</el-button>
                <el-button type="success" link>成功按钮</el-button>
                <el-button type="info" link>信息按钮</el-button>
                <el-button type="warning" link>警告按钮</el-button>
                <el-button type="danger" link>危险按钮</el-button>
              </div>
            </div>
          </div>

          <!-- 链接组件 -->
          <div class="section">
            <h2 class="section-title">链接 Links</h2>
            <div class="component-group">
              <div class="link-row">
                <el-link>默认链接</el-link>
                <el-link type="primary">主要链接</el-link>
                <el-link type="success">成功链接</el-link>
                <el-link type="info">信息链接</el-link>
                <el-link type="warning">警告链接</el-link>
                <el-link type="danger">危险链接</el-link>
              </div>
              <div class="link-row">
                <el-link disabled>禁用链接</el-link>
                <el-link :underline="false">无下划线</el-link>
                <el-link :icon="Edit">编辑</el-link>
                <el-link :icon="View">查看</el-link>
              </div>
            </div>
          </div>

          <!-- 表单组件 -->
          <div class="section">
            <h2 class="section-title">表单 Forms</h2>
            <div class="component-group">
              <el-form :model="form" label-width="120px" class="demo-form">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="输入框">
                      <el-input v-model="form.input" placeholder="请输入内容"></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="密码框">
                      <el-input
                        v-model="form.password"
                        type="password"
                        placeholder="请输入密码"
                        show-password
                      ></el-input>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="选择器">
                      <el-select v-model="form.select" placeholder="请选择">
                        <el-option label="选项一" value="option1"></el-option>
                        <el-option label="选项二" value="option2"></el-option>
                        <el-option label="选项三" value="option3"></el-option>
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="级联选择">
                      <el-cascader
                        v-model="form.cascader"
                        :options="cascaderOptions"
                        placeholder="请选择"
                      ></el-cascader>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="单选框">
                      <el-radio-group v-model="form.radio">
                        <el-radio value="radio1">选项一</el-radio>
                        <el-radio value="radio2">选项二</el-radio>
                        <el-radio value="radio3">选项三</el-radio>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="复选框">
                      <el-checkbox-group v-model="form.checkbox">
                        <el-checkbox value="checkbox1">选项一</el-checkbox>
                        <el-checkbox value="checkbox2">选项二</el-checkbox>
                        <el-checkbox value="checkbox3">选项三</el-checkbox>
                      </el-checkbox-group>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="日期选择">
                      <el-date-picker
                        v-model="form.date"
                        type="date"
                        placeholder="请选择日期"
                      ></el-date-picker>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="时间选择">
                      <el-time-picker v-model="form.time" placeholder="请选择时间"></el-time-picker>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item label="开关">
                  <el-switch v-model="form.switch"></el-switch>
                </el-form-item>

                <el-form-item label="滑块">
                  <el-slider v-model="form.slider"></el-slider>
                </el-form-item>

                <el-form-item label="评分">
                  <el-rate v-model="form.rate"></el-rate>
                </el-form-item>

                <el-form-item label="文本域">
                  <el-input
                    v-model="form.textarea"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入内容"
                  ></el-input>
                </el-form-item>

                <el-form-item>
                  <el-button type="primary">提交</el-button>
                  <el-button>重置</el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>

          <!-- 数据展示组件 -->
          <div class="section">
            <h2 class="section-title">数据展示 Data Display</h2>

            <!-- 标签 -->
            <div class="component-group">
              <h3>标签 Tags</h3>
              <div class="tag-row">
                <el-tag>标签一</el-tag>
                <el-tag type="success">标签二</el-tag>
                <el-tag type="info">标签三</el-tag>
                <el-tag type="warning">标签四</el-tag>
                <el-tag type="danger">标签五</el-tag>
              </div>
              <div class="tag-row">
                <el-tag closable>可关闭</el-tag>
                <el-tag type="success" closable>可关闭</el-tag>
                <el-tag type="info" closable>可关闭</el-tag>
                <el-tag type="warning" closable>可关闭</el-tag>
                <el-tag type="danger" closable>可关闭</el-tag>
              </div>
            </div>

            <!-- 进度条 -->
            <div class="component-group">
              <h3>进度条 Progress</h3>
              <el-progress :percentage="50"></el-progress>
              <el-progress :percentage="70" status="success"></el-progress>
              <el-progress :percentage="80" status="warning"></el-progress>
              <el-progress :percentage="90" status="exception"></el-progress>
            </div>

            <!-- 表格 -->
            <div class="component-group">
              <h3>表格 Table</h3>
              <el-table :data="tableData" style="width: 100%" border>
                <el-table-column prop="id" label="ID" width="80"></el-table-column>
                <el-table-column prop="name" label="姓名" width="120"></el-table-column>
                <el-table-column prop="email" label="邮箱"></el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="scope">
                    <el-tag :type="scope.row.status === '正常' ? 'success' : 'danger'">
                      {{ scope.row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200">
                  <template #default>
                    <el-button type="primary" size="small">编辑</el-button>
                    <el-button type="danger" size="small">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- 反馈组件 -->
          <div class="section">
            <h2 class="section-title">反馈 Feedback</h2>
            <div class="component-group">
              <div class="button-row">
                <el-button @click="showAlert">警告</el-button>
                <el-button @click="showConfirm">确认</el-button>
                <el-button @click="showPrompt">提示</el-button>
                <el-button @click="showMessage">消息</el-button>
                <el-button @click="showNotification">通知</el-button>
              </div>

              <h3>加载状态</h3>
              <div class="loading-demo">
                <el-button v-loading="loading" @click="toggleLoading"> 点击加载 </el-button>
              </div>
            </div>
          </div>

          <!-- 导航组件 -->
          <div class="section">
            <h2 class="section-title">导航 Navigation</h2>

            <!-- 分页 -->
            <div class="component-group">
              <h3>分页 Pagination</h3>
              <el-pagination
                v-model:current-page="currentPage"
                :page-size="10"
                :total="100"
                layout="prev, pager, next, jumper"
              ></el-pagination>
            </div>

            <!-- 面包屑 -->
            <div class="component-group">
              <h3>面包屑 Breadcrumb</h3>
              <el-breadcrumb separator="/">
                <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
                <el-breadcrumb-item>活动管理</el-breadcrumb-item>
                <el-breadcrumb-item>活动列表</el-breadcrumb-item>
                <el-breadcrumb-item>活动详情</el-breadcrumb-item>
              </el-breadcrumb>
            </div>

            <!-- 步骤条 -->
            <div class="component-group">
              <h3>步骤条 Steps</h3>
              <el-steps :active="1" finish-status="success">
                <el-step title="已完成"></el-step>
                <el-step title="进行中"></el-step>
                <el-step title="步骤3"></el-step>
              </el-steps>
            </div>
          </div>

          <!-- 其他组件 -->
          <div class="section">
            <h2 class="section-title">其他 Others</h2>

            <!-- 卡片 -->
            <div class="component-group">
              <h3>卡片 Card</h3>
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-card class="box-card">
                    <template #header>
                      <div class="card-header">
                        <span>卡片标题</span>
                        <el-button class="button" text>操作按钮</el-button>
                      </div>
                    </template>
                    <div class="text item">列表内容 1</div>
                    <div class="text item">列表内容 2</div>
                    <div class="text item">列表内容 3</div>
                    <div class="text item">列表内容 4</div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card class="box-card" shadow="hover">
                    <template #header>
                      <div class="card-header">
                        <span>悬停阴影</span>
                      </div>
                    </template>
                    <div class="text item">列表内容 1</div>
                    <div class="text item">列表内容 2</div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card class="box-card" shadow="never">
                    <template #header>
                      <div class="card-header">
                        <span>从不显示阴影</span>
                      </div>
                    </template>
                    <div class="text item">列表内容 1</div>
                    <div class="text item">列表内容 2</div>
                  </el-card>
                </el-col>
              </el-row>
            </div>

            <!-- 折叠面板 -->
            <div class="component-group">
              <h3>折叠面板 Collapse</h3>
              <el-collapse v-model="activeNames">
                <el-collapse-item title="一致性 Consistency" name="1">
                  <div
                    >与现实生活一致：与现实生活的流程、逻辑保持一致，遵循用户习惯的语言和概念；</div
                  >
                </el-collapse-item>
                <el-collapse-item title="反馈 Feedback" name="2">
                  <div>控制反馈：通过界面样式和交互动效让用户可以清晰的感知自己的操作；</div>
                </el-collapse-item>
                <el-collapse-item title="效率 Efficiency" name="3">
                  <div>简化流程：设计简洁直观的操作流程；</div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>
        </el-config-provider>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Edit, Search, Check, Message, Star, Delete, View } from '@element-plus/icons-vue'

// 组件尺寸设置
const currentSize = ref<'large' | 'default' | 'small'>('large')

// 表单数据
const form = ref({
  input: '',
  password: '',
  select: '',
  cascader: [],
  radio: 'radio1',
  checkbox: ['checkbox1'],
  date: '',
  time: '',
  switch: true,
  slider: 20,
  rate: 3,
  textarea: ''
})

// 级联选择器选项
const cascaderOptions = [
  {
    value: 'guide',
    label: 'Guide',
    children: [
      {
        value: 'disciplines',
        label: 'Disciplines',
        children: [
          { value: 'consistency', label: 'Consistency' },
          { value: 'feedback', label: 'Feedback' },
          { value: 'efficiency', label: 'Efficiency' },
          { value: 'controllability', label: 'Controllability' }
        ]
      }
    ]
  }
]

// 表格数据
const tableData = ref([
  {
    id: 1,
    name: '王小虎',
    email: 'wangxiaohu@example.com',
    status: '正常'
  },
  {
    id: 2,
    name: '李小明',
    email: 'lixiaoming@example.com',
    status: '正常'
  },
  {
    id: 3,
    name: '张小红',
    email: 'zhangxiaohong@example.com',
    status: '禁用'
  },
  {
    id: 4,
    name: '陈小强',
    email: 'chenxiaoqiang@example.com',
    status: '正常'
  }
])

// 分页
const currentPage = ref(1)

// 折叠面板
const activeNames = ref(['1'])

// 加载状态
const loading = ref(false)

// 反馈方法
const showAlert = () => {
  ElMessageBox.alert('这是一段内容', '标题名称', {
    confirmButtonText: '确定'
  })
}

const showConfirm = () => {
  ElMessageBox.confirm('此操作将永久删除该文件, 是否继续?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
}

const showPrompt = () => {
  ElMessageBox.prompt('请输入邮箱', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
}

const showMessage = () => {
  ElMessage({
    message: '恭喜你，这是一条成功消息',
    type: 'success'
  })
}

const showNotification = () => {
  ElNotification({
    title: '成功',
    message: '这是一条成功的提示消息',
    type: 'success'
  })
}

const toggleLoading = () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 2000)
}
</script>

<style lang="scss" scoped>
.theme-preview-container {
  height: 100%;

  .scrollbar-wrapper {
    height: 100%;
  }

  .preview-content {
    max-width: 1200px;
    padding: 20px;
    margin: 0 auto;
  }

  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 30px;

    .page-title {
      margin: 0;
      font-size: 28px;
      color: var(--el-text-color-primary);
    }

    .size-selector {
      display: flex;
      gap: 8px;
      align-items: center;

      &::before {
        font-size: 14px;
        color: var(--el-text-color-regular);
        content: '组件尺寸：';
      }
    }

    @media (width <= 768px) {
      flex-direction: column;
      gap: 15px;
      align-items: flex-start;

      .page-title {
        font-size: 24px;
      }

      .size-selector {
        align-self: flex-end;
      }
    }
  }

  .section {
    margin-bottom: 40px;
    overflow: hidden;
    border: 1px solid var(--el-border-color-light);
    border-radius: 8px;

    .section-title {
      padding: 15px 20px;
      margin: 0;
      font-size: 20px;
      color: var(--el-text-color-primary);
      background-color: var(--el-bg-color-page);
      border-bottom: 1px solid var(--el-border-color-light);
    }

    .component-group {
      padding: 20px;

      h3 {
        margin: 0 0 15px;
        font-size: 16px;
        color: var(--el-text-color-regular);
      }

      .button-row {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        margin-bottom: 15px;
      }

      .link-row {
        display: flex;
        flex-wrap: wrap;
        gap: 20px;
        margin-bottom: 10px;
      }

      .tag-row {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        margin-bottom: 15px;
      }

      .demo-form {
        .el-form-item {
          margin-bottom: 20px;
        }

        .el-select,
        .el-cascader,
        .el-date-editor {
          width: 100%;
        }
      }

      .box-card {
        .card-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .text {
          font-size: 14px;
        }

        .item {
          margin-bottom: 10px;

          &:last-child {
            margin-bottom: 0;
          }
        }
      }

      .loading-demo {
        margin-top: 15px;
      }

      .el-progress {
        margin-bottom: 10px;

        &:last-child {
          margin-bottom: 0;
        }
      }

      .el-steps {
        margin: 20px 0;
      }

      .el-breadcrumb {
        margin: 10px 0;
      }

      .el-pagination {
        justify-content: center;
        margin: 20px 0;
      }
    }
  }
}
</style>
