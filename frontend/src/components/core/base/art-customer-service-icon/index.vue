<!-- 智能客服图标 -->
<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    class="customer-service-icon"
  >
    <rect
      class="icon-stroke"
      x="6.27"
      y="5.32"
      width="11.45"
      height="15.27"
      rx="5.73"
      :stroke="strokeColor"
      stroke-width="1.91"
      stroke-miterlimit="10"
    />
    <path
      class="icon-stroke"
      d="M17.73,9.14h1.91A2.86,2.86,0,0,1,22.5,12v1.91a2.86,2.86,0,0,1-2.86,2.86H17.73a0,0,0,0,1,0,0V9.14A0,0,0,0,1,17.73,9.14Z"
      :stroke="strokeColor"
      stroke-width="1.91"
      stroke-miterlimit="10"
    />
    <path
      class="icon-stroke"
      d="M1.5,9.14H3.41A2.86,2.86,0,0,1,6.27,12v1.91a2.86,2.86,0,0,1-2.86,2.86H1.5a0,0,0,0,1,0,0V9.14A0,0,0,0,1,1.5,9.14Z"
      transform="translate(7.77 25.91) rotate(180)"
      :stroke="strokeColor"
      stroke-width="1.91"
      stroke-miterlimit="10"
    />
    <path
      class="icon-stroke"
      d="M4.36,9.14h0A7.64,7.64,0,0,1,12,1.5h0a7.64,7.64,0,0,1,7.64,7.64h0"
      :stroke="strokeColor"
      stroke-width="1.91"
      stroke-miterlimit="10"
    />
    <path
      class="icon-stroke"
      d="M19.64,16.77v1a4.78,4.78,0,0,1-4.78,4.77"
      :stroke="strokeColor"
      stroke-width="1.91"
      stroke-miterlimit="10"
    />
  </svg>
</template>

<script setup lang="ts">
defineOptions({ name: 'ArtCustomerServiceIcon' })

interface Props {
  /** 图标尺寸 */
  size?: number | string
  /** 描边颜色 */
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 24,
  color: 'currentColor'
})

const strokeColor = computed(() => props.color)
</script>

<style lang="scss" scoped>
.customer-service-icon {
  display: inline-block;
  vertical-align: middle;
  
  .icon-stroke {
    fill: none;
    transition: stroke 0.2s ease;
  }
}
</style>

