<template>
  <div class="knowledge-page">
    <!-- Hero 区 -->
    <div class="hero">
      <div class="hero-inner">
        <h1>心理健康知识库</h1>
        <p>精选心理学科普文章,陪伴你每一个需要被理解的时刻</p>
      </div>
    </div>

    <!-- 标签页:全部 / 我的收藏 -->
    <div class="tab-bar">
      <el-radio-group v-model="activeTab" size="large" @change="handleTabChange">
        <el-radio-button value="all">📚 全部文章</el-radio-button>
        <el-radio-button value="favorites">⭐ 我的收藏</el-radio-button>
      </el-radio-group>
      <div class="search-box" v-if="activeTab === 'all'">
        <el-input
          v-model="keyword"
          placeholder="搜索文章标题、标签..."
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
          @blur="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <!-- 分类筛选 -->
    <div class="category-bar" v-if="activeTab === 'all'">
      <el-tag
        :type="categoryId === null ? 'primary' : ''"
        effect="dark"
        class="cat-tag"
        @click.native="selectCategory(null)"
      >全部</el-tag>
      <el-tag
        v-for="cat in categories"
        :key="cat.id"
        :type="categoryId === cat.id ? 'primary' : ''"
        effect="dark"
        class="cat-tag"
        @click.native="selectCategory(cat.id)"
      >{{ cat.categoryName }}</el-tag>
    </div>

    <!-- 文章列表 -->
    <div class="article-grid" v-loading="loading">
      <template v-if="activeTab === 'all'">
        <div
          v-for="article in articles"
          :key="article.id"
          class="article-card"
          @click="openDetail(article)"
        >
          <div class="card-cover" :style="!article.coverImage ? getCoverStyle(article) : {}">
            <img v-if="article.coverImage" :src="article.coverImage" :alt="article.title" class="cover-img" loading="lazy" />
            <span v-else class="cover-emoji">{{ getCoverEmoji(article.categoryId) }}</span>
            <span class="cat-badge">{{ getCategoryName(article.categoryId) }}</span>
            <span class="read-count">👁 {{ article.readCount || 0 }}</span>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ article.title }}</h3>
            <p class="card-summary">{{ article.summary }}</p>
            <div class="card-footer">
              <div class="tags" v-if="article.tags">
                <el-tag
                  v-for="(tag, idx) in parseTags(article.tags).slice(0, 3)"
                  :key="idx"
                  size="small"
                  type="info"
                  effect="plain"
                >{{ tag }}</el-tag>
              </div>
              <span class="publish-time">{{ formatTime(article.publishedAt) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && articles.length === 0" description="暂无文章" />
      </template>
      <template v-else>
        <div
          v-for="item in favorites"
          :key="item.articleId"
          class="article-card"
          @click="openFavoriteDetail(item)"
        >
          <div class="card-cover" :style="!item.coverImage ? getCoverStyle(item) : {}">
            <img v-if="item.coverImage" :src="item.coverImage" :alt="item.title" class="cover-img" loading="lazy" />
            <span v-else class="cover-emoji">{{ getCoverEmoji(item.categoryId) }}</span>
            <span class="cat-badge">{{ item.categoryName }}</span>
            <span class="read-count">👁 {{ item.readCount || 0 }}</span>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-summary">{{ item.summary }}</p>
            <div class="card-footer">
              <div class="tags" v-if="item.tags">
                <el-tag
                  v-for="(tag, idx) in parseTags(item.tags).slice(0, 3)"
                  :key="idx"
                  size="small"
                  type="info"
                  effect="plain"
                >{{ tag }}</el-tag>
              </div>
              <span class="publish-time">收藏于 {{ formatTime(item.favoritedAt) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && favorites.length === 0" description="还没有收藏任何文章,快去收藏几篇吧!" />
      </template>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="activeTab === 'all' && total > pageSize">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        background
        @current-change="loadArticles"
      />
    </div>

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="detailVisible"
      :title="detailArticle?.title || '文章详情'"
      size="60%"
      direction="rtl"
      destroy-on-close
    >
      <div class="detail-cover" :style="!detailArticle?.coverImage ? getCoverStyle(detailArticle) : {}">
        <img v-if="detailArticle?.coverImage" :src="detailArticle.coverImage" :alt="detailArticle?.title" />
        <span v-else class="detail-cover-emoji">{{ getCoverEmoji(detailArticle?.categoryId) }}</span>
      </div>
      <div class="detail-header" v-if="detailArticle">
        <el-tag type="primary" effect="dark">{{ detailCategoryName }}</el-tag>
        <span class="detail-meta">
          👁 {{ detailReadCount }} 阅读 · {{ formatTime(detailArticle.publishedAt) }}
        </span>
        <el-button
          :type="isFavorited ? 'warning' : 'primary'"
          :icon="isFavorited ? StarFilled : Star"
          @click="toggleFavorite"
        >
          {{ isFavorited ? '取消收藏' : '收藏文章' }}
        </el-button>
      </div>
      <div class="detail-tags" v-if="detailArticle?.tags">
        <el-tag
          v-for="(tag, idx) in parseTags(detailArticle.tags)"
          :key="idx"
          type="info"
          effect="plain"
          style="margin-right: 6px"
        >{{ tag }}</el-tag>
      </div>
      <div class="detail-content" v-html="detailArticle?.content"></div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Star, StarFilled
} from '@element-plus/icons-vue'
import {
  getKnowledgeCategories,
  getKnowledgeArticles,
  getKnowledgeArticleDetail,
  checkKnowledgeFavorited,
  favoriteKnowledge,
  unfavoriteKnowledge,
  getKnowledgeFavorites
} from '@/api/admin.js'

const activeTab = ref('all')
const keyword = ref('')
const categories = ref([])
const categoryId = ref(null)
const articles = ref([])
const favorites = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(9)
const loading = ref(false)

const detailVisible = ref(false)
const detailArticle = ref(null)
const detailCategoryName = ref('')
const detailReadCount = ref(0)
const isFavorited = ref(false)

/* ---------- 分类 ---------- */
const loadCategories = async () => {
  try {
    const data = await getKnowledgeCategories()
    categories.value = data || []
  } catch (e) { /* 公开接口不弹窗 */ }
}

const getCategoryName = (id) => {
  const cat = categories.value.find(c => c.id === id)
  return cat ? cat.categoryName : ''
}

/* ---------- 文章列表 ---------- */
const loadArticles = async () => {
  loading.value = true
  try {
    const data = await getKnowledgeArticles({
      categoryId: categoryId.value || undefined,
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size: pageSize.value
    })
    articles.value = data.records || []
    total.value = data.total || 0
  } catch (e) { /* */ }
  loading.value = false
}

const selectCategory = (id) => {
  categoryId.value = id
  page.value = 1
  loadArticles()
}

const handleSearch = () => {
  page.value = 1
  loadArticles()
}

/* ---------- 收藏列表 ---------- */
const loadFavorites = async () => {
  loading.value = true
  try {
    const data = await getKnowledgeFavorites()
    favorites.value = data || []
  } catch (e) { /* 未登录会被拦截,静默 */ }
  loading.value = false
}

/* ---------- Tab 切换 ---------- */
const handleTabChange = (tab) => {
  if (tab === 'favorites') {
    loadFavorites()
  } else {
    loadArticles()
  }
}

/* ---------- 详情 ---------- */
const openDetail = async (article) => {
  try {
    const data = await getKnowledgeArticleDetail(article.id)
    detailArticle.value = data.article
    detailCategoryName.value = data.categoryName || ''
    detailReadCount.value = data.article.readCount || 0
    isFavorited.value = await checkFavStatus(article.id)
    detailVisible.value = true
  } catch (e) { /* */ }
}

const openFavoriteDetail = async (item) => {
  try {
    const data = await getKnowledgeArticleDetail(item.articleId)
    detailArticle.value = data.article
    detailCategoryName.value = data.categoryName || ''
    detailReadCount.value = data.article.readCount || 0
    isFavorited.value = await checkFavStatus(item.articleId)
    detailVisible.value = true
  } catch (e) { /* */ }
}

const checkFavStatus = async (id) => {
  try {
    const data = await checkKnowledgeFavorited(id)
    return data || false
  } catch (e) {
    return false
  }
}

const toggleFavorite = async () => {
  if (!detailArticle.value) return
  const id = detailArticle.value.id
  try {
    if (isFavorited.value) {
      await unfavoriteKnowledge(id)
      ElMessage.success('已取消收藏')
      isFavorited.value = false
    } else {
      await favoriteKnowledge(id)
      ElMessage.success('收藏成功')
      isFavorited.value = true
    }
    // 如果在收藏 tab,刷新列表
    if (activeTab.value === 'favorites') {
      loadFavorites()
    }
  } catch (e) { /* request 拦截器已提示 */ }
}

/* ---------- 工具函数 ---------- */
const parseTags = (tagStr) => {
  if (!tagStr) return []
  return tagStr.split(',').map(t => t.trim()).filter(Boolean)
}

const formatTime = (dt) => {
  if (!dt) return ''
  const d = new Date(dt)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 分类对应的渐变颜色
const categoryGradients = {
  1: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',  // 心理健康基础 - 紫色
  2: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',  // 情绪管理 - 粉红色
  3: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',  // 压力缓解 - 蓝色
  4: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',  // 人际关系 - 橙黄色
}

// 分类对应的 emoji
const categoryEmojis = {
  1: '🧠',  // 心理健康基础
  2: '💭',  // 情绪管理
  3: '🌊',  // 压力缓解
  4: '🤝',  // 人际关系
}

const getCoverStyle = (article) => {
  if (!article) return categoryGradients[1]
  // 根据分类返回对应渐变色
  const gradient = categoryGradients[article.categoryId]
  if (gradient) return gradient
  // 如果没有分类，使用 ID 哈希选择
  const keys = Object.keys(categoryGradients)
  const idx = article.id ? Math.abs(hashCode(article.id)) % keys.length : 0
  return categoryGradients[keys[idx]]
}

const getCoverEmoji = (categoryId) => {
  return categoryEmojis[categoryId] || '✨'
}

const hashCode = (str) => {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = ((h << 5) - h) + str.charCodeAt(i)
    h |= 0
  }
  return h
}

/* ---------- 初始化 ---------- */
onMounted(() => {
  loadCategories()
  loadArticles()
})

// 当详情抽屉打开时,检查登录状态(收藏功能需要)
watch(detailVisible, (val) => {
  if (val) {
    const token = localStorage.getItem('token')
    if (!token) {
      // 不强制跳登录,但提示收藏功能需要登录
    }
  }
})
</script>

<style scoped lang="scss">
.knowledge-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 60px;

  .hero {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    padding: 48px 40px;
    margin-bottom: 24px;
    color: #fff;
    .hero-inner {
      h1 { font-size: 32px; margin: 0 0 8px; }
      p { font-size: 16px; opacity: 0.9; margin: 0; }
    }
  }

  .tab-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    .search-box {
      display: flex;
      gap: 8px;
      width: 360px;
    }
  }

  .category-bar {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 24px;
    .cat-tag {
      cursor: pointer;
      font-size: 14px;
      padding: 6px 16px;
      border-radius: 20px;
      transition: all 0.2s;
      &:hover { transform: translateY(-1px); }
    }
  }

  .article-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;

    .article-card {
      background: #fff;
      border-radius: 12px;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.3s;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      }
      .card-cover {
        height: 140px;
        position: relative;
        display: flex;
        align-items: flex-end;
        padding: 10px 14px;
        overflow: hidden;
        transition: transform 0.3s;
        &:hover {
          transform: scale(1.05);
        }
        .cover-img {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          object-fit: cover;
          z-index: 0;
          transition: transform 0.4s;
        }
        &:hover .cover-img {
          transform: scale(1.08);
        }
        .cover-emoji {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -60%);
          font-size: 48px;
          opacity: 0.6;
          filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.2));
        }
        .cat-badge {
          position: relative;
          z-index: 1;
          background: rgba(255, 255, 255, 0.85);
          color: #333;
          padding: 3px 10px;
          border-radius: 12px;
          font-size: 12px;
          font-weight: 500;
        }
        .read-count {
          position: absolute;
          top: 10px;
          right: 12px;
          color: #fff;
          font-size: 13px;
          text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
        }
      }
      .card-body {
        padding: 16px;
        .card-title {
          font-size: 16px;
          font-weight: 600;
          margin: 0 0 8px;
          line-height: 1.4;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .card-summary {
          color: #666;
          font-size: 13px;
          line-height: 1.5;
          margin: 0 0 12px;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .card-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;
          .tags {
            display: flex;
            gap: 4px;
            flex-wrap: wrap;
          }
          .publish-time {
            color: #999;
            font-size: 12px;
          }
        }
      }
    }
  }

  .pagination {
    display: flex;
    justify-content: center;
    margin-top: 32px;
  }

  .detail-cover {
    margin: -20px -20px 20px;
    height: 220px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 0;
    overflow: hidden;
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .detail-cover-emoji {
      font-size: 100px;
      opacity: 0.5;
      filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.3));
    }
  }
  .detail-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 16px;
    border-bottom: 2px solid #f0f0f0;
    margin-bottom: 20px;
    .detail-meta {
      color: #888;
      font-size: 14px;
      flex: 1;
    }
  }
  .detail-tags {
    margin-bottom: 20px;
  }
  .detail-content {
    line-height: 2;
    font-size: 16px;
    color: #333;
    
    :deep(h2) {
      font-size: 26px;
      font-weight: 700;
      margin: 32px 0 20px;
      padding: 16px 20px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      border-radius: 12px;
      box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
      position: relative;
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        width: 4px;
        height: 100%;
        background: #fff;
        border-radius: 4px 0 0 4px;
      }
    }
    
    :deep(h3) {
      font-size: 22px;
      font-weight: 600;
      margin: 28px 0 16px;
      color: #2d3748;
      padding-left: 16px;
      border-left: 4px solid #667eea;
      line-height: 1.3;
    }
    
    :deep(h4) {
      font-size: 18px;
      font-weight: 600;
      margin: 24px 0 14px;
      color: #4a5568;
      display: flex;
      align-items: center;
      &::before {
        content: '💡';
        margin-right: 8px;
      }
    }
    
    :deep(p) {
      margin: 16px 0;
      padding: 0 4px;
      text-indent: 2em;
      &:first-of-type {
        margin-top: 8px;
      }
    }
    
    :deep(ul), :deep(ol) {
      padding-left: 24px;
      margin: 16px 0;
      li {
        margin-bottom: 12px;
        padding: 8px 14px;
        background: #f7fafc;
        border-radius: 8px;
        transition: all 0.2s;
        line-height: 1.6;
        &:hover {
          background: #edf2f7;
          transform: translateX(4px);
        }
        strong {
          color: #667eea;
        }
      }
    }
    
    :deep(ol) {
      counter-reset: item;
      list-style: none;
      li {
        counter-increment: item;
        position: relative;
        padding-left: 50px;
        &::before {
          content: counter(item);
          position: absolute;
          left: 12px;
          top: 50%;
          transform: translateY(-50%);
          width: 28px;
          height: 28px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: white;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 14px;
          font-weight: 600;
        }
      }
    }
    
    :deep(ul) {
      li {
        position: relative;
        padding-left: 32px;
        &::before {
          content: '✓';
          position: absolute;
          left: 12px;
          top: 50%;
          transform: translateY(-50%);
          color: #48bb78;
          font-weight: bold;
        }
      }
    }
  }
}

@media (max-width: 900px) {
  .knowledge-page .article-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 600px) {
  .knowledge-page .article-grid {
    grid-template-columns: 1fr;
  }
}
</style>
