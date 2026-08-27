import { createRouter, createWebHistory } from 'vue-router'
import SiteLayout from '@/components/SiteLayout.vue'
import AuthLayout from '@/components/AuthLayout.vue'

// 获取用户角色
const getUserType = () => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return userInfo.uesrType || parseInt(userInfo.roleType) || 1
  } catch {
    return 1
  }
}

// 路由配置
const routes = [
  {
    path: '/',
    component: SiteLayout,
    redirect: '/home',
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/dashboard.vue'),
        meta: { title: '数据分析', navKey: 'dashboard', requiresAdmin: true }
      },
      {
        path: 'home',
        component: () => import('@/views/home.vue'),
        meta: { title: '首页', navKey: 'home' }
      },
      {
        path: 'consultation',
        component: () => import('@/views/consultation.vue'),
        meta: { title: 'AI咨询', navKey: 'consultation' }
      },
      {
        path: 'emotional',
        component: () => import('@/views/emotional.vue'),
        meta: { title: '情绪日记', navKey: 'emotional' }
      },
      {
        path: 'knowledge',
        component: () => import('@/views/knowledge.vue'),
        meta: { title: '知识库', navKey: 'knowledge' }
      },
      {
        path: 'profile',
        component: () => import('@/views/profile.vue'),
        meta: { title: '个人中心', navKey: 'profile' }
      }
    ]
  },
  {
    path: '/auth',
    component: AuthLayout,
    children: [
      {
        path: 'login',
        component: () => import('@/views/login.vue'),
        meta: { title: '登录' }
      },
      {
        path: 'register',
        component: () => import('@/views/register.vue'),
        meta: { title: '注册' }
      }
    ]
  },
  // 兼容旧的 /back 地址,直接跳到首页
  {
    path: '/back/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：登录检查 + 管理员权限检查
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.path.startsWith('/auth')) {
    next()
    return
  }

  if (!token) {
    next('/auth/login')
    return
  }

  // 检查是否需要管理员权限
  if (to.meta.requiresAdmin && getUserType() !== 2) {
    next('/home')  // 普通用户跳转到首页
    return
  }

  next()
})

export default router
