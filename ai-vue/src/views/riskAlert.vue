<template>
  <div class="risk-alert">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1>🚨 风险预警</h1>
        <p class="page-desc">系统根据用户情绪日记自动识别持续低落与高危情绪,支持主动干预处理</p>
      </div>
      <div class="header-right">
        <el-button @click="loadAll" :loading="loading" circle>
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stats-row">
      <div class="stat-card pending-card">
        <div class="stat-top">
          <div class="stat-icon-wrap"><span class="stat-icon">🔔</span></div>
        </div>
        <div class="stat-num">{{ stats.pending || 0 }}</div>
        <div class="stat-label">待处理预警</div>
      </div>
      <div class="stat-card today-card">
        <div class="stat-top">
          <div class="stat-icon-wrap"><span class="stat-icon">📅</span></div>
        </div>
        <div class="stat-num">{{ stats.todayNew || 0 }}</div>
        <div class="stat-label">今日新增</div>
      </div>
      <div class="stat-card handled-card">
        <div class="stat-top">
          <div class="stat-icon-wrap"><span class="stat-icon">✅</span></div>
        </div>
        <div class="stat-num">{{ stats.handled || 0 }}</div>
        <div class="stat-label">已处理</div>
      </div>
      <div class="stat-card total-card">
        <div class="stat-top">
          <div class="stat-icon-wrap"><span class="stat-icon">📈</span></div>
        </div>
        <div class="stat-num">{{ stats.total || 0 }}</div>
        <div class="stat-label">累计预警</div>
      </div>
    </div>

    <!-- 预警列表 -->
    <div class="chart-card">
      <div class="table-toolbar">
        <el-radio-group v-model="query.status" @change="handleSearch">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="0">待处理</el-radio-button>
          <el-radio-button :value="1">已处理</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="alerts" v-loading="loading" stripe>
        <el-table-column label="预警用户" min-width="140" fixed>
          <template #default="{ row }">
            <div class="user-cell">
              <span class="username">{{ row.username }}</span>
              <span class="nickname">{{ row.nickname || '—' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预警类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.alertType === 'HIGH_RISK_EMOTION'" type="danger" effect="light">高危情绪</el-tag>
            <el-tag v-else type="warning" effect="light">连续低分</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.alertLevel)" effect="dark" size="small">{{ row.alertLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="触发原因" min-width="240" show-overflow-tooltip />
        <el-table-column prop="relatedDate" label="关联日期" width="110" align="center">
          <template #default="{ row }">{{ row.relatedDate || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="产生时间" width="165">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" effect="light">已处理</el-tag>
            <el-tag v-else type="danger" effect="dark">待处理</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="primary" link @click="openHandle(row)">处理</el-button>
            <el-button size="small" type="info" link @click="openDiaries(row)">查看日记</el-button>
            <el-button v-if="row.status === 1" size="small" type="success" link @click="showRemark(row)">处理记录</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadAlerts"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 处理弹窗 -->
    <el-dialog v-model="handleVisible" title="处理风险预警" width="480px" destroy-on-close>
      <div class="handle-info" v-if="currentAlert">
        <p><b>用户：</b>{{ currentAlert.username }}（{{ currentAlert.nickname || '—' }}）</p>
        <p><b>原因：</b>{{ currentAlert.reason }}</p>
      </div>
      <el-input
        v-model="handleRemark"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        placeholder="填写处理备注,如:已电话回访,情况稳定(留空则默认记录'已回访处理')"
      />
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmHandle">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 用户日记抽屉 -->
    <el-drawer v-model="diaryVisible" :title="diaryTitle" size="52%">
      <el-table :data="diaries" v-loading="diaryLoading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="diary-expand">
              <p v-if="row.diaryContent"><b>日记内容：</b>{{ row.diaryContent }}</p>
              <p v-if="row.emotionTriggers"><b>情绪触发因素：</b>{{ row.emotionTriggers }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="diaryDate" label="日记日期" width="120" />
        <el-table-column label="情绪评分" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="moodTagType(row.moodScore)" effect="light">{{ row.moodScore }}/10</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dominantEmotion" label="主要情绪" width="110">
          <template #default="{ row }">{{ row.dominantEmotion || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!diaryLoading && diaries.length === 0" description="该用户还没有情绪日记" :image-size="80" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getAlertPage, getAlertStats, handleAlert, getAdminUserDiaries } from '@/api/admin.js'

const loading = ref(false)
const saving = ref(false)
const stats = reactive({ total: 0, pending: 0, handled: 0, todayNew: 0 })

const query = reactive({ status: null, page: 1, size: 10 })
const total = ref(0)
const alerts = ref([])

// ---------- 列表与统计 ----------
const loadAlerts = async () => {
  loading.value = true
  try {
    const res = await getAlertPage({ status: query.status, page: query.page, size: query.size })
    alerts.value = res.records || []
    total.value = Number(res.total) || 0
  } catch (e) {
    alerts.value = []
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    Object.assign(stats, await getAlertStats() || {})
  } catch (e) { /* 忽略 */ }
}

const loadAll = () => {
  loadStats()
  loadAlerts()
}

const handleSearch = () => {
  query.page = 1
  loadAlerts()
}

const handleSizeChange = () => {
  query.page = 1
  loadAlerts()
}

// ---------- 处理预警 ----------
const handleVisible = ref(false)
const currentAlert = ref(null)
const handleRemark = ref('')

const openHandle = (row) => {
  currentAlert.value = row
  handleRemark.value = ''
  handleVisible.value = true
}

const confirmHandle = async () => {
  saving.value = true
  try {
    await handleAlert(currentAlert.value.id, handleRemark.value)
    ElMessage.success('预警已处理')
    handleVisible.value = false
    loadAll()
  } catch (e) { /* 错误提示由拦截器统一处理 */ } finally {
    saving.value = false
  }
}

// 查看已处理记录的备注
const showRemark = (row) => {
  ElMessage.info(`处理备注：${row.handleRemark || '未填写'}`)
}

// ---------- 查看用户日记 ----------
const diaryVisible = ref(false)
const diaryLoading = ref(false)
const diaries = ref([])
const diaryUser = ref(null)

const diaryTitle = computed(() => diaryUser.value ? `情绪日记：${diaryUser.value.username}` : '情绪日记')

const openDiaries = async (row) => {
  diaryUser.value = row
  diaryVisible.value = true
  diaryLoading.value = true
  try {
    diaries.value = await getAdminUserDiaries(row.userId) || []
  } catch (e) {
    diaries.value = []
  } finally {
    diaryLoading.value = false
  }
}

// ---------- 工具函数 ----------
const formatTime = (t) => {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

// 等级标签颜色
const levelTagType = (level) => {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

// 情绪评分颜色
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
.risk-alert {
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

/* 指标卡片 */
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
.pending-card .stat-icon-wrap {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
}
.today-card .stat-icon-wrap {
  background: linear-gradient(135deg, #f6d365 0%, #fda085 100%);
}
.handled-card .stat-icon-wrap {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}
.total-card .stat-icon-wrap {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
  margin-bottom: 16px;
}
.user-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}
.user-cell .username {
  font-weight: 600;
  color: #2d3748;
}
.user-cell .nickname {
  font-size: 12px;
  color: #a0aec0;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 处理弹窗 */
.handle-info {
  margin-bottom: 12px;
  line-height: 1.8;
  font-size: 14px;
  color: #4a5568;
}
.handle-info p {
  margin: 0;
}

/* 日记展开 */
.diary-expand {
  padding: 8px 16px;
  line-height: 1.8;
  font-size: 13px;
  color: #4a5568;
}
.diary-expand p {
  margin: 4px 0;
}

@media (max-width: 900px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
