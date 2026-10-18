<script setup lang="ts">
/**
 * 知识选择组件
 * 从数据库加载商品和活动知识，选中后影响对话上下文
 */
import { fetchListProducts, fetchListActivities } from '@/api/customer-service/knowledge'

const emit = defineEmits<{
  selectionChange: [data: { selectedProducts: number[]; selectedActivities: number[] }]
}>()

const activeTab = ref('product')

const products = ref<any[]>([])
const activities = ref<any[]>([])
const selectedProducts = ref<number[]>([])
const selectedActivities = ref<number[]>([])

const productEmojis = ['👕', '👖', '👟', '🧢', '👜', '⌚', '📱', '🎧']

const loadData = async () => {
  try {
    const [pRes, aRes] = await Promise.all([
      fetchListProducts({ page: 1, size: 20 }),
      fetchListActivities({ page: 1, size: 20 })
    ])
    products.value = (pRes?.records || []).map((p: any, i: number) => ({
      id: p.id,
      name: p.productName,
      price: p.price,
      category: p.category,
      image: productEmojis[i % productEmojis.length],
      tags: [p.category, p.status].filter(Boolean)
    }))
    activities.value = (aRes?.records || []).map((a: any) => ({
      id: a.id,
      name: a.activityName,
      description: a.description,
      validTime: a.endTime ? `${a.endTime}前有效` : '',
      status: a.status
    }))
  } catch {
    // 后端不可用时保持空列表
  }
}

onMounted(() => loadData())

const toggleProduct = (productId: number) => {
  const index = selectedProducts.value.indexOf(productId)
  if (index > -1) selectedProducts.value.splice(index, 1)
  else selectedProducts.value.push(productId)
  emitSelection()
}

const toggleActivity = (activityId: number) => {
  const index = selectedActivities.value.indexOf(activityId)
  if (index > -1) selectedActivities.value.splice(index, 1)
  else selectedActivities.value.push(activityId)
  emitSelection()
}

const emitSelection = () => {
  emit('selectionChange', {
    selectedProducts: [...selectedProducts.value],
    selectedActivities: [...selectedActivities.value]
  })
}
</script>

<template>
  <div class="knowledge-select">
    <div class="knowledge-select__header">
      <span class="knowledge-select__title">📚 知识选择</span>
    </div>

    <!-- Tab切换 -->
    <el-tabs v-model="activeTab" class="knowledge-tabs">
      <el-tab-pane label="商品" name="product">
        <div class="product-list">
          <div
            v-for="product in products"
            :key="product.id"
            class="product-item"
            :class="{ 'product-item--selected': selectedProducts.includes(product.id) }"
            @click="toggleProduct(product.id)"
          >
            <div class="product-item__image">{{ product.image }}</div>
            <div class="product-item__info">
              <div class="product-item__name">{{ product.name }}</div>
              <div class="product-item__price">
                <span class="product-item__current">¥{{ product.price }}</span>
                <span v-if="product.category" class="product-item__category">{{ product.category }}</span>
              </div>
              <div class="product-item__tags">
                <el-tag
                  v-for="tag in product.tags"
                  :key="tag"
                  size="small"
                  type="warning"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
            <div v-if="selectedProducts.includes(product.id)" class="product-item__check">
              ✓
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="活动" name="activity">
        <div class="activity-list">
          <div
            v-for="activity in activities"
            :key="activity.id"
            class="activity-item"
            :class="{ 'activity-item--selected': selectedActivities.includes(activity.id) }"
            @click="toggleActivity(activity.id)"
          >
            <div class="activity-item__icon">🎁</div>
            <div class="activity-item__info">
              <div class="activity-item__name">{{ activity.name }}</div>
              <div class="activity-item__desc">{{ activity.description }}</div>
              <div class="activity-item__time">{{ activity.validTime }}</div>
            </div>
            <div v-if="selectedActivities.includes(activity.id)" class="activity-item__check">
              ✓
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 已选择的内容 -->
    <div v-if="selectedProducts.length || selectedActivities.length" class="selected-summary">
      <div class="selected-summary__title">已选择</div>
      <div class="selected-summary__content">
        <el-tag
          v-for="id in selectedProducts"
          :key="id"
          closable
          @close="toggleProduct(id)"
        >
          {{ products.find(p => p.id === id)?.name }}
        </el-tag>
        <el-tag
          v-for="id in selectedActivities"
          :key="id"
          type="success"
          closable
          @close="toggleActivity(id)"
        >
          {{ activities.find(a => a.id === id)?.name }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.knowledge-select {
  height: 100%;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &__header {
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;
    flex-shrink: 0;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }
}

.knowledge-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs__header) {
    margin: 0;
    padding: 0 16px;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    overflow-y: auto;
    padding: 0;
  }

  :deep(.el-tab-pane) {
    height: 100%;
    overflow-y: auto;
    padding: 12px;
  }
}

.product-list,
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.product-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background-color: #f9fafb;
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;

  &:hover {
    border-color: #1890ff;
    background-color: #f0f9ff;
  }

  &--selected {
    border-color: #1890ff;
    background-color: #e6f7ff;
  }

  &__image {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    background-color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    margin-left: 10px;
    min-width: 0;
  }

  &__name {
    font-size: 13px;
    font-weight: 500;
    color: #333;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__price {
    margin-bottom: 4px;
  }

  &__current {
    font-size: 14px;
    font-weight: 600;
    color: #f5222d;
  }

  &__category {
    font-size: 12px;
    color: #999;
    margin-left: 6px;
  }

  &__tags {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;

    .el-tag {
      font-size: 10px;
      padding: 0 4px;
      height: 18px;
      line-height: 16px;
    }
  }

  &__check {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background-color: #1890ff;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
  }
}

.activity-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background-color: #f9fafb;
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;

  &:hover {
    border-color: #52c41a;
    background-color: #f6ffed;
  }

  &--selected {
    border-color: #52c41a;
    background-color: #f6ffed;
  }

  &__icon {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    background-color: #fff6e6;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    margin-left: 10px;
  }

  &__name {
    font-size: 13px;
    font-weight: 500;
    color: #333;
    margin-bottom: 2px;
  }

  &__desc {
    font-size: 12px;
    color: #52c41a;
    margin-bottom: 2px;
  }

  &__time {
    font-size: 11px;
    color: #999;
  }

  &__check {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background-color: #52c41a;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
  }
}

.selected-summary {
  padding: 12px;
  border-top: 1px solid #f0f0f0;
  background-color: #fafafa;
  flex-shrink: 0;

  &__title {
    font-size: 12px;
    color: #666;
    margin-bottom: 8px;
  }

  &__content {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;

    .el-tag {
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}
</style>

