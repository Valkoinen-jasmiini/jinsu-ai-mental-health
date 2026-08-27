<template>
  <div class="container">
    <div class="title">
      <div class="back-home" @click="router.push('/home')">
        <el-icon><Back /></el-icon>
        <span>返回首页</span>
      </div>
      <div class="title-text">
        <h2>创建你的账号</h2>
        <p>加入我们,开启温暖陪伴之旅</p>
      </div>
    </div>
    <div class="form-container">
      <el-form
        ref="ruleFormRef"
        :model="formData"
        :rules="rules"
        label-position="top"
        status-icon
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="formData.username"
            size="large"
            placeholder="3-50 位字母或数字,登录时使用"
            maxlength="50"
            show-word-limit
            clearable
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="formData.nickname"
            size="large"
            placeholder="选填,留空默认使用用户名"
            maxlength="50"
            show-word-limit
            clearable
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="formData.email"
            size="large"
            placeholder="例如 you@example.com,可用于登录"
            maxlength="100"
            clearable
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="formData.phone"
            size="large"
            placeholder="11 位大陆手机号,选填"
            maxlength="11"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            size="large"
            placeholder="6-50 位,字母数字符号均可"
            type="password"
            show-password
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="formData.confirmPassword"
            size="large"
            placeholder="再输入一遍密码"
            type="password"
            show-password
            maxlength="50"
          />
        </el-form-item>
        <el-button
          class="btn"
          size="large"
          type="primary"
          :loading="submitting"
          @click="submitForm(ruleFormRef)"
        >
          {{ submitting ? '正在注册...' : '立即注册并登录' }}
        </el-button>
      </el-form>
      <div class="footer">
        <p>已经有账号？<router-link to="/auth/login">直接登录</router-link></p>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, login } from '@/api/admin.js'

const router = useRouter()

const ruleFormRef = ref()
const submitting = ref(false)

const formData = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

/* ---------- 校验 ---------- */
// 两次密码一致校验
const validateConfirm = (rule, value, cb) => {
  if (!value) return cb(new Error('请再输入一遍密码'))
  if (value !== formData.password) return cb(new Error('两次输入的密码不一致'))
  cb()
}
// 手机号格式(选填:空就放过,填了就要对)
const validatePhone = (rule, value, cb) => {
  if (!value) return cb()
  if (!/^1[3-9]\d{9}$/.test(value)) return cb(new Error('手机号格式不正确'))
  cb()
}
// 用户名 3-50 字母数字下划线(和后端 DTO 保持一致)
const validateUsername = (rule, value, cb) => {
  if (!value) return cb(new Error('请输入用户名'))
  if (value.length < 3 || value.length > 50) return cb(new Error('用户名长度 3-50 位'))
  if (!/^[a-zA-Z0-9_]+$/.test(value)) return cb(new Error('用户名只能包含字母、数字和下划线'))
  cb()
}

const rules = reactive({
  username: [{ validator: validateUsername, trigger: 'blur' }],
  nickname: [
    {
      validator: (r, v, cb) => (v && v.length > 50 ? cb(new Error('昵称最多 50 个字符')) : cb()),
      trigger: 'blur'
    }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
    { max: 100, message: '邮箱最多 100 个字符', trigger: 'blur' }
  ],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度 6-50 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
})

/* ---------- 提交:注册成功 → 自动登录 → 跳首页 ---------- */
const submitForm = async (formEl) => {
  if (!formEl) return
  const valid = await formEl.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    // 1. 调注册接口(后端默认 userType=1 普通用户)
    await register({
      username: formData.username.trim(),
      nickname: formData.nickname ? formData.nickname.trim() : formData.username.trim(),
      email: formData.email.trim(),
      phone: formData.phone ? formData.phone.trim() : '',
      password: formData.password,
      confirmPassword: formData.confirmPassword,
      userType: 1
    })
    ElMessage.success('注册成功,正在为你自动登录...')

    // 2. 自动登录(复用登录接口)
    const loginData = await login({
      username: formData.username.trim(),
      password: formData.password
    })
    if (!loginData || !loginData.token) {
      ElMessage.error('自动登录失败,请手动前往登录页')
      router.push('/auth/login')
      return
    }
    localStorage.setItem('token', loginData.token)
    localStorage.setItem('userInfo', JSON.stringify(loginData.userInfo))
    ElMessage.success('已自动登录')

    // 3. 跳首页
    setTimeout(() => router.push('/home'), 300)
  } catch (e) {
    // 错误(用户名/邮箱/手机号 已注册等)已由 request 拦截器提示,这里不再重复
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.container {
  width: 420px;
  .title {
    .back-home {
      margin-bottom: 40px;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: #6b7280;
      cursor: pointer;
      transition: color 0.2s;
      &:hover {
        color: #10b981;
      }
    }
    .title-text {
      text-align: center;
      h2 {
        font-size: 32px;
        margin-bottom: 10px;
      }
      p {
        font-size: 16px;
        color: #6b7280;
      }
    }
  }
  .form-container {
    margin-top: 24px;
    .btn {
      margin-top: 10px;
      width: 100%;
    }
  }
  .footer {
    padding: 24px;
    text-align: center;
  }
}
</style>
