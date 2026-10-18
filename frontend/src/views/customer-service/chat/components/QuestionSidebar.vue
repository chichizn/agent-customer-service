<script setup lang="ts">
/**
 * 常见问题侧边栏
 * 从 FAQ 知识库加载真实数据，按分类分组展示
 */
import { fetchListFaqs } from '@/api/customer-service/knowledge'

interface Question {
  id: string
  text: string
}

interface QuestionCategory {
  name: string
  icon: string
  questions: Question[]
}

interface Props {
  selectedQuestion?: string
}

interface Emits {
  select: [question: Question]
}

defineProps<Props>()
const emit = defineEmits<Emits>()

const expandedCategories = ref<string[]>([])
const categories = ref<QuestionCategory[]>([])

const categoryIcons: Record<string, string> = {
  '售前咨询': '💬',
  '售后': '🔧',
  '售后服务': '🔧',
  '物流': '🚚',
  '物流配送': '🚚',
  '支付': '💳',
  '支付问题': '💳',
  '订单': '📦',
  '通用': '💡'
}

const loadFaqs = async () => {
  try {
    const res = await fetchListFaqs({ page: 1, size: 100 })
    const faqs = res?.records || []

    const grouped: Record<string, Question[]> = {}
    for (const faq of faqs) {
      const cat = faq.category || '通用'
      if (!grouped[cat]) grouped[cat] = []
      grouped[cat].push({ id: String(faq.id), text: faq.question })
    }

    categories.value = Object.entries(grouped).map(([name, questions]) => ({
      name,
      icon: categoryIcons[name] || '❓',
      questions
    }))

    if (categories.value.length > 0) {
      expandedCategories.value = [categories.value[0].name]
    }
  } catch {
    // 后端不可用时保持空
  }
}

onMounted(() => loadFaqs())

const toggleCategory = (categoryName: string) => {
  const index = expandedCategories.value.indexOf(categoryName)
  if (index > -1) expandedCategories.value.splice(index, 1)
  else expandedCategories.value.push(categoryName)
}

const selectQuestion = (question: Question) => {
  emit('select', question)
}

const isCategoryExpanded = (categoryName: string) => {
  return expandedCategories.value.includes(categoryName)
}
</script>

<template>
  <div class="question-sidebar">
    <div class="question-sidebar__header">
      <span class="question-sidebar__title">💡 消费者常见问题</span>
    </div>

    <div class="question-sidebar__content">
      <div
        v-for="category in categories"
        :key="category.name"
        class="question-category"
      >
        <!-- 分类标题 -->
        <div
          class="question-category__header"
          @click="toggleCategory(category.name)"
        >
          <span class="question-category__icon">{{ category.icon }}</span>
          <span class="question-category__name">{{ category.name }}</span>
          <el-icon class="question-category__arrow">
            <component :is="isCategoryExpanded(category.name) ? 'ArrowDown' : 'ArrowRight'" />
          </el-icon>
        </div>

        <!-- 问题列表 -->
        <transition name="collapse">
          <div
            v-show="isCategoryExpanded(category.name)"
            class="question-category__list"
          >
            <div
              v-for="question in category.questions"
              :key="question.id"
              class="question-item"
              :class="{ 'question-item--active': selectedQuestion === question.text }"
              @click="selectQuestion(question)"
            >
              {{ question.text }}
            </div>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.question-sidebar {
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

  &__content {
    flex: 1;
    overflow-y: auto;
    padding: 8px;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background-color: #ddd;
      border-radius: 2px;
    }
  }
}

.question-category {
  margin-bottom: 4px;

  &__header {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    cursor: pointer;
    border-radius: 8px;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f5f5f5;
    }
  }

  &__icon {
    font-size: 16px;
    margin-right: 8px;
  }

  &__name {
    flex: 1;
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }

  &__arrow {
    color: #999;
    transition: transform 0.2s;
  }

  &__list {
    padding-left: 8px;
    overflow: hidden;
  }
}

.question-item {
  padding: 10px 12px;
  margin: 4px 0;
  font-size: 13px;
  color: #666;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.5;

  &:hover {
    background-color: #e6f7ff;
    color: #1890ff;
  }

  &--active {
    background-color: #1890ff;
    color: #fff;

    &:hover {
      background-color: #40a9ff;
      color: #fff;
    }
  }
}

// 折叠动画
.collapse-enter-active,
.collapse-leave-active {
  transition: all 0.3s ease;
}

.collapse-enter-from,
.collapse-leave-to {
  opacity: 0;
  max-height: 0;
}

.collapse-enter-to,
.collapse-leave-from {
  opacity: 1;
  max-height: 500px;
}
</style>

