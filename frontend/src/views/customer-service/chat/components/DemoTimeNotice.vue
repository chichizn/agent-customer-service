<script setup lang="ts">
  import { computed } from 'vue'
  import type { DemoBusinessTime } from '@/api/customer-service/model/workbenchModel'

  const props = defineProps<{ businessTime?: DemoBusinessTime }>()
  const reference = computed(() => {
    if (!props.businessTime) return ''
    return new Date(props.businessTime.effectiveAt).toLocaleString('zh-CN', {
      timeZone: props.businessTime.timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    })
  })
</script>

<template>
  <div v-if="businessTime" class="cs-demo-banner" role="note">
    <span class="cs-badge amber">演示时间</span>
    <span v-if="businessTime.mode === 'fixed'">
      演示活动按 {{ reference }}（{{ businessTime.timeZone }}）判断有效期。
    </span>
    <span v-else>活动有效期使用系统时间（{{ businessTime.timeZone }}）。</span>
    订单与物流为预置数据；消息和工单记录实际操作时间，售后诉求由商家审核。
  </div>
</template>
