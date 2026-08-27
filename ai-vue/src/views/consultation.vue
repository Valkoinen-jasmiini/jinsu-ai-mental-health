<template>
  <div class="consultation">
    <div class="c-inner">
      <!-- 左栏 -->
      <aside class="side">
        <!-- AI助手卡片 -->
        <div class="card ai-card" @click="createNewSession">
          <div class="ai-icon">
            <img src="@/assets/images/机器人.png" alt="AI助手" />
          </div>
          <div class="ai-name">瑾肃AI助手</div>
          <div class="ai-status">
            <span class="dot"></span>
            在线服务中
          </div>
        </div>

        <!-- 情绪花园 -->
        <div class="card garden-card">
          <div class="card-title">情绪花园</div>
          <div class="garden-score">
            <div class="score-circle" :style="scoreStyle">
              <span class="score-label">{{ mood.label }}</span>
              <span class="score-val">{{ mood.score }}</span>
            </div>
            <div class="garden-meta">
              <div class="meta-row">
                <span class="meta-label">今天感觉</span>
                <span class="meta-value strong">{{ mood.feeling }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">状态</span>
                <span class="meta-value">
                  <span
                    v-for="(dot, i) in 3"
                    :key="i"
                    class="status-dot"
                    :class="{ active: i < mood.level }"
                  ></span>
                  <span class="status-text">{{ mood.levelText }}</span>
                </span>
              </div>
            </div>
          </div>
          <div class="garden-advice">
            <div class="advice-title">
              <el-icon><Medal /></el-icon>给你的小建议
            </div>
            <div class="advice-text">{{ mood.advice }}</div>
          </div>
        </div>

        <!-- 会话列表 -->
        <div class="card session-card">
          <div class="card-title flex-between">
            会话列表
            <el-icon class="add-btn" title="新建会话" @click.stop="createNewSession">
              <Plus />
            </el-icon>
          </div>
          <div class="session-list">
            <div
              v-for="s in sessions"
              :key="s.id"
              class="session-item"
              :class="{ active: activeDbId === s.id }"
              @click="selectSession(s)"
            >
              <div class="si-main">
                <div class="si-title">{{ s.sessionTitle || '未命名会话' }}</div>
                <div class="si-date">{{ formatFullTime(s.startedAt) }}</div>
                <div class="si-preview">{{ s._preview || '点击查看会话详情' }}</div>
                <div class="si-meta">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>{{ s._messageCount ?? 0 }}</span>
                  <span class="sep">·</span>
                  <el-icon><Timer /></el-icon>
                  <span>{{ s._duration || '刚刚' }}</span>
                </div>
              </div>
              <div class="si-actions">
                <el-icon class="action-btn rename-btn" title="重命名" @click.stop="handleRenameSession(s)">
                  <Edit />
                </el-icon>
                <el-icon class="action-btn delete-btn" title="删除" @click.stop="removeSession(s)">
                  <Delete />
                </el-icon>
              </div>
            </div>
            <el-empty
              v-if="sessions.length === 0"
              description="还没有会话，点击上方 AI助手 开启对话"
              :image-size="80"
            />
          </div>
        </div>
      </aside>

      <!-- 右栏 聊天 -->
      <section class="chat">
        <!-- 顶部橙色条 -->
        <div class="chat-header">
          <div class="chat-head-inner">
            <div class="left-info">
              <div class="heart">
                <el-icon :size="20"><Star /></el-icon>
              </div>
              <div class="head-text">
                <div class="head-title">{{ activeSession ? activeSession.sessionTitle : '瑾肃AI助手' }}</div>
                <div class="head-sub">您的贴心AI心理健康助手</div>
              </div>
            </div>
            <el-button circle class="plus-btn" @click="createNewSession">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- 聊天区 -->
        <div class="chat-body" ref="bodyRef">
          <template v-if="messages.length > 0 || streaming.content">
            <div
              v-for="(m, idx) in messages"
              :key="idx"
              class="msg-row"
              :class="m.senderType === 1 ? 'me' : 'ai'"
            >
              <template v-if="m.senderType === 2">
                <div class="avatar ai-avatar">
                  <img src="@/assets/images/机器人.png" alt="AI" />
                </div>
                <div class="bubble ai-bubble">
                  <div class="b-content">{{ m.content }}</div>
                  <div class="b-time">{{ formatTime(m.createdAt) }}</div>
                </div>
              </template>
              <template v-else>
                <div class="bubble me-bubble">
                  <div class="b-content">{{ m.content }}</div>
                  <div class="b-time">{{ formatTime(m.createdAt) }}</div>
                </div>
                <div class="avatar me-avatar">
                  <img :src="userAvatar" alt="我" />
                </div>
              </template>
            </div>

            <!-- 流式气泡 -->
            <div
              v-if="streaming.content !== '' || streamingPending"
              class="msg-row ai"
            >
              <div class="avatar ai-avatar">
                <img src="@/assets/images/机器人.png" alt="AI" />
              </div>
              <div class="bubble ai-bubble">
                <div class="b-content">
                  {{ streaming.content }}
                  <span v-if="streamingPending" class="cursor">▍</span>
                </div>
              </div>
            </div>
          </template>

          <!-- 空状态:首次进入 -->
          <div v-else class="empty-state">
            <img src="@/assets/images/机器人.png" alt="AI助手" class="empty-img" />
            <h3 class="empty-title">开始一次温暖的对话吧</h3>
            <p class="empty-desc">在下方输入框告诉AI助手你的感受，或点击左侧「瑾肃AI助手」开启会话。</p>
            <div class="quick-tags">
              <el-tag
                v-for="t in quickTags"
                :key="t"
                class="q-tag"
                size="large"
                effect="plain"
                @click="quickSend(t)"
              >
                {{ t }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="chat-input">
          <div class="input-wrap">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="3"
              resize="none"
              maxlength="500"
              show-word-limit
              placeholder="请输入您想要分享的内容..."
              @keydown="onKeyDown"
            />
          </div>
          <div class="send-bar">
            <span class="tip">按 Enter 发送，Shift+Enter 换行</span>
            <el-button
              class="send-btn"
              type="primary"
              shape="round"
              :loading="sending"
              :disabled="!inputText.trim() || sending"
              @click="sendMessage"
            >
              <el-icon><Promotion /></el-icon>
              <span>发送</span>
            </el-button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, onBeforeUnmount, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Star,
  Promotion,
  Delete,
  Edit,
  Medal,
  ChatDotRound,
  Timer
} from '@element-plus/icons-vue'
import {
  listSessions,
  listMessages,
  startSession,
  streamChat,
  getSessionMood,
  deleteSession,
  renameSession
} from '@/api/admin.js'

// ============ 状态 ============
const sessions = ref([])
const activeSession = ref(null)
const activeDbId = ref(null)
const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const streaming = reactive({ content: '' })
const streamingPending = ref(false)
const streamHandle = shallowRef(null)
const bodyRef = ref(null)

const MOOD_CACHE_KEY_PREFIX = 'mental_session_mood_'

// 情绪花园(按会话维度独立记分)
const mood = reactive({
  label: '中性',
  score: 50,
  feeling: '很不错',
  level: 2,
  levelText: '正常',
  advice: '选择一个会话开始对话,这里将实时展示该会话的情绪走向。'
})
function sessionMoodKey(dbId) {
  return `${MOOD_CACHE_KEY_PREFIX}${dbId}`
}
function applyMood(d, { persistTo, persist = false } = {}) {
  if (!d) return
  mood.label = d.label ?? '中性'
  mood.score = Math.max(0, Math.min(100, Number(d.score) || 50))
  mood.feeling = d.feeling ?? '正常'
  mood.level = Math.max(1, Math.min(3, Number(d.level) || 2))
  mood.levelText = d.levelText ?? '正常'
  mood.advice = d.advice ?? ''
  if (persist && persistTo) {
    try {
      const payload = {
        ts: Date.now(),
        data: {
          label: mood.label,
          score: mood.score,
          feeling: mood.feeling,
          level: mood.level,
          levelText: mood.levelText,
          advice: mood.advice
        }
      }
      localStorage.setItem(sessionMoodKey(persistTo), JSON.stringify(payload))
    } catch (_) {}
  }
}
function restoreMoodCacheBy(dbId) {
  try {
    const raw = localStorage.getItem(sessionMoodKey(dbId))
    if (!raw) return false
    const parsed = JSON.parse(raw)
    // 仅今天内有效
    const today = new Date().toDateString()
    const cachedDay = new Date(parsed.ts || 0).toDateString()
    if (today === cachedDay && parsed.data) {
      applyMood(parsed.data, { persist: false })
      return true
    }
  } catch (_) {}
  return false
}
async function refreshMoodBy(dbId, { persist = true } = {}) {
  if (!dbId) return
  try {
    const data = await getSessionMood(dbId)
    applyMood(data, { persistTo: dbId, persist })
  } catch (e) {
    console.warn('刷新会话情绪失败', e)
  }
}
function resetMoodToNeutral() {
  mood.label = '中性'
  mood.score = 50
  mood.feeling = '很不错'
  mood.level = 2
  mood.levelText = '正常'
  mood.advice = '新建会话的情绪基线为 50,发送消息后会根据内容实时更新分数、标签和建议。'
}
const scoreStyle = computed(() => {
  const color =
    mood.score >= 70 ? '#22c55e' : mood.score >= 40 ? '#f59e0b' : '#ef4444'
  return {
    background: `conic-gradient(${color} ${(mood.score / 100) * 360}deg, #fde7cf 0deg)`
  }
})

const quickTags = [
  '最近我有点焦虑',
  '睡眠质量不太好',
  '心里难受，想找人聊聊',
  '工作压力很大'
]

// 头像:用户头像取本地存储里的 avatar,没有的话使用默认
const userAvatar = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return info && info.avatar ? info.avatar : 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
  } catch {
    return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
  }
})

// ============ 会话列表/消息 ============
function parseMoodJson(json) {
  if (!json) return null
  try {
    const d = JSON.parse(json)
    if (!d || typeof d !== 'object') return null
    return {
      label: d.label,
      score: d.score,
      feeling: d.feeling,
      level: d.level,
      levelText: d.levelText,
      advice: d.advice
    }
  } catch (e) {
    return null
  }
}
async function loadSessions() {
  try {
    const data = await listSessions()
    sessions.value = Array.isArray(data) ? data : []
    // 附加一些显示字段 + 预解析每条会话的情绪花园(不用再打额外接口,0ms显示)
    sessions.value.forEach((s) => {
      s._preview = s.sessionTitle || '瑾肃AI助手为您服务'
      s._messageCount = 0
      s._duration = '刚刚'
      s._mood = parseMoodJson(s.lastEmotionAnalysis)
    })
  } catch (e) {
    console.error(e)
  }
}

function selectSession(s) {
  activeSession.value = s
  activeDbId.value = s.id
  // 【加速情绪花园】:先直接用 listSessions 里已经带回来的 lastEmotionAnalysis,0ms 渲染,不等消息和接口
  if (s._mood) {
    applyMood(s._mood, { persist: false })
  } else {
    resetMoodToNeutral()
  }
  // 并行异步:加载消息列表 + 兜底刷新情绪(避免lastEmotionAnalysis为空场景)
  loadMessages(s.id)
  refreshMoodBy(s.id, { persist: true })
}

async function loadMessages(dbId) {
  try {
    const data = await listMessages(dbId)
    messages.value = Array.isArray(data) ? data : []
    // 反向同步一下会话显示辅助
    const s = sessions.value.find((x) => x.id === dbId)
    if (s) {
      s._messageCount = messages.value.length
      // 取最后一条AI消息当预览
      const lastAi = [...messages.value].reverse().find((m) => m.senderType === 2)
      if (lastAi) {
        s._preview = lastAi.content
      }
      if (messages.value[0]?.createdAt) {
        s._duration = agoText(messages.value[0].createdAt)
      }
    }
    nextTick(scrollBottom)
  } catch (e) {
    console.error(e)
  }
}

// ============ 新建会话 ============
async function createNewSession() {
  const text = inputText.value.trim()
  if (!text) {
    try {
      const { value } = await ElMessageBox.prompt(
        '请输入想要告诉AI助手的第一句话',
        '新建会话',
        {
          confirmButtonText: '创建',
          cancelButtonText: '取消',
          inputPlaceholder: '例如：最近觉得有点焦虑，睡不好...',
          inputValidator: (v) => (v && v.trim() ? true : '请输入内容')
        }
      )
      await doCreateSession(value.trim())
    } catch (_) { /* cancel */ }
    return
  }
  await doCreateSession(text)
}

async function doCreateSession(initialMessage) {
  sending.value = true
  try {
    const data = await startSession({
      sessionTitle: '',
      initialMessage
    })
    const dbId = extractDbId(data.sessionId)
    await loadSessions()
    const created = sessions.value.find((s) => s.id === dbId)
    activeSession.value = created || { id: dbId, sessionTitle: '瑾肃AI助手' }
    activeDbId.value = dbId
    messages.value = [
      {
        id: 0,
        sessionId: dbId,
        senderType: 1,
        content: initialMessage,
        createdAt: new Date().toISOString()
      }
    ]
    inputText.value = ''
    await startStreaming(data.sessionId, initialMessage)
    await loadMessages(dbId)
    streaming.content = ''
    await refreshMoodBy(dbId, { persist: true })
  } catch (e) {
    ElMessage.error(e.message || '创建会话失败')
  } finally {
    sending.value = false
    streamingPending.value = false
  }
}

function extractDbId(sessionId) {
  if (!sessionId) return null
  if (sessionId.startsWith('session_')) {
    const n = Number(sessionId.slice('session_'.length))
    return Number.isNaN(n) ? null : n
  }
  return null
}

function quickSend(tag) {
  inputText.value = tag
  nextTick(() => sendMessage())
}

// ============ 发送消息 ============
function onKeyDown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return
  if (sending.value) return

  if (!activeDbId.value) {
    await doCreateSession(text)
    return
  }

  sending.value = true
  messages.value.push({
    id: Date.now(),
    sessionId: activeDbId.value,
    senderType: 1,
    content: text,
    createdAt: new Date().toISOString()
  })
  inputText.value = ''
  nextTick(scrollBottom)

  try {
    const sessionId = 'session_' + activeDbId.value
    await startStreaming(sessionId, text)
    await loadMessages(activeDbId.value)
    streaming.content = ''
    await refreshMoodBy(activeDbId.value, { persist: true })
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    sending.value = false
    streamingPending.value = false
  }
}

function startStreaming(sessionId, userMessage) {
  return new Promise((resolve, reject) => {
    streaming.content = ''
    streamingPending.value = true
    nextTick(scrollBottom)
    streamHandle.value = streamChat(sessionId, userMessage, {
      onMessage: (chunk) => {
        streaming.content += chunk
        nextTick(scrollBottom)
      },
      onDone: () => {
        streamingPending.value = false
        if (streaming.content) {
          messages.value.push({
            id: Date.now() + 1,
            sessionId: activeDbId.value,
            senderType: 2,
            content: streaming.content,
            createdAt: new Date().toISOString()
          })
          streaming.content = ''
          nextTick(scrollBottom)
        }
        resolve()
      },
      onError: (err) => {
        streamingPending.value = false
        // unmount 时主动 abort 不算错误
        if (err && (err.name === 'AbortError' || err.code === 20 || err.message?.includes('abort'))) {
          return resolve()
        }
        ElMessage.error(err.message || 'AI回复失败')
        reject(err)
      }
    })
  })
}

async function removeSession(s) {
  try {
    await ElMessageBox.confirm(
      `删除会话「${s.sessionTitle || '未命名会话'}」？历史消息将不可恢复。`,
      '提示',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (_) {
    return // 用户点取消
  }
  try {
    await deleteSession(s.id)
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
    return
  }
  // 删成功,前端立刻移除(不要等 reload 接口)
  sessions.value = sessions.value.filter((x) => x.id !== s.id)
  try { localStorage.removeItem(sessionMoodKey(s.id)) } catch (_) {}
  if (activeDbId.value === s.id) {
    activeDbId.value = null
    activeSession.value = null
    messages.value = []
    // 删掉当前激活的 → 切第一个剩下的;都没了就重置
    if (sessions.value.length > 0) {
      selectSession(sessions.value[0])
    } else {
      resetMoodToNeutral()
    }
  }
  ElMessage.success('已删除')
}

async function handleRenameSession(s) {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入新的会话标题',
      '重命名会话',
      {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        inputValue: s.sessionTitle || '',
        inputValidator: (v) => (v && v.trim() ? true : '标题不能为空')
      }
    )
    const newTitle = value.trim()
    await renameSession(s.id, newTitle)
    s.sessionTitle = newTitle
    // 更新当前选中的会话标题
    if (activeDbId.value === s.id) {
      activeSession.value = { ...activeSession.value, sessionTitle: newTitle }
    }
    ElMessage.success('重命名成功')
  } catch (_) { /* cancel */ }
}

// ============ 工具方法 ============
function scrollBottom() {
  if (bodyRef.value) {
    bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  }
}

function pad(n) {
  return String(n).padStart(2, '0')
}
function formatTime(t) {
  if (!t) return ''
  const d = typeof t === 'string' || typeof t === 'number' ? new Date(t) : t
  if (isNaN(d.getTime())) return ''
  const now = new Date()
  const same =
    now.getFullYear() === d.getFullYear() &&
    now.getMonth() === d.getMonth() &&
    now.getDate() === d.getDate()
  if (same) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function formatFullTime(t) {
  if (!t) return ''
  const d = typeof t === 'string' || typeof t === 'number' ? new Date(t) : t
  if (isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function agoText(t) {
  const d = typeof t === 'string' || typeof t === 'number' ? new Date(t) : t
  if (isNaN(d.getTime())) return '刚刚'
  const diff = Math.max(0, Date.now() - d.getTime())
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min} 分钟`
  const hr = Math.floor(min / 60)
  if (hr < 24) return `${hr} 小时`
  return `${Math.floor(hr / 24)} 天`
}

onMounted(async () => {
  await loadSessions()
  if (sessions.value.length > 0) {
    selectSession(sessions.value[0]) // selectSession 内部已经按会话维度刷新情绪
  } else {
    resetMoodToNeutral()
  }
})

onBeforeUnmount(() => {
  streamHandle.value && streamHandle.value.close()
})
</script>

<style lang="scss" scoped>
.consultation {
  width: 100%;
  background: #f5f6f8;

  .c-inner {
    max-width: 1440px;
    margin: 0 auto;
    padding: 24px;
    display: grid;
    grid-template-columns: 300px 1fr;
    gap: 20px;
    min-height: calc(100vh - 64px - 48px);
  }
}

.side {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .card {
    background: #fff;
    border-radius: 16px;
    padding: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  }

  .flex-between {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .ai-card {
    text-align: center;
    cursor: pointer;
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 16px rgba(0, 0, 0, 0.06);
    }

    .ai-icon {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      margin: 0 auto 10px;
      background: linear-gradient(135deg, #fff0e0, #ffdcbe);
      display: flex;
      align-items: center;
      justify-content: center;

      img {
        width: 44px;
        height: 44px;
      }
    }
    .ai-name {
      font-weight: 700;
      color: #1f2937;
      font-size: 16px;
    }
    .ai-status {
      margin-top: 4px;
      font-size: 12px;
      color: #22c55e;
      display: inline-flex;
      align-items: center;
      gap: 4px;

      .dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #22c55e;
      }
    }
  }

  .garden-card {
    .card-title {
      font-weight: 700;
      color: #7c5a2a;
      font-size: 14px;
      margin-bottom: 14px;
    }

    .garden-score {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 16px;

      .score-circle {
        width: 86px;
        height: 86px;
        border-radius: 50%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        position: relative;
        flex-shrink: 0;

        &::before {
          content: '';
          position: absolute;
          inset: 8px;
          border-radius: 50%;
          background: #fff7ea;
        }

        .score-label,
        .score-val {
          position: relative;
          z-index: 1;
        }
        .score-label {
          font-size: 12px;
          color: #92400e;
        }
        .score-val {
          font-size: 22px;
          font-weight: 800;
          color: #b45309;
        }
      }

      .garden-meta {
        flex: 1;

        .meta-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 13px;
          margin-bottom: 6px;

          .meta-label {
            color: #8b6f47;
          }
          .meta-value {
            color: #1f2937;
            &.strong {
              color: #16a34a;
              font-weight: 600;
            }
            display: flex;
            align-items: center;
            gap: 4px;
          }
          .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #e5e7eb;
            &.active {
              background: #22c55e;
            }
          }
          .status-text {
            margin-left: 4px;
          }
        }
      }
    }

    .garden-advice {
      background: #fff7ea;
      border-radius: 10px;
      padding: 10px 12px;

      .advice-title {
        font-size: 13px;
        color: #b45309;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 4px;
        margin-bottom: 4px;
      }
      .advice-text {
        font-size: 12px;
        color: #92400e;
        line-height: 1.5;
      }
    }
  }

  .session-card {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;

    .card-title {
      font-weight: 700;
      color: #1f2937;
      font-size: 14px;
      margin-bottom: 12px;

      .add-btn {
        cursor: pointer;
        color: #9ca3af;
        font-size: 16px;
        padding: 4px;
        border-radius: 6px;
        &:hover {
          background: #f3f4f6;
          color: #111827;
        }
      }
    }

    .session-list {
      overflow-y: auto;
      max-height: 460px;
      padding-right: 4px;

      .session-item {
        padding: 12px;
        border-radius: 12px;
        border: 1px solid #f1f2f4;
        margin-bottom: 10px;
        cursor: pointer;
        display: flex;
        gap: 8px;
        transition: all 0.2s;

        &:hover {
          background: #f9fafb;
        }
        &.active {
          border-color: #ffd5a3;
          background: #fff7ea;
        }

        .si-main {
          flex: 1;
          min-width: 0;
        }
        .si-title {
          font-weight: 600;
          font-size: 13px;
          color: #1f2937;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        .si-date {
          font-size: 11px;
          color: #9ca3af;
          margin: 3px 0 6px;
        }
        .si-preview {
          font-size: 12px;
          color: #6b7280;
          line-height: 1.5;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .si-meta {
          margin-top: 8px;
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 11px;
          color: #9ca3af;

          .sep {
            margin: 0 2px;
          }
        }
        .si-actions {
          display: flex;
          flex-direction: column;
          gap: 6px;
          align-self: flex-start;
        }
        .action-btn {
          color: #d1d5db;
          cursor: pointer;
          padding: 2px;
          border-radius: 4px;
          transition: all 0.15s;
          &:hover {
            background: #f3f4f6;
          }
        }
        .rename-btn {
          &:hover {
            color: #3b82f6;
          }
        }
        .delete-btn {
          &:hover {
            color: #ef4444;
          }
        }
      }
    }
  }
}

/* ============ 右栏 聊天 ============ */
.chat {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  min-height: 700px;

  .chat-header {
    background: linear-gradient(90deg, #f49d47 0%, #f6b46a 100%);
    color: #fff;

    .chat-head-inner {
      padding: 16px 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .left-info {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .heart {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.22);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .head-title {
      font-weight: 700;
      font-size: 16px;
    }
    .head-sub {
      font-size: 12px;
      opacity: 0.85;
      margin-top: 2px;
    }

    .plus-btn {
      background: rgba(255, 255, 255, 0.22);
      border: none;
      color: #fff;
      &:hover {
        background: rgba(255, 255, 255, 0.32);
        color: #fff;
      }
    }
  }

  .chat-body {
    flex: 1;
    padding: 24px 28px 8px;
    overflow-y: auto;
    background: #fdfbf7;

    .empty-state {
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;

      .empty-img {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        background: #fff2e0;
        padding: 16px;
        box-sizing: border-box;
        margin-bottom: 16px;
      }
      .empty-title {
        font-size: 18px;
        font-weight: 700;
        color: #1f2937;
        margin: 0 0 6px;
      }
      .empty-desc {
        color: #6b7280;
        margin: 0 0 18px;
      }
      .quick-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        justify-content: center;
        max-width: 560px;

        .q-tag {
          cursor: pointer;
          padding: 6px 14px;
          border-radius: 999px;
          font-size: 13px;
        }
      }
    }

    .msg-row {
      display: flex;
      align-items: flex-start;
      margin-bottom: 20px;
      gap: 10px;

      &.me {
        flex-direction: row;
        justify-content: flex-end;
      }

      .avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        overflow: hidden;
        flex-shrink: 0;
        background: #fef3c7;
        display: flex;
        align-items: center;
        justify-content: center;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }
      .me-avatar {
        background: #dbeafe;
      }

      .bubble {
        max-width: 64%;
        padding: 10px 14px;
        border-radius: 14px;
        font-size: 14px;
        line-height: 1.7;
        white-space: pre-wrap;
        word-break: break-word;

        .b-content {
          color: #1f2937;
        }
        .b-time {
          font-size: 11px;
          color: #9ca3af;
          margin-top: 6px;
          text-align: right;
        }
      }
      .ai-bubble {
        background: #ffffff;
        border: 1px solid #f1e9da;
        border-top-left-radius: 4px;

        .cursor {
          display: inline-block;
          margin-left: 2px;
          color: #f49d47;
          animation: blink 0.9s infinite;
        }
      }
      .me-bubble {
        background: linear-gradient(135deg, #ffd5a3, #f49d47);
        color: #fff;
        border-top-right-radius: 4px;

        .b-content {
          color: #ffffff;
        }
        .b-time {
          color: rgba(255, 255, 255, 0.85);
        }
      }
    }
  }

  .chat-input {
    border-top: 1px solid #f0ecdf;
    padding: 16px 20px 18px;
    background: #fff;

    .input-wrap :deep(.el-textarea__inner) {
      border-radius: 12px;
      border-color: #ece6d6;
      padding: 12px 14px;
      font-size: 14px;
    }

    .send-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 10px;

      .tip {
        font-size: 12px;
        color: #9ca3af;
      }
      .send-btn {
        background: linear-gradient(90deg, #f49d47, #f28b2c);
        border: none;
        font-weight: 600;
        padding: 10px 22px;

        &:hover {
          filter: brightness(1.03);
        }
      }
    }
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.2; }
}

@media (max-width: 980px) {
  .consultation .c-inner {
    grid-template-columns: 1fr;
    padding: 12px;
  }
}
</style>
