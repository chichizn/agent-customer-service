<template>
  <div class="login">
    <div class="login-container">
      <!-- Logo区域 -->
      <div class="logo-section">
        <div class="logo-icon">
          <img src="@/assets/images/login/logo.png" alt="logo" />
        </div>
        <h1 class="welcome-title">欢迎回来</h1>
        <h2 class="login-title">请登录</h2>
      </div>

      <!-- 登录方式切换 -->
      <div class="login-method-tabs">
        <div class="tab-group">
          <button
            class="tab-button"
            :class="{ active: loginMethod === 'password' }"
            @click="loginMethod = 'password'"
          >
            账号密码
          </button>
          <button
            class="tab-button"
            :class="{ active: loginMethod === 'sms' }"
            @click="loginMethod = 'sms'"
          >
            手机验证码
          </button>
        </div>
      </div>

      <!-- 登录表单 -->
      <div class="login-form">
        <el-form ref="formRef" :model="formData" :rules="rules" @keyup.enter="handleSubmit">
          <!-- 手机验证码登录 -->
          <template v-if="loginMethod === 'sms'">
            <el-form-item prop="phone">
              <el-input v-model.trim="formData.phone" placeholder="手机号" class="login-input" />
            </el-form-item>

            <el-form-item prop="smsCode">
              <div class="sms-input-wrapper">
                <el-input
                  v-model.trim="formData.smsCode"
                  placeholder="验证码"
                  class="login-input"
                />
                <button
                  type="button"
                  class="send-code-btn"
                  @click="sendSmsCode"
                  :disabled="smsCountDown > 0"
                >
                  {{ smsCountDown > 0 ? `${smsCountDown}s` : '发送验证码' }}
                </button>
              </div>
            </el-form-item>
          </template>

          <!-- 账号密码登录 -->
          <template v-else>
            <el-form-item prop="account">
              <el-input v-model.trim="formData.account" placeholder="账号" class="login-input" />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model.trim="formData.password"
                type="password"
                placeholder="密码"
                class="login-input"
                autocomplete="off"
              />
            </el-form-item>
          </template>

          <!-- 拖拽验证 -->
          <div class="drag-verify">
            <div class="drag-verify-content" :class="{ error: !isPassing && isClickPass }">
              <ArtDragVerify
                ref="dragVerify"
                v-model:value="isPassing"
                :width="dragVerifyWidth"
                text="按住滑块拖动"
                textColor="#fff"
                successText="验证成功"
                progressBarBg="#57d187"
                background="#303030"
                handlerBg="#424243"
                @pass="onPass"
              />
            </div>
            <p class="error-text" :class="{ 'show-error-text': !isPassing && isClickPass }">
              请完成安全验证
            </p>
          </div>

          <!-- 记住我 -->
          <!-- <div class="remember-section">
            <el-checkbox v-model="formData.rememberMe" class="remember-checkbox">
              记住我
            </el-checkbox>
          </div> -->

          <!-- 登录按钮 -->
          <el-button class="login-btn" @click="handleSubmit" :loading="loading"> 登录 </el-button>
        </el-form>
      </div>
    </div>

    <!-- 手机验证码弹窗 -->
    <MobileVerificationDialog
      v-model="showMobileVerification"
      :phone="mobileVerificationData.phone"
      :pre-auth-token="mobileVerificationData.preAuthToken"
      @success="handleMobileVerificationSuccess"
      @close="handleMobileVerificationClose"
      ref="mobileVerificationRef"
    />
  </div>
</template>

<script setup lang="ts">
import AppConfig from '@/config'
// import { RoutesAlias } from '@/router/routesAlias'
import { ElNotification } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { useRouter } from 'vue-router'
import { ApiStatus } from '@/utils/http/status'
// import { getCssVariable } from '@/utils/colors'
// import { languageOptions } from '@/language'
// import { LanguageEnum, SystemThemeEnum } from '@/enums/appEnum'
import { useI18n } from 'vue-i18n'
import { watch } from 'vue'

const { t } = useI18n()
// import { useSettingStore } from '@/store/modules/setting'
import type { FormInstance, FormRules } from 'element-plus'
import { UserService } from '@/api/user'
import type { Verify2FAParams, SendOtpParams, SmsLoginParams } from '@/api/model/userModel'
import MobileVerificationDialog from '@/components/mobile-verification-dialog/index.vue'

type AccountKey = 'super' | 'admin' | 'user'

export interface Account {
  key: AccountKey
  label: string
  account: string
  password: string
  roles: string[]
}

const dragVerify = ref()

const userStore = useUserStore()
const router = useRouter()
const isPassing = ref(false)
const isClickPass = ref(false)

const systemName = AppConfig.systemInfo.name
const formRef = ref<FormInstance>()

const loginMethod = ref('password') // 'password' or 'sms'
const smsCountDown = ref(0)

// 监听登录方式切换，重置相关状态
watch(loginMethod, () => {
  // 重置拖拽验证
  resetDragVerify()
  // 重置表单验证状态
  formRef.value?.clearValidate()
  // 重置preAuthToken
  smsLoginPreAuthToken.value = ''
})

const formData = reactive({
  accountType: '',
  account: '',
  password: '',
  phone: '',
  smsCode: '',
  rememberMe: false
})

const rules = computed<FormRules>(() => {
  if (loginMethod.value === 'sms') {
    return {
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
      ],
      smsCode: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { pattern: /^\d{6}$/, message: '验证码必须为6位数字', trigger: 'blur' }
      ]
    }
  } else {
    return {
      account: [{ required: true, message: t('login.placeholder[0]'), trigger: 'blur' }],
      password: [{ required: true, message: t('login.placeholder[1]'), trigger: 'blur' }]
    }
  }
})

const loading = ref(false)

// 拖拽验证组件宽度 - 适配300px输入框宽度
const dragVerifyWidth = 300

// F2A 手机验证码相关状态
const showMobileVerification = ref(false)
const mobileVerificationRef = ref()
const mobileVerificationData = ref({
  phone: '',
  preAuthToken: ''
})

// 手机号验证码登录的preAuthToken
const smsLoginPreAuthToken = ref('')

// onMounted(() => {
//   setupAccount('super')
// })

// 设置账号
// const setupAccount = (key: AccountKey) => {
//   const selectedAccount = accounts.value.find((account: Account) => account.key === key)
//   // formData.account = key
//   formData.account = selectedAccount?.account ?? ''
//   formData.password = selectedAccount?.password ?? ''
// }

const onPass = () => {}

// 发送短信验证码
const sendSmsCode = async () => {
  if (!formRef.value) return

  // 使用表单校验验证手机号字段
  try {
    await formRef.value.validateField('phone')
  } catch {
    // 表单校验失败，直接返回，错误信息会自动显示
    return
  }

  // 检查是否完成安全验证
  if (!isPassing.value) {
    isClickPass.value = true
    return
  }

  try {
    const params: SendOtpParams = {
      phone: formData.phone,
      otpType: 'LOGIN_PHONE'
    }

    const res = await UserService.sendOtp(params)

    if (res.code === ApiStatus.success) {
      // 保存preAuthToken用于后续登录
      if (res.data && typeof res.data === 'object' && 'preAuthToken' in res.data) {
        smsLoginPreAuthToken.value = (res.data as any).preAuthToken
      }

      // 开始倒计时
      smsCountDown.value = 60
      const timer = setInterval(() => {
        smsCountDown.value--
        if (smsCountDown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      // 手机验证码登录时不需要安全验证（已在发送验证码时验证过）
      if (loginMethod.value !== 'sms' && !isPassing.value) {
        isClickPass.value = true
        return
      }

      loading.value = true

      try {
        let res: any

        if (loginMethod.value === 'sms') {
          // 手机验证码登录
          if (!smsLoginPreAuthToken.value) {
            ElNotification.error({ message: '请先获取验证码' })
            loading.value = false
            resetDragVerify()
            return
          }

          const smsParams: SmsLoginParams = {
            preAuthToken: smsLoginPreAuthToken.value,
            otpCode: formData.smsCode,
            isClient: false
          }

          res = await UserService.smsLogin(smsParams)
        } else {
          // 账号密码登录
          const loginParams = {
            username: formData.account,
            password: formData.password
          }
          console.log('🔍 [前端调试] 发送登录请求:', loginParams)
          res = await UserService.login(loginParams)
          console.log('🔍 [前端调试] 登录响应:', res)
        }

        console.log(
          '🔍 [前端调试] 检查响应码:',
          res.code,
          '期望:',
          ApiStatus.success,
          '是否相等:',
          res.code === ApiStatus.success
        )
        if (res.code === ApiStatus.success) {
          const {
            token,
            tokenType,
            expiresIn,
            userId,
            username,
            nickname,
            avatar,
            requiresTwoFactor,
            preAuthToken,
            phone
          } = res.data
          console.log('🔍 [前端调试] 解析响应数据:', {
            token,
            tokenType,
            expiresIn,
            userId,
            username,
            nickname,
            avatar,
            requiresTwoFactor,
            preAuthToken,
            phone
          })

          // 判断是否需要双因子验证
          if (requiresTwoFactor && preAuthToken && phone) {
            // 需要手机验证码验证
            loading.value = false
            mobileVerificationData.value = {
              phone,
              preAuthToken
            }
            showMobileVerification.value = true
            return
          }

          // 直接登录成功
          if (token) {
            userStore.setToken(token)
            await userStore.initUserInfo()

            // 清除错误状态
            isClickPass.value = false

            // 登录成功提示
            showLoginSuccessNotice()

            // 登录成功后跳转到首页，让路由守卫处理动态路由注册和权限检查
            await router.push('/')
          }
        } else {
          loading.value = false
          resetDragVerify()
        }
      } catch (error) {
        loading.value = false
        resetDragVerify()
        console.error('登录失败:', error)
      } finally {
        if (!showMobileVerification.value) {
          loading.value = false
          resetDragVerify()
        }
      }
    }
  })
}

// 重置拖拽验证
const resetDragVerify = () => {
  dragVerify.value?.reset()
  isClickPass.value = false
}

// 登录成功提示
const showLoginSuccessNotice = () => {
  setTimeout(() => {
    ElNotification({
      title: t('login.success.title'),
      type: 'success',
      duration: 2500,
      zIndex: 10000,
      message: `${t('login.success.message')}, ${systemName}!`
    })
  }, 150)
}

// 处理手机验证码验证成功
const handleMobileVerificationSuccess = async (otpCode: string) => {
  if (!mobileVerificationData.value.preAuthToken) {
    ElNotification.error({
      title: '验证失败',
      message: '缺少必要参数，请重新登录'
    })
    return
  }

  // 设置验证码弹窗loading状态
  mobileVerificationRef.value?.setLoading(true)

  try {
    const params: Verify2FAParams = {
      preAuthToken: mobileVerificationData.value.preAuthToken,
      otpCode,
      isClient: false
    }

    const res = await UserService.verify2FA(params)

    if (res.code === ApiStatus.success) {
      const { token } = res.data

      if (token) {
        userStore.setToken(token)
        await userStore.initUserInfo()

        // 关闭验证码弹窗
        showMobileVerification.value = false

        // 清除错误状态
        isClickPass.value = false

        // 登录成功提示
        showLoginSuccessNotice()

        // 登录成功后跳转到首页，让路由守卫处理动态路由注册和权限检查
        await router.push('/')
      }
    }
  } catch (error) {
    // ElNotification.error({
    //   title: '验证失败',
    //   message: '验证码验证失败，请重试'
    // })
    console.error('手机验证码验证失败:', error)
  } finally {
    // 重置验证码弹窗loading状态
    mobileVerificationRef.value?.setLoading(false)
  }
}

// 处理手机验证码弹窗关闭
const handleMobileVerificationClose = () => {
  // 重置相关状态
  mobileVerificationData.value = {
    phone: '',
    preAuthToken: ''
  }
  // 重置拖拽验证
  resetDragVerify()
}

// 切换语言
// const { locale } = useI18n()

// const changeLanguage = (lang: LanguageEnum) => {
//   if (locale.value === lang) return
//   locale.value = lang
//   userStore.setLanguage(lang)
// }

// 切换主题
// import { useTheme } from '@/composables/useTheme'

// const toggleTheme = () => {
//   let { LIGHT, DARK } = SystemThemeEnum
//   useTheme().switchThemeStyles(systemThemeType.value === LIGHT ? DARK : LIGHT)
// }
</script>

<style lang="scss" scoped>
@use './index';
</style>
