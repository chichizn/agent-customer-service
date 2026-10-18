<template>
  <el-dialog
    v-model="visible"
    title="手机验证码登录"
    width="420px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <div class="mobile-verification">
      <!-- 手机号信息 -->
      <div class="info-section">
        <p class="phone-info">
          已向手机号
          <span class="phone-number">{{ maskedPhone }}</span>
          发送验证码
        </p>
        <p class="tip-text">请输入收到的6位数字验证码</p>
      </div>

      <!-- 验证码输入区域 -->
      <div class="input-section">
        <el-form ref="formRef" :model="formData" :rules="rules" @keyup.enter="handleConfirm">
          <el-form-item prop="otpCode" class="otp-form-item">
            <div class="input-with-button">
              <el-input
                v-model="formData.otpCode"
                placeholder="请输入6位验证码"
                maxlength="6"
                clearable
                :disabled="loading"
                class="otp-input"
                size="large"
              >
                <template #prefix>
                  <el-icon color="#909399">
                    <Lock />
                  </el-icon>
                </template>
              </el-input>
              <el-button
                type="primary"
                :disabled="sendDisabled"
                :loading="sendLoading"
                @click.stop="handleResend"
                class="send-btn"
                size="large"
              >
                {{ sendButtonText }}
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-center">
        <el-button @click="handleClose" :disabled="loading"> 取消 </el-button>
        <el-button
          type="primary"
          @click="handleConfirm"
          :loading="loading"
          :disabled="!formData.otpCode || formData.otpCode.length !== 6"
        >
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import { UserService } from '@/api/user'
import type { SendOtpParams } from '@/api/model/userModel'
import { ApiStatus } from '@/utils/http/status'

// 定义组件props
interface Props {
  /** 弹窗显示状态 */
  modelValue: boolean
  /** 手机号 */
  phone: string
  /** 临时认证token */
  preAuthToken: string
}

// 定义组件事件
interface Emits {
  /** 更新弹窗显示状态 */
  (e: 'update:modelValue', value: boolean): void
  /** 验证码验证成功 */
  (e: 'success', otpCode: string): void
  /** 弹窗关闭 */
  (e: 'close'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 表单相关
const formRef = ref<FormInstance>()
const formData = ref({
  otpCode: ''
})

// 表单验证规则
const rules: FormRules = {
  otpCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码必须为6位数字', trigger: 'blur' }
  ]
}

// 状态管理
const loading = ref(false)
const sendLoading = ref(false)
const countdown = ref(0)
const timer = ref<ReturnType<typeof setTimeout>>()

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const maskedPhone = computed(() => {
  if (!props.phone) return ''
  return props.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
})

const sendDisabled = computed(() => sendLoading.value || countdown.value > 0)

const sendButtonText = computed(() => {
  if (sendLoading.value) return '发送中...'
  if (countdown.value > 0) return `${countdown.value}秒后重发`
  return '重新发送'
})

// 监听弹窗显示状态
watch(visible, (newVal) => {
  if (newVal) {
    // 弹窗打开时自动发送验证码
    nextTick(() => {
      sendOtp()
    })
  } else {
    // 弹窗关闭时重置表单和倒计时
    resetForm()
    stopCountdown()
  }
})

// 发送验证码
const sendOtp = async () => {
  if (!props.preAuthToken || !props.phone) {
    ElMessage.error('缺少必要参数，请重新登录')
    return
  }

  sendLoading.value = true

  try {
    const params: SendOtpParams = {
      preAuthToken: props.preAuthToken,
      phone: props.phone,
      otpType: 'LOGIN_2FA'
    }

    const res = await UserService.sendOtp(params)

    if (res.code === ApiStatus.success) {
      ElMessage.success('验证码发送成功')
      startCountdown()
    }
  } catch (error) {
    // ElMessage.error('验证码发送失败，请重试')
    console.error('发送验证码失败:', error)
  } finally {
    sendLoading.value = false
  }
}

// 重新发送验证码
const handleResend = () => {
  if (sendDisabled.value) return
  sendOtp()
}

// 开始倒计时
const startCountdown = () => {
  countdown.value = 60
  timer.value = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      stopCountdown()
    }
  }, 1000)
}

// 停止倒计时
const stopCountdown = () => {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = undefined
  }
  countdown.value = 0
}

// 确认提交
const handleConfirm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      emit('success', formData.value.otpCode)
    }
  })
}

// 关闭弹窗
const handleClose = () => {
  if (loading.value) return

  visible.value = false
  emit('close')
}

// 重置表单
const resetForm = () => {
  formData.value.otpCode = ''
  formRef.value?.resetFields()
}

// 设置loading状态（供父组件调用）
const setLoading = (value: boolean) => {
  loading.value = value
}

// 暴露方法给父组件
defineExpose({
  setLoading
})
</script>

<style lang="scss" scoped>
.mobile-verification {
  .info-section {
    margin-bottom: 24px;
    text-align: center;

    .phone-info {
      margin-bottom: 8px;
      font-size: 16px;
      line-height: 1.5;
      color: var(--el-text-color-primary);

      .phone-number {
        font-family: Monaco, Menlo, monospace;
        font-weight: 600;
        color: #f9c924;
      }
    }

    .tip-text {
      margin: 0;
      font-size: 14px;
      line-height: 1.4;
      color: var(--el-text-color-regular);
    }
  }

  .input-section {
    padding: 0 24px;

    .otp-form-item {
      margin-bottom: 0;

      :deep(.el-form-item__content) {
        display: block;
      }

      .input-with-button {
        display: flex;
        gap: 12px;
        align-items: center;
      }
    }
  }
}
</style>
