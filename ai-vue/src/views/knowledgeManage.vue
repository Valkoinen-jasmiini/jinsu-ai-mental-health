<template>
  <div class="kb-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1>📚 知识库管理</h1>
        <p class="page-desc">维护知识库分类与文章内容,普通用户端实时同步展示上架内容</p>
      </div>
      <div class="header-right">
        <el-button @click="loadAll" :loading="loading" circle>
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 分类管理 -->
    <div class="chart-card category-card">
      <div class="card-head">
        <h3>分类管理</h3>
        <el-button type="primary" size="small" @click="openCategoryDialog()">
          <el-icon><Plus /></el-icon>&nbsp;新增分类
        </el-button>
      </div>
      <el-table :data="categories" v-loading="loading" stripe size="default">
        <el-table-column prop="categoryName" label="分类名称" min-width="140">
          <template #default="{ row }">
            <span class="category-name">{{ row.categoryName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="categoryCode" label="分类编码" min-width="120">
          <template #default="{ row }">{{ row.categoryCode || '—' }}</template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '—' }}</template>
        </el-table-column>
        <el-table-column prop="articleCount" label="文章数" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="info">{{ row.articleCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" effect="light" size="small">启用</el-tag>
            <el-tag v-else type="info" effect="light" size="small">停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openCategoryDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="handleDeleteCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 文章管理 -->
    <div class="chart-card">
      <div class="card-head">
        <h3>文章管理</h3>
        <div class="toolbar">
          <el-select v-model="articleQuery.categoryId" placeholder="全部分类" clearable style="width: 150px" @change="handleArticleSearch">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.categoryName"
              :value="cat.id"
            />
          </el-select>
          <el-input
            v-model="articleQuery.keyword"
            placeholder="搜索标题 / 摘要 / 标签"
            clearable
            class="search-input"
            @keyup.enter="handleArticleSearch"
            @clear="handleArticleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="openArticleDialog()">
            <el-icon><Plus /></el-icon>&nbsp;新增文章
          </el-button>
        </div>
      </div>

      <el-table :data="articles" v-loading="articleLoading" stripe>
        <el-table-column label="封面" width="90" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.coverImage"
              :src="row.coverImage"
              :preview-src-list="[row.coverImage]"
              preview-teleported
              fit="cover"
              class="cover-thumb"
            />
            <span v-else class="text-muted">无封面</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="article-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.categoryName || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag
              v-for="tag in splitTags(row.tags)"
              :key="tag"
              size="small"
              type="warning"
              effect="plain"
              class="tag-chip"
            >{{ tag }}</el-tag>
            <span v-if="!row.tags" class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="readCount" label="阅读数" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" effect="light" size="small">上架</el-tag>
            <el-tag v-else type="info" effect="light" size="small">下架</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="165">
          <template #default="{ row }">{{ formatTime(row.publishedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openArticleDialog(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" link @click="handleToggleArticle(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" type="danger" link @click="handleDeleteArticle(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="articleQuery.page"
          v-model:page-size="articleQuery.size"
          :page-sizes="[10, 20, 50]"
          :total="articleTotal"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadArticles"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 分类编辑弹窗 -->
    <el-dialog
      v-model="categoryDialogVisible"
      :title="categoryForm.id ? '编辑分类' : '新增分类'"
      width="480px"
      destroy-on-close
    >
      <el-form :model="categoryForm" label-width="80px">
        <el-form-item label="分类名称" required>
          <el-input v-model="categoryForm.categoryName" placeholder="例如：情绪调节" maxlength="50" />
        </el-form-item>
        <el-form-item label="分类编码">
          <el-input v-model="categoryForm.categoryCode" placeholder="唯一编码,例如 emotion-regulation" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="categoryForm.description" type="textarea" :rows="2" placeholder="分类简介(可选)" maxlength="255" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="categoryForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveCategory">保存</el-button>
      </template>
    </el-dialog>

    <!-- 文章编辑弹窗 -->
    <el-dialog
      v-model="articleDialogVisible"
      :title="articleForm.id ? '编辑文章' : '新增文章'"
      width="720px"
      top="6vh"
      destroy-on-close
    >
      <el-form :model="articleForm" label-width="80px">
        <el-form-item label="所属分类" required>
          <el-select v-model="articleForm.categoryId" placeholder="请选择分类" style="width: 240px">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.categoryName"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文章标题" required>
          <el-input v-model="articleForm.title" placeholder="请输入文章标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="articleForm.summary" type="textarea" :rows="2" placeholder="文章摘要,展示在知识库卡片上(可选)" maxlength="500" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="articleForm.tags" placeholder="多个标签用英文逗号分隔,例如：焦虑,冥想,睡眠" maxlength="200" />
        </el-form-item>
        <el-form-item label="封面地址">
          <el-input v-model="articleForm.coverImage" placeholder="封面图片URL(可选)" maxlength="255" />
        </el-form-item>
        <el-form-item label="文章内容">
          <div class="content-editor">
            <div class="content-toolbar">
              <el-button size="small" @click="triggerImport">
                <el-icon><Upload /></el-icon>&nbsp;导入本地文件(.md / .txt)
              </el-button>
              <input
                ref="fileInputRef"
                type="file"
                accept=".md,.markdown,.txt"
                style="display: none"
                @change="handleFileImport"
              />
              <span class="content-hint">支持 Markdown / 纯文本,可直接粘贴或导入本地文件</span>
            </div>
            <el-input
              v-model="articleForm.content"
              type="textarea"
              :rows="14"
              placeholder="请输入文章正文内容"
            />
          </div>
        </el-form-item>
        <el-form-item label="上架状态">
          <el-switch v-model="articleForm.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="articleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveArticle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Plus, Upload } from '@element-plus/icons-vue'
import {
  getAdminCategories,
  addAdminCategory,
  updateAdminCategory,
  deleteAdminCategory,
  getAdminArticles,
  getAdminArticleDetail,
  addAdminArticle,
  updateAdminArticle,
  deleteAdminArticle
} from '@/api/admin.js'

const loading = ref(false)
const saving = ref(false)

// ---------- 分类管理 ----------
const categories = ref([])
const categoryDialogVisible = ref(false)
const categoryForm = reactive({
  id: null,
  categoryName: '',
  categoryCode: '',
  description: '',
  sortOrder: 0,
  status: 1
})

const loadCategories = async () => {
  try {
    categories.value = await getAdminCategories() || []
  } catch (e) { /* 错误提示由拦截器统一处理 */ }
}

// 打开分类弹窗(row为空表示新增)
const openCategoryDialog = (row) => {
  if (row) {
    Object.assign(categoryForm, {
      id: row.id,
      categoryName: row.categoryName,
      categoryCode: row.categoryCode,
      description: row.description,
      sortOrder: row.sortOrder ?? 0,
      status: row.status ?? 1
    })
  } else {
    Object.assign(categoryForm, {
      id: null,
      categoryName: '',
      categoryCode: '',
      description: '',
      sortOrder: 0,
      status: 1
    })
  }
  categoryDialogVisible.value = true
}

// 保存分类(新增/编辑)
const handleSaveCategory = async () => {
  if (!categoryForm.categoryName.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    const { id, ...data } = categoryForm
    if (id) {
      await updateAdminCategory(id, data)
      ElMessage.success('分类更新成功')
    } else {
      await addAdminCategory(data)
      ElMessage.success('分类创建成功')
    }
    categoryDialogVisible.value = false
    loadCategories()
  } catch (e) { /* 错误提示由拦截器统一处理 */ } finally {
    saving.value = false
  }
}

// 删除分类(分类下有文章时后端会拒绝)
const handleDeleteCategory = (row) => {
  ElMessageBox.confirm(
    `确定要删除分类「${row.categoryName}」吗？${row.articleCount > 0 ? `该分类下还有 ${row.articleCount} 篇文章,无法删除。` : ''}`,
    '删除确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    await deleteAdminCategory(row.id)
    ElMessage.success('分类删除成功')
    loadCategories()
  }).catch(() => {})
}

// ---------- 文章管理 ----------
const articleLoading = ref(false)
const articles = ref([])
const articleTotal = ref(0)
const articleQuery = reactive({
  categoryId: null,
  keyword: '',
  page: 1,
  size: 10
})

const loadArticles = async () => {
  articleLoading.value = true
  try {
    const res = await getAdminArticles({
      categoryId: articleQuery.categoryId,
      keyword: articleQuery.keyword,
      page: articleQuery.page,
      size: articleQuery.size
    })
    articles.value = res.records || []
    articleTotal.value = Number(res.total) || 0
  } catch (e) {
    articles.value = []
  } finally {
    articleLoading.value = false
  }
}

// 条件搜索(重置页码)
const handleArticleSearch = () => {
  articleQuery.page = 1
  loadArticles()
}

const handleSizeChange = () => {
  articleQuery.page = 1
  loadArticles()
}

// 文章编辑表单
const articleDialogVisible = ref(false)
const articleForm = reactive({
  id: null,
  categoryId: null,
  title: '',
  summary: '',
  tags: '',
  coverImage: '',
  content: '',
  status: 1
})

// 打开文章弹窗(row为空表示新增;编辑时拉取详情补全content)
const openArticleDialog = async (row) => {
  if (row) {
    Object.assign(articleForm, {
      id: row.id,
      categoryId: row.categoryId,
      title: row.title,
      summary: row.summary,
      tags: row.tags,
      coverImage: row.coverImage,
      content: row.content || '',
      status: row.status ?? 1
    })
  } else {
    Object.assign(articleForm, {
      id: null,
      categoryId: categories.value.length ? categories.value[0].id : null,
      title: '',
      summary: '',
      tags: '',
      coverImage: '',
      content: '',
      status: 1
    })
  }
  articleDialogVisible.value = true
}

// 保存文章(新增/编辑)
const handleSaveArticle = async () => {
  if (!articleForm.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  if (!articleForm.categoryId) {
    ElMessage.warning('请选择所属分类')
    return
  }
  saving.value = true
  try {
    const { id, ...data } = articleForm
    if (id) {
      await updateAdminArticle(id, data)
      ElMessage.success('文章更新成功')
    } else {
      await addAdminArticle(data)
      ElMessage.success('文章创建成功')
    }
    articleDialogVisible.value = false
    loadArticles()
    loadCategories() // 文章数可能变化
  } catch (e) { /* 错误提示由拦截器统一处理 */ } finally {
    saving.value = false
  }
}

// 上架/下架文章
const handleToggleArticle = (row) => {
  const action = row.status === 1 ? '下架' : '上架'
  ElMessageBox.confirm(
    `确定要${action}文章「${row.title}」吗？${row.status === 1 ? '下架后普通用户将无法查看该文章。' : '上架后普通用户端立即可见。'}`,
    `${action}确认`,
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    await updateAdminArticle(row.id, {
      categoryId: row.categoryId,
      title: row.title,
      summary: row.summary,
      content: row.content,
      coverImage: row.coverImage,
      tags: row.tags,
      status: row.status === 1 ? 0 : 1
    })
    ElMessage.success(`已${action}文章「${row.title}」`)
    loadArticles()
  }).catch(() => {})
}

// 删除文章(后端级联删除收藏与阅读历史)
const handleDeleteArticle = (row) => {
  ElMessageBox.confirm(
    `确定要删除文章「${row.title}」吗？删除后不可恢复,该文章的收藏与阅读记录将一并删除。`,
    '删除确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    await deleteAdminArticle(row.id)
    ElMessage.success('文章删除成功')
    loadArticles()
    loadCategories()
  }).catch(() => {})
}

// ---------- 本地文件导入 ----------
const fileInputRef = ref(null)

const triggerImport = () => {
  fileInputRef.value && fileInputRef.value.click()
}

// 读取本地 .md/.txt 文件内容填充到正文
const handleFileImport = (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = '' // 允许重复选择同一文件
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('文件过大,请选择 2MB 以内的文本文件')
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    articleForm.content = String(reader.result || '')
    if (!articleForm.title && file.name) {
      // 用文件名(去扩展名)作为默认标题
      articleForm.title = file.name.replace(/\.(md|markdown|txt)$/i, '')
    }
    ElMessage.success('文件导入成功')
  }
  reader.onerror = () => ElMessage.error('文件读取失败,请重试')
  reader.readAsText(file, 'UTF-8')
}

// ---------- 工具函数 ----------
// 标签字符串转数组
const splitTags = (tags) => {
  if (!tags) return []
  return String(tags).split(/[,，]/).map(t => t.trim()).filter(Boolean)
}

// 时间格式化
const formatTime = (t) => {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

const loadAll = () => {
  loadCategories()
  loadArticles()
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.kb-manage {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.page-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1a202c;
  margin: 0 0 4px;
}
.page-header .page-desc {
  font-size: 14px;
  color: #718096;
  margin: 0;
}
.page-header .el-button {
  width: 40px;
  height: 40px;
}

/* 卡片 */
.chart-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  margin-bottom: 24px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.card-head h3 {
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
  margin: 0;
}
.category-card {
  margin-bottom: 24px;
}
.category-name {
  font-weight: 600;
  color: #2d3748;
}

/* 文章工具栏 */
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}
.search-input {
  width: 260px;
}
.article-title {
  font-weight: 600;
  color: #2d3748;
}
.text-muted {
  color: #a0aec0;
  font-size: 12px;
}
.tag-chip {
  margin-right: 4px;
  margin-bottom: 2px;
}
.cover-thumb {
  width: 56px;
  height: 40px;
  border-radius: 6px;
  display: block;
  margin: 0 auto;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 文章内容编辑 */
.content-editor {
  width: 100%;
}
.content-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.content-hint {
  font-size: 12px;
  color: #a0aec0;
}

/* 响应式 */
@media (max-width: 900px) {
  .toolbar {
    flex-wrap: wrap;
  }
  .search-input {
    width: 180px;
  }
}
</style>
