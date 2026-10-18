<!-- 系统logo -->
<template>
  <div class="flex-cc">
    <img v-if="menuOpen" :style="logoStyle" :src="logoSrc" alt="logo" class="w-full h-full" />
    <img v-else :style="logoStyle" :src="logoSmallSrc" alt="logo" />
  </div>
</template>

<script setup lang="ts">
  import { useSettingStore } from '@/store/modules/setting'
  import logoImage from '@/assets/images/common/logo.svg'
  import logoWhiteImage from '@/assets/images/common/logo-white.svg'
  import logoSmallImage from '@/assets/images/common/logo-small.svg'
  import logoSmallWhiteImage from '@/assets/images/common/logo-small-white.svg'

  defineOptions({ name: 'AnjingLogo' })

  interface Props {
    menuOpen?: boolean
    /** logo 大小 */
    size?: number | string
  }

  const props = withDefaults(defineProps<Props>(), {
    size: 110
  })

  const settingStore = useSettingStore()

  const logoStyle = computed(() => ({ width: `${props.size}px` }))

  // 根据主题模式动态切换 logo
  const logoSrc = computed(() => {
    return settingStore.isDark ? logoWhiteImage : logoImage
  })
  // 根据主题模式动态切换 logo
  const logoSmallSrc = computed(() => {
    return settingStore.isDark ? logoSmallWhiteImage : logoSmallImage
  })
</script>

<style lang="scss" scoped>
  .art-logo {
    display: flex;
    align-items: center;
    justify-content: center;

    img {
      width: 100%;
      height: 100%;
    }
  }
</style>
