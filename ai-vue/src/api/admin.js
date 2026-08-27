import request from '@/utils/request'

// 登录
export const login = (data) => {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

// 注册
export const register = (data) => {
  return request({
    url: '/user/add',
    method: 'post',
    data
  })
}

// 获取当前用户的咨询会话列表
export const listSessions = () => {
  return request({
    url: '/psychological-chat/sessions',
    method: 'get'
  })
}

// 获取某会话的历史消息（sessionDbId 为数据库主键 ID，不含 session_ 前缀）
export const listMessages = (sessionDbId) => {
  return request({
    url: `/psychological-chat/sessions/${sessionDbId}/messages`,
    method: 'get'
  })
}

// 发起新的咨询会话, initialMessage 作为第一条消息
export const startSession = (data) => {
  return request({
    url: '/psychological-chat/session/start',
    method: 'post',
    data
  })
}

/**
 * 通过 SSE 流式接收 AI 回复
 * @param {string} sessionId 形如 "session_123"
 * @param {string} userMessage 用户消息
 * @param {object} callbacks { onMessage(content), onDone(), onError(err) }
 * @returns {{ close: () => void }} 手动关闭连接的方法
 */
export const streamChat = (sessionId, userMessage, callbacks = {}) => {
  const token = localStorage.getItem('token') || ''
  // 先 POST 创建流,后端通过 /psychological-chat/stream (produces=text/event-stream) 返回 SSE 流
  // 但由于浏览器 EventSource 不支持 POST+body,这里用原生 fetch + ReadableStream 手动解析 SSE
  const ctrl = new AbortController()

  const body = JSON.stringify({ sessionId, userMessage })

  fetch('/api/psychological-chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      token
    },
    body,
    signal: ctrl.signal
  }).then((res) => {
    if (!res.ok) {
      throw new Error(`请求失败: ${res.status}`)
    }
    if (!res.body) {
      throw new Error('响应体为空')
    }
    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    // SSE 解析:事件以空行分隔
    function parseChunk(chunkText) {
      buffer += chunkText
      let idx
      while ((idx = buffer.indexOf('\n\n')) !== -1) {
        const rawEvent = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        parseEvent(rawEvent)
      }
    }
    function parseEvent(rawEvent) {
      // SSE 事件格式: 行首 event: xxx / data: xxx
      const lines = rawEvent.split('\n')
      let event = 'message'
      const dataLines = []
      for (const line of lines) {
        if (line.startsWith('event:')) {
          event = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).trimStart())
        }
      }
      const dataStr = dataLines.join('\n')
      if (event === 'done') {
        callbacks.onDone && callbacks.onDone()
      } else if (event === 'message') {
        try {
          const parsed = JSON.parse(dataStr)
          if (parsed.code === '200' && parsed.data) {
            callbacks.onMessage && callbacks.onMessage(parsed.data.content || '', parsed.data)
          } else {
            callbacks.onError && callbacks.onError(new Error(parsed.msg || 'AI 返回异常'))
          }
        } catch (e) {
          // 个别分片可能不是完整 JSON,兜底直接透传
          callbacks.onMessage && callbacks.onMessage(dataStr, { type: 'raw' })
        }
      } else if (event === 'error') {
        try {
          const parsed = JSON.parse(dataStr)
          callbacks.onError && callbacks.onError(new Error(parsed.msg || 'AI 返回异常'))
        } catch (e) {
          callbacks.onError && callbacks.onError(new Error(dataStr))
        }
      }
    }
    function read() {
      reader.read().then(({ done, value }) => {
        if (done) {
          // 若末尾 buffer 还有内容,解析一次
          if (buffer.trim()) {
            parseEvent(buffer)
            buffer = ''
          }
          callbacks.onDone && callbacks.onDone()
          return
        }
        parseChunk(decoder.decode(value, { stream: true }))
        read()
      }).catch((err) => {
        if (err.name !== 'AbortError') {
          callbacks.onError && callbacks.onError(err)
        }
      })
    }
    read()
  }).catch((err) => {
    if (err.name !== 'AbortError') {
      callbacks.onError && callbacks.onError(err)
    }
  })

  return {
    close: () => ctrl.abort()
  }
}

// 查询当前用户今日情绪概要
export const getTodayMood = () => {
  return request({
    url: '/psychological-chat/mood/today',
    method: 'get'
  })
}

// 查询某个会话当前的情绪(按会话维度独立记分)
export const getSessionMood = (sessionDbId) => {
  return request({
    url: `/psychological-chat/mood/session/${sessionDbId}`,
    method: 'get'
  })
}

// 删除某个会话(级联删除该会话所有消息,只能删当前用户自己的)
export const deleteSession = (sessionDbId) => {
  return request({
    url: `/psychological-chat/sessions/${sessionDbId}`,
    method: 'delete'
  })
}

// 重命名会话
export const renameSession = (sessionDbId, sessionTitle) => {
  return request({
    url: `/psychological-chat/sessions/${sessionDbId}/rename`,
    method: 'put',
    data: { sessionTitle }
  })
}

// ===== 情绪日记(对应后端真实表 emotion_diary,1-10 mood_score 存库,接口输出×10做UI分) =====

// 心情字典选项:levels 是 10 档 1-10;buckets 是 5 档
export const getEmotionOptions = () => {
  return request({
    url: '/emotion-diary/options',
    method: 'get'
  })
}

// 写一条心情记录
// data: { diaryDate:'YYYY-MM-DD'|undefined, moodScore: 1-10, dominantEmotion?:string, emotionTriggers?:string, diaryContent?:string, sleepQuality?:1-5, stressLevel?:1-5 }
export const addEmotionLog = (data) => {
  return request({
    url: '/emotion-diary/add',
    method: 'post',
    data
  })
}

// 某月统计 + 每日聚合 + 每日原始记录
// date 可选,格式 'YYYY-MM',不传默认本月
export const getEmotionMonthStats = (date) => {
  return request({
    url: '/emotion-diary/month',
    method: 'get',
    params: { date }
  })
}

// 今日统计 + 今日所有记录
export const getEmotionTodaySummary = () => {
  return request({
    url: '/emotion-diary/today',
    method: 'get'
  })
}

// ===== 知识库 =====

// 获取所有启用的分类
export const getKnowledgeCategories = () => {
  return request({
    url: '/knowledge/categories',
    method: 'get'
  })
}

// 分页查询文章(categoryId可选,keyword可选,page默认1,size默认10)
export const getKnowledgeArticles = (params = {}) => {
  return request({
    url: '/knowledge/articles',
    method: 'get',
    params
  })
}

// 文章详情(阅读数+1)
export const getKnowledgeArticleDetail = (id) => {
  return request({
    url: `/knowledge/articles/${id}`,
    method: 'get'
  })
}

// 检查当前用户是否已收藏某文章
export const checkKnowledgeFavorited = (id) => {
  return request({
    url: `/knowledge/user/articles/${id}/favorited`,
    method: 'get'
  })
}

// 收藏文章
export const favoriteKnowledge = (id) => {
  return request({
    url: `/knowledge/user/articles/${id}/favorite`,
    method: 'post'
  })
}

// 取消收藏
export const unfavoriteKnowledge = (id) => {
  return request({
    url: `/knowledge/user/articles/${id}/favorite`,
    method: 'delete'
  })
}

// 当前用户的收藏列表
export const getKnowledgeFavorites = () => {
  return request({
    url: '/knowledge/user/favorites',
    method: 'get'
  })
}

// ========== Dashboard 统计接口 ==========

// 获取系统统计数据（管理员）
export const getDashboardStats = () => {
  return request({
    url: '/dashboard/stats',
    method: 'get'
  })
}

// 获取30天趋势数据（管理员）
export const getDashboardTrend = () => {
  return request({
    url: '/dashboard/trend',
    method: 'get'
  })
}

// 获取当前用户的个人统计数据
export const getMyDashboardStats = () => {
  return request({
    url: '/dashboard/my-stats',
    method: 'get'
  })
}

// ========== 用户资料接口 ==========

// 获取当前用户信息
export const getCurrentUser = () => {
  return request({
    url: '/user/current',
    method: 'get'
  })
}

// 更新用户资料
export const updateUserProfile = (data) => {
  return request({
    url: '/user/profile',
    method: 'put',
    data
  })
}

// 修改密码
export const updateUserPassword = (data) => {
  return request({
    url: '/user/password',
    method: 'put',
    data
  })
}


