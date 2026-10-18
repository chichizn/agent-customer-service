<script setup lang="ts">
/**
 * 商品知识管理 - 调用后端 KnowledgeController 接口
 */
import { fetchListProducts, fetchCreateProduct, fetchUpdateProduct, fetchDeleteProduct, fetchVectorize } from '@/api/customer-service/knowledge'

interface Product {
  id: number
  productCode: string
  productName: string
  price: number
  category: string
  brand: string
  description: string
  specifications: string
  features: string
  status: string
  vectorized: boolean
  updatedAt: string
}

const searchForm = reactive({ keyword: '', category: '', status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const tableData = ref<Product[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const submitting = ref(false)
const formRef = ref()
const editingId = ref<number | null>(null)

const formData = reactive({
  productName: '',
  productCode: '',
  category: '',
  brand: '',
  price: undefined as number | undefined,
  description: '',
  features: ''
})

const formRules = {
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  productCode: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await fetchListProducts({
      keyword: searchForm.keyword || undefined,
      category: searchForm.category || undefined,
      status: searchForm.status || undefined,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch { /* 后端不可用时保持空数据 */ } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })

const handleSearch = () => { pagination.page = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.category = ''; searchForm.status = ''; handleSearch() }

const resetForm = () => {
  editingId.value = null
  formData.productName = ''
  formData.productCode = ''
  formData.category = ''
  formData.brand = ''
  formData.price = undefined
  formData.description = ''
  formData.features = ''
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增商品'
  dialogVisible.value = true
}

const handleEdit = (row: Product) => {
  editingId.value = row.id
  dialogTitle.value = '编辑商品'
  formData.productName = row.productName
  formData.productCode = row.productCode
  formData.category = row.category
  formData.brand = row.brand || ''
  formData.price = row.price
  formData.description = row.description || ''
  formData.features = row.features || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch { return }

  submitting.value = true
  try {
    if (editingId.value) {
      await fetchUpdateProduct({ id: editingId.value, ...formData })
      ElMessage.success('更新成功')
    } else {
      await fetchCreateProduct({ ...formData })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('操作失败，请检查后端服务')
  } finally {
    submitting.value = false
  }
}

const handleDelete = (row: Product) => {
  ElMessageBox.confirm('确定要删除该商品知识吗？', '提示', { type: 'warning' }).then(async () => {
    await fetchDeleteProduct(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

const handleVectorize = async (row: Product) => {
  try {
    await fetchVectorize({ knowledgeType: 'product', knowledgeId: row.id })
    ElMessage.success('已提交向量化任务')
    loadData()
  } catch { ElMessage.error('向量化失败') }
}

const handlePageChange = (page: number) => { pagination.page = page; loadData() }
const handleSizeChange = (size: number) => { pagination.size = size; pagination.page = 1; loadData() }
</script>

<template>
  <div class="product-knowledge">
    <!-- 搜索区域 -->
    <div class="search-area">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="商品名称/SKU" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="上架" value="上架" />
            <el-option label="草稿" value="草稿" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="search-actions">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增商品
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" :loading="loading" stripe>
      <el-table-column prop="productCode" label="商品编码" width="120" />
      <el-table-column prop="productName" label="商品名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column label="价格" width="100">
        <template #default="{ row }">
          <span class="price-current">¥{{ row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === '上架' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="向量化" width="100">
        <template #default="{ row }">
          <el-tag :type="row.vectorized ? 'success' : 'info'" size="small">{{ row.vectorized ? '已向量化' : '待向量化' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="150" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="primary" link size="small" @click="handleVectorize(row)">向量化</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-area">
      <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]" :current-page="pagination.page" :page-size="pagination.size"
        @current-change="handlePageChange" @size-change="handleSizeChange" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="商品名称" prop="productName">
          <el-input v-model="formData.productName" placeholder="如：夏季清凉短袖T恤" />
        </el-form-item>
        <el-form-item label="商品编码" prop="productCode">
          <el-input v-model="formData.productCode" placeholder="如：TSH-007" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="formData.category" placeholder="请选择" style="width: 100%">
                <el-option label="服装" value="服装" />
                <el-option label="鞋类" value="鞋类" />
                <el-option label="配饰" value="配饰" />
                <el-option label="数码" value="数码" />
                <el-option label="箱包" value="箱包" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="formData.brand" placeholder="如：AJ时尚" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="formData.price" :min="0" :precision="2" style="width: 200px" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="商品描述，对话检索时会用到" />
        </el-form-item>
        <el-form-item label="特色">
          <el-input v-model="formData.features" type="textarea" :rows="2" placeholder="商品特色，逗号分隔，如：纯棉透气,经典百搭" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.product-knowledge {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-area {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.search-actions {
  display: flex;
  gap: 8px;
}

.price-current {
  color: #f5222d;
  font-weight: 600;
}

.price-original {
  color: #999;
  text-decoration: line-through;
  margin-left: 8px;
  font-size: 12px;
}

.pagination-area {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

