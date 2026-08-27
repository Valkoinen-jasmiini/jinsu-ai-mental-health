<template>
  <div class="profile-page">
    <div class="profile-container">
      <!-- 左侧卡片 - 基本信息 -->
      <div class="card basic-info">
        <div class="card-header">
          <h2>👤 基本资料</h2>
          <span class="card-subtitle">查看和修改您的基本信息</span>
        </div>
        <div class="card-body">
          <!-- 头像 -->
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <div class="avatar" v-if="!form.avatar">{{ avatarLetter }}</div>
              <img v-else :src="form.avatar" class="avatar" />
            </div>
            <div class="avatar-info">
              <h3>{{ displayName }}</h3>
              <el-tag :type="isAdmin ? 'warning' : 'success'" size="small">
                {{ isAdmin ? '管理员' : '普通用户' }}
              </el-tag>
            </div>
          </div>

          <!-- 表单 -->
          <el-form :model="form" :rules="rules" ref="basicFormRef" label-position="top" class="form">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :label="1">男</el-radio>
                <el-radio :label="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="生日">
              <el-date-picker v-model="form.birthday" type="date" placeholder="请选择生日" style="width: 100%" />
            </el-form-item>
            <el-form-item label="头像URL">
              <el-input v-model="form.avatar" placeholder="请输入头像URL（选填）" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdateProfile" :loading="updating">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 右侧卡片组 -->
      <div class="right-section">
        <!-- 我的数据 -->
        <div class="card data-overview">
          <div class="card-header">
            <h2>📊 我的数据</h2>
            <span class="card-subtitle">您使用平台的活动概览</span>
          </div>
          <div class="card-body stats-grid">
            <div class="stat-item">
              <div class="stat-value">{{ stats.favorites }}</div>
              <div class="stat-label">我的收藏</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.sessions }}</div>
              <div class="stat-label">AI咨询次数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.diaries }}</div>
              <div class="stat-label">情绪日记</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.articles }}</div>
              <div class="stat-label">阅读文章</div>
            </div>
          </div>
        </div>

        <!-- 修改密码 -->
        <div class="card password-card">
          <div class="card-header">
            <h2>🔐 修改密码</h2>
            <span class="card-subtitle">定期更换密码有助于账号安全</span>
          </div>
          <div class="card-body">
            <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-position="top" class="form">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleUpdatePassword" :loading="updatingPassword">修改密码</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

const displayName = computed(() => {
  return userInfo.value.nickname || userInfo.value.username || '用户'
})

const avatarLetter = computed(() => displayName.value.charAt(0).toUpperCase())

const isAdmin = computed(() => {
  return userInfo.value.uesrType === 2 || userInfo.value.roleType === '2'
})

// 基本资料表单
const basicFormRef = ref(null)
const updating = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  gender: null,
  birthday: ''
})

const rules = {
  nickname: [{ max: 50, message: '昵称长度不能超过50个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

// 密码表单
const passwordFormRef = ref(null)
const updatingPassword = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 255, message: '密码长度必须在6到255个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 统计数据
const stats = reactive({
  favorites: 0,
  sessions: 0,
  diaries: 0,
  articles: 0
})

// 加载用户信息
const loadUserInfo = async () => {
  // 先用 localStorage 中的数据初始化
  const stored = userInfo.value
  form.nickname = stored.nickname || ''
  form.email = stored.email || ''
  form.phone = stored.phone || ''
  form.avatar = stored.avatar || ''
  form.gender = stored.gender ?? null
  form.birthday = stored.birthday || ''

  try {
    // 使用项目封装的 request 实例（会自动带 token）
    const user = await request.get('/user/current')
    if (user) {
      // 用后端返回的数据覆盖
      form.nickname = user.nickname || form.nickname
      form.email = user.email || form.email
      form.phone = user.phone || form.phone
      form.avatar = user.avatar || form.avatar
      form.gender = user.gender ?? form.gender
      form.birthday = user.birthday || form.birthday
    }
  } catch (e) {
    console.warn('加载用户信息失败，使用本地缓存数据', e)
    // 失败时保留 localStorage 中的数据作为默认值
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    // 使用项目封装的 request 实例
    const myStats = await request.get('/dashboard/my-stats').catch(() => ({}))

    stats.favorites = myStats?.myFavorites || 0
    stats.sessions = myStats?.mySessions || 0
    stats.diaries = myStats?.myDiaries || 0
    stats.articles = myStats?.myReadArticles || 0
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

// 更新资料
const handleUpdateProfile = async () => {
  if (!basicFormRef.value) return
  await basicFormRef.value.validate(async (valid) => {
    if (valid) {
      updating.value = true
      try {
        const payload = { ...form }
        if (payload.birthday && typeof payload.birthday === 'object') {
          payload.birthday = payload.birthday.toISOString().split('T')[0]
        }
        // 使用项目封装的 request 实例
        const user = await request.put('/user/profile', payload)
        if (user) {
          ElMessage.success('资料更新成功')
          // 更新本地存储的用户信息
          const newUserInfo = { ...userInfo.value, ...user }
          localStorage.setItem('userInfo', JSON.stringify(newUserInfo))
        }
      } catch (e) {
        // request 拦截器已经处理了错误提示
      } finally {
        updating.value = false
      }
    }
  })
}

// 修改密码
const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      updatingPassword.value = true
      try {
        // 使用项目封装的 request 实例
        await request.put('/user/password', passwordForm)
        ElMessage.success('密码修改成功')
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
      } catch (e) {
        // request 拦截器已经处理了错误提示
      } finally {
        updatingPassword.value = false
      }
    }
  })
}

onMounted(() => {
  loadUserInfo()
  loadStats()
})
</script>

<style lang="scss" scoped>
.profile-page {
  padding: 24px;
}

.profile-container {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

@media (max-width: 900px) {
  .profile-container {
    grid-template-columns: 1fr;
  }
}

.card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;

  .card-header {
    padding: 20px 24px;
    border-bottom: 1px solid #f0f0f0;

    h2 {
      font-size: 18px;
      font-weight: 600;
      color: #1a1a1a;
      margin: 0 0 4px 0;
    }

    .card-subtitle {
      font-size: 13px;
      color: #999;
    }
  }

  .card-body {
    padding: 24px;
  }
}

.basic-info {
  .avatar-section {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
    padding-bottom: 20px;
    border-bottom: 1px dashed #eee;

    .avatar-wrapper {
      .avatar {
        width: 72px;
        height: 72px;
        border-radius: 50%;
        background: linear-gradient(135deg, #2d7d7f 0%, #4fd1c5 100%);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        font-weight: 600;
        object-fit: cover;
      }
    }

    .avatar-info {
      h3 {
        margin: 0 0 8px 0;
        font-size: 20px;
        font-weight: 600;
      }
    }
  }

  .form {
    :deep(.el-form-item) {
      margin-bottom: 18px;
    }
  }
}

.right-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.data-overview {
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .stat-item {
    text-align: center;
    padding: 20px;
    background: linear-gradient(135deg, #f8f9fa 0%, #f0f4f4 100%);
    border-radius: 12px;
    transition: transform 0.2s, box-shadow 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }

    .stat-value {
      font-size: 32px;
      font-weight: 700;
      background: linear-gradient(135deg, #2d7d7f 0%, #4fd1c5 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .stat-label {
      font-size: 13px;
      color: #666;
      margin-top: 4px;
    }
  }
}

.password-card {
  .form {
    :deep(.el-form-item) {
      margin-bottom: 16px;
    }
  }
}
</style>
