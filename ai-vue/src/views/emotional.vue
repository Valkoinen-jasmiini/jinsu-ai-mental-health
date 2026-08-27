<template>
  <div class="emo-page">
    <div class="emo-inner">
      <!-- 顶部 Hero + 写入按钮 -->
      <section class="hero">
        <div class="hero-left">
          <div class="hero-badge">记录此刻 · 看到自己</div>
          <h1 class="hero-title">情绪日记</h1>
          <p class="hero-desc">每一天的心情都值得被看见。在这里快速记录一点感受,让它们沉淀成温柔的时光脚印。</p>
          <div class="hero-actions">
            <el-button
              type="primary"
              size="large"
              class="hero-btn"
              @click="openWriteDialog"
            >
              <el-icon><Edit /></el-icon>
              <span>写一条心情记录</span>
            </el-button>
          </div>
        </div>
        <div class="hero-right">
          <div class="today-card" :style="{ '--today-col': todayColor }">
            <div class="today-head">今日心情</div>
            <div class="today-body">
              <div class="today-score" v-if="today.avgScore != null">{{ today.avgScore }}</div>
              <div class="today-score empty" v-else>—</div>
              <div class="today-label" :class="{ empty: today.avgScore == null }">
                {{ today.topLabel || '未记录' }}
              </div>
            </div>
            <div class="today-foot">
              今天已记录 <b>{{ today.recordCount }}</b> 条
            </div>
          </div>
        </div>
      </section>

      <!-- 月度概览3卡 -->
      <section class="stats">
        <div class="stat-card">
          <div class="stat-label">本月平均心情分</div>
          <div class="stat-val big">
            <template v-if="stats.avgScore != null">{{ stats.avgScore }}</template>
            <template v-else>—</template>
          </div>
          <div class="stat-sub">
            <el-progress
              v-if="stats.avgScore != null"
              :percentage="stats.avgScore"
              :color="scoreColor(stats.avgScore)"
              :show-text="false"
              :stroke-width="8"
            />
            <span v-else class="gray">暂无记录</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">本月最多心情</div>
          <div class="stat-val mood-chip" :style="chipStyle(stats.topColor)">
            {{ stats.topLabel }}
          </div>
          <div class="stat-sub gray">{{ monthText }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">本月累计记录</div>
          <div class="stat-val big">{{ stats.recordCount }}</div>
          <div class="stat-sub gray">
            连续 {{ streakDays }} 天记日记
          </div>
        </div>
      </section>

      <!-- 日历 + 详情两列 -->
      <section class="content">
        <div class="calendar-panel">
          <div class="cal-head">
            <el-button circle link @click="prevMonth">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <div class="cal-title">
              <span class="year">{{ curYear }}</span>
              <span class="sep">年</span>
              <span class="month">{{ curMonth }}月</span>
            </div>
            <el-button circle link @click="nextMonth">
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div class="cal-week">
            <div v-for="w in weekDays" :key="w" class="w">{{ w }}</div>
          </div>
          <div class="cal-grid">
            <div
              v-for="(cell, i) in calendarCells"
              :key="i"
              class="cell"
              :class="{
                muted: !cell.inMonth,
                today: cell.isToday,
                selected: cell.dateISO === selectedDate,
                'has-log': !!cell.summary
              }"
              @click="selectDate(cell)"
            >
              <div class="d">{{ cell.day }}</div>
              <div
                v-if="cell.summary"
                class="mood-dot"
                :style="{ background: cell.summary.moodColor }"
                :title="`${cell.summary.moodLabel} · 平均分 ${cell.summary.avgScore}`"
              ></div>
            </div>
          </div>

          <div class="legend">
            <span v-for="b in bucketOptions" :key="b.key" class="legend-item">
              <i class="dot" :style="{ background: b.color }"></i>
              {{ b.label }}
            </span>
          </div>
        </div>

        <div class="detail-panel">
          <div class="detail-head">
            <div>
              <div class="detail-title">{{ selectedDateText }}</div>
              <div class="detail-sub" v-if="selectedSummary">
                当日平均 <b :style="{ color: selectedSummary.moodColor }">{{ selectedSummary.avgScore }}</b> 分 ·
                主心情 <b :style="{ color: selectedSummary.moodColor }">{{ selectedSummary.moodLabel }}</b>
              </div>
              <div class="detail-sub gray" v-else>这一天还没有心情记录,点右上角「记录这天」吧</div>
            </div>
            <el-button type="primary" link @click="openWriteDialog(selectedDate)">
              <el-icon><Edit /></el-icon>
              <span>记录这天</span>
            </el-button>
          </div>

          <el-empty
            v-if="!selectedLogs || selectedLogs.length === 0"
            description="还没有记录,写一条记录让这天不孤单。"
            :image-size="100"
          />

          <div v-else class="log-list">
            <div
              v-for="log in selectedLogs"
              :key="log.id"
              class="log-item"
            >
              <div class="log-time">
                <el-icon><Clock /></el-icon>
                <span>{{ formatTime(log.createdAt) }}</span>
              </div>
              <div class="log-mood" :style="{ background: moodColorOf(log) }">
                {{ log.dominantEmotion || '—' }} · {{ scoreOf(log) }}
              </div>
              <div class="log-content" v-if="log.diaryContent">{{ log.diaryContent }}</div>
              <div v-if="log.emotionTriggers || log.sleepQuality || log.stressLevel" class="log-meta">
                <el-tag v-if="log.emotionTriggers" v-for="t in splitTags(log.emotionTriggers)" :key="t" size="small" effect="plain" class="m-tag">
                  {{ t }}
                </el-tag>
                <el-tag v-if="log.sleepQuality" type="info" size="small" effect="plain" class="m-tag">
                  睡眠 {{ qualityLabel(log.sleepQuality) }}({{ log.sleepQuality }})
                </el-tag>
                <el-tag v-if="log.stressLevel" type="warning" size="small" effect="plain" class="m-tag">
                  压力 {{ qualityLabel(log.stressLevel) }}({{ log.stressLevel }})
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 写心情弹窗 -->
    <el-dialog
      v-model="writeVisible"
      :title="`写心情 - ${form.diaryDateText}`"
      width="640px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="92px" label-position="left" v-if="writeVisible">
        <el-form-item label="日记日期">
          <el-date-picker
            v-model="form.diaryDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="心情分数" required>
          <div class="mood-levels">
            <div
              v-for="m in levelOptions"
              :key="m.score"
              class="mood-opt"
              :class="{ active: form.moodScore === m.score }"
              @click="pickLevel(m)"
            >
              <div class="mood-circle" :style="{ background: m.color }">
                <span>{{ m.emoji }}</span>
              </div>
              <div class="mood-l">{{ m.label }}</div>
              <div class="mood-s">分 {{ m.score }}</div>
            </div>
          </div>
          <el-slider
            class="mt-8"
            v-model="form.moodScore"
            :min="1"
            :max="10"
            :step="1"
            show-stops
            show-tooltip
          />
          <div class="score-hint" :style="{ color: scoreColor(form.moodScore * 10) }">
            当前 {{ form.moodScore }} 分(展示分 {{ form.moodScore * 10 }}) · {{ labelByScore(form.moodScore * 10) }}
          </div>
        </el-form-item>

        <el-form-item label="主要情绪">
          <el-input
            v-model="form.dominantEmotion"
            maxlength="50"
            show-word-limit
            placeholder="例:愉快 / 焦虑 / 平静... (不填会根据分数自动生成)"
          />
        </el-form-item>

        <el-form-item label="触发原因">
          <el-input
            v-model="form.emotionTriggers"
            maxlength="500"
            placeholder="逗号分隔,例:考试压力,和朋友吵架"
          />
        </el-form-item>

        <el-form-item label="日记正文">
          <el-input
            v-model="form.diaryContent"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="今天发生了什么?你有什么感受?"
          />
        </el-form-item>

        <el-form-item label="附加维度">
          <div class="extras">
            <div class="extra-item">
              <span class="extra-l">睡眠</span>
              <el-rate v-model="form.sleepQuality" :max="5" show-text allow-half :texts="['很差','差','一般','不错','很好']" text-color="#374151" />
            </div>
            <div class="extra-item">
              <span class="extra-l">压力</span>
              <el-rate v-model="form.stressLevel" :max="5" show-text allow-half :texts="['无','轻','一般','较重','很大']" text-color="#374151" />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="writeVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitLog">保存记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Edit,
  ArrowLeft,
  ArrowRight,
  Clock
} from '@element-plus/icons-vue'
import {
  getEmotionOptions,
  addEmotionLog,
  getEmotionMonthStats,
  getEmotionTodaySummary
} from '@/api/admin.js'

// ===== 状态 =====
const today = reactive({
  date: '',
  avgScore: null,
  topLabel: '',
  topColor: '#9ca3af',
  recordCount: 0,
  logs: []
})

const stats = reactive({
  recordCount: 0,
  avgScore: null,
  topLabel: '暂无记录',
  topColor: '#9ca3af',
  days: {},
  logsByDate: {}
})

const levelOptions = ref([])   // 10 档 1-10
const bucketOptions = ref([])  // 5 档图例
const curYear = ref(new Date().getFullYear())
const curMonth = ref(new Date().getMonth() + 1)
const selectedDate = ref(formatISO(new Date()))
const writeVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  diaryDate: '',
  moodScore: 6,
  dominantEmotion: '',
  emotionTriggers: '',
  diaryContent: '',
  sleepQuality: 3,
  stressLevel: 3,
  get diaryDateText() {
    return this.diaryDate || formatISO(new Date())
  }
})

const weekDays = ['一', '二', '三', '四', '五', '六', '日']
const todayISO = formatISO(new Date())

const monthText = computed(() => `${curYear.value}年${curMonth.value}月`)
const todayColor = computed(() => today.topColor || '#9ca3af')

const calendarCells = computed(() => {
  const y = curYear.value
  const m = curMonth.value - 1
  const firstDay = new Date(y, m, 1)
  const startOffset = (firstDay.getDay() + 6) % 7
  const daysInMonth = new Date(y, m + 1, 0).getDate()
  const totalCells = Math.ceil((startOffset + daysInMonth) / 7) * 7
  const cells = []
  for (let i = 0; i < totalCells; i++) {
    const d = new Date(y, m, i - startOffset + 1)
    const inMonth = d.getMonth() === m
    const dateISO = formatISO(d)
    const summary = (stats.days && stats.days[dateISO]) || null
    cells.push({ inMonth, day: d.getDate(), dateISO, date: d, isToday: dateISO === todayISO, summary })
  }
  return cells
})

const selectedDateText = computed(() => {
  const d = selectedDate.value
  return d === todayISO ? `${d} · 今天` : `${d} · ${diffDayText(d)}`
})

const selectedLogs = computed(() => {
  if (!stats.logsByDate) return []
  return stats.logsByDate[selectedDate.value] || []
})
const selectedSummary = computed(() => {
  if (!stats.days) return null
  return stats.days[selectedDate.value] || null
})

const streakDays = computed(() => {
  if (!stats.logsByDate) return 0
  let n = 0
  const cursor = new Date()
  while (stats.logsByDate[formatISO(cursor)] && stats.logsByDate[formatISO(cursor)].length) {
    n++
    cursor.setDate(cursor.getDate() - 1)
  }
  return n
})

// ===== 工具 =====
function pad(n) { return String(n).padStart(2, '0') }
function formatISO(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
function formatTime(t) {
  if (!t) return ''
  const d = typeof t === 'string' || typeof t === 'number' ? new Date(t) : t
  if (isNaN(d.getTime())) return ''
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function splitTags(t) {
  if (!t) return []
  return t.split(/[,，]/).map(s => s.trim()).filter(Boolean)
}
function scoreColor(s) {
  const v = Number(s)
  if (isNaN(v)) return '#9ca3af'
  if (v >= 75) return '#22c55e'
  if (v >= 50) return '#eab308'
  if (v >= 30) return '#f97316'
  return '#ef4444'
}
function labelByScore(s) {
  const v = Number(s)
  if (isNaN(v)) return '未记录'
  if (v >= 85) return '开心'
  if (v >= 65) return '不错'
  if (v >= 45) return '一般'
  if (v >= 25) return '低落'
  return '很差'
}
function chipStyle(color) {
  if (!color) return { background: '#f3f4f6', color: '#111827' }
  return { background: color + '22', color: color, border: '1px solid ' + color + '55' }
}
function diffDayText(dateStr) {
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  const d = new Date(dateStr + 'T00:00:00')
  const diff = Math.round((d - now) / 86400000)
  if (diff === 0) return '今天'
  if (diff === -1) return '昨天'
  if (diff === 1) return '明天'
  if (diff < 0) return `${-diff} 天前`
  return `${diff} 天后`
}

/** 单条日记的颜色:优先主情绪匹配后端 colorForLabel 逻辑(但后端给的 avg/聚合颜色,这里再按 1-10分映射更直观) */
function moodColorOf(log) {
  const s = (log.moodScore == null ? 6 : log.moodScore) * 10
  return scoreColor(s)
}
function scoreOf(log) {
  return (log.moodScore == null ? 6 : log.moodScore) * 10
}
function qualityLabel(v) {
  const n = Number(v)
  const map = ['—', '很差', '差', '一般', '不错', '很好']
  return map[n] || map[3]
}

// ===== 月份切换 =====
function prevMonth() {
  let y = curYear.value, m = curMonth.value
  m--
  if (m < 1) { m = 12; y-- }
  curYear.value = y
  curMonth.value = m
  loadMonthStats()
}
function nextMonth() {
  let y = curYear.value, m = curMonth.value
  m++
  if (m > 12) { m = 1; y++ }
  curYear.value = y
  curMonth.value = m
  loadMonthStats()
}

// ===== 日历选择 =====
function selectDate(cell) {
  if (!cell.inMonth) {
    curYear.value = cell.date.getFullYear()
    curMonth.value = cell.date.getMonth() + 1
    loadMonthStats().finally(() => { selectedDate.value = cell.dateISO })
  } else {
    selectedDate.value = cell.dateISO
  }
}

// ===== 弹窗相关 =====
function openWriteDialog(dateStr) {
  const d = dateStr || selectedDate.value || formatISO(new Date())
  form.diaryDate = d
  // 默认 6 分(一般档)
  form.moodScore = 6
  form.dominantEmotion = ''
  form.emotionTriggers = ''
  form.diaryContent = ''
  form.sleepQuality = 3
  form.stressLevel = 3
  writeVisible.value = true
}
function pickLevel(m) {
  form.moodScore = m.score
  if (!form.dominantEmotion) {
    // 用户没填主情绪时,自动给个兜底(保存时后端也会兜底)
    form.dominantEmotion = m.label
  }
}

async function submitLog() {
  if (form.moodScore == null || form.moodScore < 1 || form.moodScore > 10) {
    ElMessage.warning('请选择心情分数(1-10)')
    return
  }
  submitting.value = true
  try {
    const payload = {
      diaryDate: form.diaryDate || undefined,
      moodScore: Number(form.moodScore),
      dominantEmotion: form.dominantEmotion || undefined,
      emotionTriggers: form.emotionTriggers || undefined,
      diaryContent: form.diaryContent || undefined,
      sleepQuality: form.sleepQuality || undefined,
      stressLevel: form.stressLevel || undefined
    }
    await addEmotionLog(payload)
    ElMessage.success('已记录这份心情 💛')
    writeVisible.value = false
    await Promise.all([loadMonthStats(), loadTodaySummary()])
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

// ===== 接口加载 =====
async function loadOptions() {
  const d = await getEmotionOptions()
  levelOptions.value = d.levels || []
  bucketOptions.value = d.buckets || []
}
async function loadTodaySummary() {
  try {
    const d = await getEmotionTodaySummary()
    today.date = d.date
    today.avgScore = d.avgScore
    today.topLabel = d.topLabel
    today.topColor = d.topColor || '#9ca3af'
    today.recordCount = d.recordCount
    today.logs = d.logs || []
  } catch (e) {
    console.warn(e)
  }
}
async function loadMonthStats() {
  const ym = `${curYear.value}-${pad(curMonth.value)}`
  try {
    const d = await getEmotionMonthStats(ym)
    stats.recordCount = d.recordCount || 0
    stats.avgScore = d.avgScore
    stats.topLabel = d.topLabel
    stats.topColor = d.topColor || '#9ca3af'
    stats.days = d.days || {}
    stats.logsByDate = d.logsByDate || {}
  } catch (e) {
    console.warn(e)
  }
}

onMounted(async () => {
  await loadOptions()
  selectedDate.value = formatISO(new Date())
  await Promise.all([loadTodaySummary(), loadMonthStats()])
})
</script>

<style lang="scss" scoped>
.emo-page {
  background: #f6f5f0;
  min-height: calc(100vh - 64px);
}
.emo-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 28px 24px 48px;
}

/* ===== Hero ===== */
.hero {
  background: linear-gradient(135deg, #f9a8d4 0%, #fbbf77 60%, #fcd34d 100%);
  border-radius: 20px;
  padding: 32px;
  color: #5b3a14;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;

  .hero-badge {
    display: inline-block;
    background: rgba(255,255,255,0.6);
    padding: 4px 12px;
    border-radius: 999px;
    font-size: 12px;
    letter-spacing: 0.5px;
    margin-bottom: 12px;
  }
  .hero-title {
    margin: 0 0 8px;
    font-size: 32px;
    font-weight: 800;
  }
  .hero-desc {
    margin: 0 0 18px;
    line-height: 1.7;
    max-width: 520px;
    color: #7c5a2a;
  }
  .hero-btn {
    background: #ffffff;
    color: #b45309;
    border: none;
    border-radius: 12px;
    font-weight: 700;
    padding: 12px 22px;
    box-shadow: 0 6px 14px rgba(180,83,9,0.12);
    &:hover { background: #fff7ea; color: #92400e; }
  }
  .today-card {
    width: 220px;
    border-radius: 18px;
    padding: 18px 20px;
    background: rgba(255,255,255,0.72);
    backdrop-filter: blur(6px);
    border: 1px solid rgba(255,255,255,0.5);
    box-shadow: 0 8px 22px rgba(91,58,20,0.08);

    .today-head { font-size: 13px; color: #7c5a2a; margin-bottom: 10px; }
    .today-body {
      display: flex; align-items: baseline; gap: 12px; margin-bottom: 14px;
      .today-score {
        font-size: 46px; font-weight: 800; color: var(--today-col, #b45309); line-height: 1;
        &.empty { color: #9ca3af; }
      }
      .today-label {
        padding: 4px 12px; background: var(--today-col, #e5e7eb); color: #fff;
        border-radius: 999px; font-weight: 700; font-size: 13px;
        &.empty { background: #e5e7eb; color: #6b7280; }
      }
    }
    .today-foot { font-size: 12px; color: #7c5a2a; }
  }
}

/* ===== Stats ===== */
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;

  .stat-card {
    background: #fff; border-radius: 16px; padding: 20px 22px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    .stat-label { font-size: 13px; color: #6b7280; margin-bottom: 10px; }
    .stat-val { font-weight: 800; color: #1f2937;
      &.big { font-size: 32px; line-height: 1; }
    }
    .stat-sub { margin-top: 12px; font-size: 12px; color: #374151; &.gray { color: #9ca3af; } }
    .mood-chip { display: inline-flex; align-items: center; padding: 4px 12px; border-radius: 999px; font-weight: 700; font-size: 14px; }
  }
}

/* ===== Content ===== */
.content {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 20px;

  .calendar-panel, .detail-panel {
    background: #fff; border-radius: 16px; padding: 22px 22px 18px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  }
  .cal-head {
    display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;
    .cal-title {
      font-weight: 800; color: #1f2937; font-size: 18px;
      .year { font-size: 16px; color: #6b7280; font-weight: 600; }
      .sep { margin: 0 4px; color: #9ca3af; font-weight: 600; }
      .month { font-size: 20px; }
    }
  }
  .cal-week { display: grid; grid-template-columns: repeat(7, 1fr); margin-bottom: 6px;
    .w { text-align: center; font-size: 12px; color: #9ca3af; padding: 6px 0; }
  }
  .cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px;
    .cell {
      height: 62px; border-radius: 10px; padding: 6px 8px; position: relative;
      background: #fafaf5; border: 1px solid transparent; cursor: pointer;
      display: flex; flex-direction: column; align-items: flex-start; transition: all 0.18s ease;
      .d { font-size: 13px; color: #111827; }
      .mood-dot { margin-top: auto; width: 10px; height: 10px; border-radius: 50%; align-self: flex-end; }
      &.muted { background: transparent; .d { color: #d1d5db; } }
      &.today { border: 1.5px solid #f59e0b; .d { font-weight: 800; color: #b45309; } }
      &.selected { background: linear-gradient(135deg, #fff3c4, #ffd5a3); border: 1.5px solid #f59e0b; }
      &:hover:not(.selected):not(.muted) { background: #fdf3dd; }
    }
  }
  .legend { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 16px; font-size: 12px; color: #6b7280;
    .legend-item { display: inline-flex; align-items: center; gap: 6px;
      .dot { width: 10px; height: 10px; border-radius: 50%; }
    }
  }

  .detail-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px;
    .detail-title { font-weight: 800; font-size: 18px; color: #111827; }
    .detail-sub { font-size: 13px; color: #374151; margin-top: 4px; &.gray { color: #9ca3af; } }
  }

  .log-list { display: flex; flex-direction: column; gap: 12px; max-height: 520px; overflow-y: auto; padding-right: 4px; }
  .log-item { background: #faf9f5; border-radius: 12px; padding: 14px 16px; border: 1px solid #f0ecd7;
    .log-time { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #9ca3af; margin-bottom: 8px; }
    .log-mood { display: inline-block; padding: 3px 10px; color: #fff; border-radius: 999px; font-size: 12px; font-weight: 700; margin-bottom: 10px; }
    .log-content { font-size: 14px; line-height: 1.7; color: #374151; white-space: pre-wrap; word-break: break-word; }
    .log-meta { margin-top: 10px; display: flex; flex-wrap: wrap; gap: 6px; .m-tag { margin-right: 0; } }
  }
}

/* ===== 弹窗 ===== */
.mood-levels {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 8px;
  width: 100%;
  .mood-opt {
    display: flex; flex-direction: column; align-items: center; padding: 10px 4px;
    border-radius: 12px; cursor: pointer; transition: all 0.18s ease;
    border: 1.5px solid #f3f4f6;
    .mood-circle {
      width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
      color: #fff; font-size: 18px; margin-bottom: 4px;
    }
    .mood-l { font-size: 12px; color: #6b7280; line-height: 1.2; }
    .mood-s { font-size: 11px; color: #9ca3af; margin-top: 2px; }
    &:hover { background: #fafaf5; }
    &.active {
      border-color: #f59e0b; background: #fff7ea;
      .mood-l { color: #b45309; font-weight: 700; }
      .mood-s { color: #b45309; }
    }
  }
}
.mt-8 { margin-top: 12px; }
.score-hint { margin-top: 8px; font-size: 12px; font-weight: 600; }

.extras { display: flex; flex-direction: column; gap: 14px; width: 100%;
  .extra-item {
    display: flex; align-items: center; gap: 12px;
    .extra-l { width: 42px; font-size: 13px; color: #6b7280; flex-shrink: 0; }
    .el-rate { flex: 1; }
  }
}

@media (max-width: 980px) {
  .hero { flex-direction: column; align-items: flex-start; }
  .stats { grid-template-columns: 1fr; }
  .content { grid-template-columns: 1fr; }
  .mood-levels { grid-template-columns: repeat(5, 1fr); }
}
</style>
