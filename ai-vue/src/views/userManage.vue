<template>
  <div class="user-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1>👥 用户管理</h1>
        <p class="page-desc">管理全部注册用户,查看用户详情与历史记录,支持启用/禁用账号</p>
      </div>
      <div class="header-right">
        <el-button @click="loadAll" :loading="loading" circle>
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stats-row">
      <div class="stat-card users-card">
        <div class="stat-top">
          <div class="stat-icon-wrap"><span class="stat-icon">👥</span></div>
        </div>
        <div class="stat-num">{{ stats.totalUsers || 0 }}</div>
        <div class="stat-label">注册用户总数</div>
      </div>
      <div class="stat-card normal-card">
        <div class="stat-top">
          <div class="stat-icon-wrap"><span class="stat-icon">✅</span></div>
        </div>
        <div class="stat-num">{{ stats.normalUsers || 0 }}</div>
        <div class="stat-label">正常账号</div>
      </div>
      <div class="stat-card disabled-card">
        <div class="stat-top">
          <div class="stat-icon-wrap"><span class="stat-icon">🚫</span></div>
        </div>
        <div class="stat-num">{{ stats.disabledUsers || 0 }}</div>
        <div class="stat-label">禁用账号</div>
      </div>
      <div class="stat-card admin-card">
        <div class="stat-top">
          <div class="stat-icon-wrap"><span class="stat-icon">🛡️</span></div>
        </div>
        <div class="stat-num">{{ stats.adminUsers || 0 }}</div>
        <div class="stat-label">管理员数量</div>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="chart-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名 / 昵称 / 邮箱 / 手机号"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <el-table :data="userList" v-loading="loading" stripe class="user-table">
        <el-table-column prop="username" label="用户账号" min-width="120" fixed>
          <template #default="{ row }">
            <span class="username-text">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="110" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="120">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="用户类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.userType === 2" type="warning" effect="light">管理员</el-tag>
            <el-tag v-else type="info" effect="plain">普通用户</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" effect="light">正常</el-tag>
            <el-tag v-else type="danger" effect="light">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="lastActiveTime" label="最近活跃时间" min-width="160">
          <template #default="{ row }">
            <span v-if="row.lastActiveTime">{{ formatTime(row.lastActiveTime) }}</span>
            <span v-else class="text-muted">从未活跃</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDetail(row)">
              <el-icon><View /></el-icon>&nbsp;详情
            </el-button>
            <el-button
              v-if="row.userType !== 2 && row.id !== currentUserId"
              size="small"
              :type="row.status === 1 ? 'danger' : 'success'"
              link
              @click="handleToggleStatus(row)"
            >
              <el-icon><component :is="row.status === 1 ? 'Lock' : 'Unlock'" /></el-icon>&nbsp;
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadUsers"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 用户详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="detailTitle" size="58%">
      <div class="detail-wrap" v-if="detail">
        <!-- 基本信息 -->
        <div class="detail-section">
          <h4 class="section-title">基本信息</h4>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="用户账号">{{ detail.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ detail.nickname || '—' }}</el-descriptions-item>
            <el-descriptions-item label="用户类型">
              {{ detail.userType === 2 ? '管理员' : '普通用户' }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ detail.phone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag v-if="detail.status === 1" type="success" size="small">正常</el-tag>
              <el-tag v-else type="danger" size="small">禁用</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="最近活跃时间">
              {{ detail.lastActiveTime ? formatTime(detail.lastActiveTime) : '从未活跃' }}
            </el-descriptions-item>
            <el-descriptions-item label="数据概览">
              <span class="count-chip">日记 {{ detail.diaryCount || 0 }}</span>
              <span class="count-chip">会话 {{ detail.sessionCount || 0 }}</span>
              <span class="count-chip">收藏 {{ detail.favoriteCount || 0 }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 历史记录 Tabs -->
        <div class="detail-section">
          <el-tabs v-model="activeTab">
            <!-- 历史情绪日记 -->
            <el-tab-pane label="历史情绪日记" name="diaries">
              <el-table :data="diaries" v-loading="diaryLoading" stripe max-height="420">
                <el-table-column type="expand">
                  <template #default="{ row }">
                    <div class="diary-expand">
                      <p v-if="row.diaryContent"><b>日记内容：</b>{{ row.diaryContent }}</p>
                      <p v-if="row.emotionTriggers"><b>情绪触发因素：</b>{{ row.emotionTriggers }}</p>
                      <p v-if="!row.diaryContent && !row.emotionTriggers" class="text-muted">该日记未填写详细内容</p>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="diaryDate" label="日记日期" width="120" />
                <el-table-column label="情绪评分" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="moodTagType(row.moodScore)" effect="light">{{ row.moodScore }}/10</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="dominantEmotion" label="主要情绪" width="100">
                  <template #default="{ row }">{{ row.dominantEmotion || '—' }}</template>
                </el-table-column>
                <el-table-column label="睡眠质量" width="90" align="center">
                  <template #default="{ row }">{{ row.sleepQuality ? row.sleepQuality + '/5' : '—' }}</template>
                </el-table-column>
                <el-table-column label="压力水平" width="90" align="center">
                  <template #default="{ row }">{{ row.stressLevel ? row.stressLevel + '/5' : '—' }}</template>
                </el-table-column>
                <el-table-column prop="createdAt" label="提交时间" min-width="160">
                  <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!diaryLoading && diaries.length === 0" description="该用户还没有情绪日记" :image-size="80" />
            </el-tab-pane>

            <!-- 历史AI咨询记录 -->
            <el-tab-pane label="历史AI咨询记录" name="sessions">
              <el-table :data="sessions" v-loading="sessionLoading" stripe max-height="420">
                <el-table-column prop="sessionTitle" label="会话标题" min-width="220" show-overflow-tooltip />
                <el-table-column prop="startedAt" label="开始时间" width="170">
                  <template #default="{ row }">{{ formatTime(row.startedAt) }}</template>
                </el-table-column>
                <el-table-column prop="messageCount" label="消息数" width="90" align="center" />
                <el-table-column label="操作" width="110" align="center">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" link @click="viewMessages(row)">查看对话</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!sessionLoading && sessions.length === 0" description="该用户还没有咨询记录" :image-size="80" />

              <!-- 对话消息面板 -->
              <div v-if="currentSession" class="chat-panel">
                <div class="chat-panel-header">
                  <span class="chat-panel-title">对话详情：{{ currentSession.sessionTitle }}</span>
                  <el-button size="small" circle @click="closeMessages">
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
                <div class="chat-panel-body" v-loading="messageLoading">
                  <div
                    v-for="msg in messages"
                    :key="msg.id"
                    class="chat-row"
                    :class="{ 'chat-row-ai': msg.senderType === 2 }"
                  >
                    <div class="chat-avatar">{{ msg.senderType === 2 ? '🤖' : '🧑' }}</div>
                    <div class="chat-bubble">
                      <div class="chat-content">{{ msg.content }}</div>
                      <div class="chat-time">{{ formatTime(msg.createdAt) }}</div>
                    </div>
                  </div>
                  <el-empty v-if="!messageLoading && messages.length === 0" description="暂无消息" :image-size="60" />
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, View, Close, Lock, Unlock } from '@element-plus/icons-vue'
import {
  getAdminUserPage,
  getAdminUserStats,
  getAdminUserDetail,
  getAdminUserDiaries,
  getAdminUserSessions,
  getAdminSessionMessages,
  updateAdminUserStatus
} from '@/api/admin.js'

// ---------- 基础状态 ----------
const loading = ref(false)
const stats = reactive({ totalUsers: 0, normalUsers: 0, disabledUsers: 0, adminUsers: 0 })

const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const userList = ref([])

// 当前登录管理员ID(用于禁止操作自己)
const currentUserId = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}').id
  } catch {
    return null
  }
})

// ---------- 列表加载 ----------
const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getAdminUserPage({ keyword: keyword.value, page: page.value, size: size.value })
    userList.value = res.records || []
    total.value = Number(res.total) || 0
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await getAdminUserStats()
    Object.assign(stats, res || {})
  } catch (e) { /* 忽略 */ }
}

const loadAll = () => {
  loadStats()
  loadUsers()
}

// 搜索(重置页码)
const handleSearch = () => {
  page.value = 1
  loadUsers()
}

// 修改每页条数
const handleSizeChange = () => {
  page.value = 1
  loadUsers()
}

// ---------- 启用/禁用 ----------
const handleToggleStatus = (row) => {
  const action = row.status === 1 ? '禁用' : '启用'
  ElMessageBox.confirm(
    `确定要${action}用户「${row.username}」的账号吗？`,
    `${action}确认`,
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    const targetStatus = row.status === 1 ? 0 : 1
    await updateAdminUserStatus(row.id, targetStatus)
    ElMessage.success(`已${action}账号「${row.username}」`)
    loadAll()
  }).catch(() => {})
}

// ---------- 用户详情抽屉 ----------
const detailVisible = ref(false)
const detail = ref(null)
const activeTab = ref('diaries')
const diaries = ref([])
const diaryLoading = ref(false)
const sessions = ref([])
const sessionLoading = ref(false)
const currentSession = ref(null)
const messages = ref([])
const messageLoading = ref(false)

const detailTitle = computed(() => detail.value ? `用户详情：${detail.value.username}` : '用户详情')

// 打开详情抽屉并加载历史数据
const openDetail = async (row) => {
  detail.value = row
  detailVisible.value = true
  activeTab.value = 'diaries'
  currentSession.value = null
  messages.value = []

  // 并行加载详情、日记、会话
  diaryLoading.value = true
  sessionLoading.value = true
  try {
    const [detailRes, diaryRes, sessionRes] = await Promise.all([
      getAdminUserDetail(row.id),
      getAdminUserDiaries(row.id),
      getAdminUserSessions(row.id)
    ])
    detail.value = detailRes || row
    diaries.value = diaryRes || []
    sessions.value = sessionRes || []
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    diaryLoading.value = false
    sessionLoading.value = false
  }
}

// 查看某条会话的对话消息
const viewMessages = async (session) => {
  currentSession.value = session
  messageLoading.value = true
  try {
    messages.value = await getAdminSessionMessages(detail.value.id, session.id) || []
  } catch (e) {
    messages.value = []
  } finally {
    messageLoading.value = false
  }
}

// 关闭对话面板
const closeMessages = () => {
  currentSession.value = null
  messages.value = []
}

// ---------- 工具函数 ----------
// 时间格式化(后端 jackson 已输出 yyyy-MM-dd HH:mm:ss,这里仅做兼容处理)
const formatTime = (t) => {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

// 情绪评分标签颜色: >=8 绿色 >=5 橙色 其余红色
const moodTagType = (score) => {
  if (score >= 8) return 'success'
  if (score >= 5) return 'warning'
  return 'danger'
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.user-manage {
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

/* 指标卡片行 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}
.stat-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-icon {
  font-size: 24px;
}
.users-card .stat-icon-wrap {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.normal-card .stat-icon-wrap {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}
.disabled-card .stat-icon-wrap {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}
.admin-card .stat-icon-wrap {
  background: linear-gradient(135deg, #30cfd0 0%, #330867 100%);
}
.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #1a202c;
  line-height: 1;
  margin-bottom: 4px;
}
.stat-label {
  font-size: 14px;
  color: #4a5568;
  font-weight: 500;
}

/* 列表卡片 */
.chart-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.toolbar-left {
  display: flex;
  gap: 12px;
}
.search-input {
  width: 320px;
}
.username-text {
  font-weight: 600;
  color: #2d3748;
}
.text-muted {
  color: #a0aec0;
  font-size: 12px;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 详情抽屉 */
.detail-wrap {
  padding: 0 4px;
}
.detail-section {
  margin-bottom: 20px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #2d3748;
  margin: 0 0 12px;
}
.count-chip {
  display: inline-block;
  background: #edf2f7;
  color: #4a5568;
  border-radius: 10px;
  padding: 2px 10px;
  font-size: 12px;
  margin-right: 6px;
}
.diary-expand {
  padding: 8px 16px;
  line-height: 1.8;
  font-size: 13px;
  color: #4a5568;
}
.diary-expand p {
  margin: 4px 0;
}

/* 对话面板 */
.chat-panel {
  margin-top: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}
.chat-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #f7fafc;
  border-bottom: 1px solid #e2e8f0;
}
.chat-panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #2d3748;
}
.chat-panel-body {
  max-height: 360px;
  overflow-y: auto;
  padding: 16px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.chat-row {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.chat-row-ai {
  flex-direction: row;
}
.chat-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.chat-bubble {
  max-width: 70%;
  background: #4fd1c5;
  color: #1a202c;
  border-radius: 12px 4px 12px 12px;
  padding: 10px 14px;
}
.chat-row-ai .chat-bubble {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 4px 12px 12px 12px;
}
.chat-content {
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-time {
  font-size: 11px;
  opacity: 0.6;
  margin-top: 4px;
  text-align: right;
}

/* 响应式 */
@media (max-width: 900px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .search-input {
    width: 220px;
  }
}
</style>
