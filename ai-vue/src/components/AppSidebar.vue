<template>
  <div class="sidebar" :class="{ collapsed }">
    <!-- Logo 区域 -->
    <div class="sidebar-header">
      <div class="logo">
        <el-icon class="logo-icon"><Cpu /></el-icon>
        <span v-show="!collapsed" class="logo-text">瑾肃AI</span>
      </div>
    </div>

    <!-- 折叠按钮 -->
    <div class="collapse-btn" @click="toggle">
      <el-icon :size="16">
        <ArrowLeft v-if="!collapsed" />
        <ArrowRight v-else />
      </el-icon>
    </div>

    <!-- 导航菜单 -->
    <nav class="menu">
      <router-link
        v-for="item in visibleMenus"
        :key="item.key"
        :to="item.path"
        class="menu-item"
        :class="{ active: currentKey === item.key }"
      >
        <span class="menu-icon">{{ item.icon }}</span>
        <span v-show="!collapsed" class="menu-text">{{ item.label }}</span>
      </router-link>
    </nav>

    <!-- 底部用户信息 -->
    <div class="sidebar-footer">
      <div class="user-info" @click="handleLogout">
        <el-icon class="user-icon"><User /></el-icon>
        <div v-show="!collapsed" class="user-detail">
          <div class="user-name">{{ displayName }}</div>
          <div class="user-action">退出登录</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Cpu, ArrowLeft, ArrowRight, User } from '@element-plus/icons-vue'
import { useSidebar } from '@/composables/useSidebar'

const route = useRoute()
const router = useRouter()

// 使用全局侧边栏状态
const { collapsed, toggle } = useSidebar()

// 获取用户信息
const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

// 显示名称
const displayName = computed(() => {
  return userInfo.value.nickname || userInfo.value.username || '用户'
})

// 是否为管理员
const isAdmin = computed(() => {
  return userInfo.value.uesrType === 2 || userInfo.value.roleType === '2'
})

// 普通用户菜单（用户端功能保持不变）
const userMenus = [
  { key: 'home', label: '首页', path: '/home', icon: '🏠' },
  { key: 'consultation', label: 'AI咨询', path: '/consultation', icon: '💬' },
  { key: 'emotional', label: '情绪日记', path: '/emotional', icon: '📝' },
  { key: 'knowledge', label: '知识库', path: '/knowledge', icon: '📚' }
]

// 管理员菜单（最终结构：用户管理/风险预警/数据分析/知识库管理/操作日志）
const adminMenus = [
  { key: 'userManage', label: '用户管理', path: '/users', icon: '👥' },
  { key: 'riskAlert', label: '风险预警', path: '/alerts', icon: '🚨' },
  { key: 'dashboard', label: '数据分析', path: '/dashboard', icon: '📊' },
  { key: 'knowledgeAdmin', label: '知识库管理', path: '/knowledge-admin', icon: '📚' },
  { key: 'operationLog', label: '操作日志', path: '/logs', icon: '📋' }
]

// 根据角色过滤菜单：管理员只显示后台管理菜单，普通用户菜单不变
const visibleMenus = computed(() => {
  return isAdmin.value ? adminMenus : userMenus
})

// 当前激活的菜单
const currentKey = computed(() => route.meta.navKey)

// 退出登录
function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      ElMessage.success('已退出登录')
      router.replace('/auth/login')
    })
    .catch(() => {})
}
</script>

<style lang="scss" scoped>
.sidebar {
  width: 220px;
  min-height: 100vh;
  background: linear-gradient(180deg, #1e3a3a 0%, #2d5a5a 100%);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 200;
  transition: width 0.3s ease;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.1);

  &.collapsed {
    width: 64px;
  }
}

.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;

  .logo-icon {
    font-size: 32px;
    color: #4fd1c5;
    flex-shrink: 0;
  }

  .logo-text {
    font-size: 18px;
    font-weight: 700;
    color: #ffffff;
    white-space: nowrap;
    overflow: hidden;
  }
}

.collapse-btn {
  position: absolute;
  top: 60px;
  right: -12px;
  width: 24px;
  height: 24px;
  background: #4fd1c5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #1e3a3a;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 10;
  transition: background 0.2s;

  &:hover {
    background: #81e6d9;
  }
}

.menu {
  flex: 1;
  padding: 12px 10px 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 12px 12px 10px;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  transition: all 0.2s ease;
  white-space: nowrap;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #ffffff;
  }

  &.active {
    background: rgba(79, 209, 197, 0.2);
    color: #4fd1c5;
    font-weight: 600;

    &::before {
      content: '';
      width: 4px;
      height: 20px;
      background: #4fd1c5;
      border-radius: 2px;
      margin-right: -4px;
    }
  }

  .menu-icon {
    font-size: 22px;
    flex-shrink: 0;
    text-align: center;
  }

  .menu-text {
    font-size: 15px;
  }
}

.sidebar-footer {
  padding: 16px 8px 16px 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);

  .sidebar.collapsed & {
    padding: 16px 6px;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 8px 10px 6px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }

  /* 折叠时靠左显示 */
  .sidebar.collapsed & {
    justify-content: center;
    padding: 8px;
  }
}

.user-icon {
  font-size: 24px;
  color: #4fd1c5;
  flex-shrink: 0;
  background: rgba(79, 209, 197, 0.2);
  padding: 8px;
  border-radius: 50%;
}

.user-detail {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-action {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 2px;
}
</style>
