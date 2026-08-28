<template>
  <header class="site-navbar">
    <div class="nav-inner">
      <div class="brand" @click="goHome">
        <el-icon class="brand-icon"><Cpu /></el-icon>
        <span class="brand-text">瑾肃AI助手</span>
      </div>

      <nav class="menu">
        <router-link
          v-for="item in visibleMenus"
          :key="item.key"
          :to="item.path"
          class="menu-link"
          :class="{ active: currentKey === item.key }"
          active-class=""
        >
          <span class="link-icon">{{ item.icon }}</span>
          <span class="link-text">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="right">
        <el-dropdown v-if="userInfo.nickname || userInfo.username" trigger="click" @command="handleCommand">
          <div class="user-menu">
            <span class="user-name">{{ userInfo.nickname || userInfo.username }}</span>
            <el-tag v-if="isAdmin" type="warning" size="small" effect="dark" class="admin-tag">管理员</el-tag>
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人中心
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Cpu, SwitchButton, ArrowDown, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 获取用户信息
const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
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

// 管理员菜单（与侧边栏保持一致：用户管理/风险预警/数据分析/知识库管理/操作日志）
const adminMenus = [
  { key: 'userManage', label: '用户管理', path: '/users', icon: '👥' },
  { key: 'riskAlert', label: '风险预警', path: '/alerts', icon: '🚨' },
  { key: 'dashboard', label: '数据分析', path: '/dashboard', icon: '📊' },
  { key: 'knowledgeAdmin', label: '知识库管理', path: '/knowledge-admin', icon: '📚' },
  { key: 'operationLog', label: '操作日志', path: '/logs', icon: '📋' }
]

// 根据角色过滤菜单：管理员与管理端侧边栏一致，普通用户菜单不变
const visibleMenus = computed(() => {
  return isAdmin.value ? adminMenus : userMenus
})

const currentKey = computed(() => route.meta.navKey)

// 点击logo跳转
const goHome = () => {
  if (isAdmin.value) {
    router.push('/dashboard')
  } else {
    router.push('/home')
  }
}

// 下拉菜单命令处理
function handleCommand(command) {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

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
.site-navbar {
  height: 64px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

  .nav-inner {
    max-width: 1440px;
    margin: 0 auto;
    height: 100%;
    padding: 0 32px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .brand {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    user-select: none;

    .brand-icon {
      font-size: 28px;
      color: #2d7d7f;
    }

    .brand-text {
      font-size: 18px;
      font-weight: 700;
      background: linear-gradient(135deg, #2d7d7f 0%, #4fd1c5 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      letter-spacing: 0.5px;
    }
  }

  .menu {
    display: flex;
    align-items: center;
    gap: 4px;

    .menu-link {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 18px;
      border-radius: 10px;
      font-size: 14px;
      font-weight: 500;
      color: #6b7280;
      text-decoration: none;
      transition: all 0.25s ease;
      position: relative;

      .link-icon {
        font-size: 18px;
        transition: transform 0.25s;
      }

      &:hover {
        background: linear-gradient(135deg, rgba(45, 125, 127, 0.08) 0%, rgba(79, 209, 197, 0.08) 100%);
        color: #2d7d7f;
        transform: translateY(-1px);

        .link-icon {
          transform: scale(1.15);
        }
      }

      &.active {
        background: linear-gradient(135deg, #2d7d7f 0%, #4fd1c5 100%);
        color: #ffffff;
        box-shadow: 0 4px 12px rgba(45, 125, 127, 0.3);

        .link-icon {
          animation: bounce 0.4s ease;
        }

        &::after {
          content: '';
          position: absolute;
          bottom: -1px;
          left: 50%;
          transform: translateX(-50%);
          width: 30px;
          height: 3px;
          background: #4fd1c5;
          border-radius: 2px;
        }
      }
    }
  }

  .right {
    display: flex;
    align-items: center;
    gap: 16px;

    .user-menu {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 6px 14px;
      background: #f7f8fa;
      border-radius: 30px;
      cursor: pointer;
      transition: all 0.25s;
      user-select: none;

      &:hover {
        background: linear-gradient(135deg, rgba(45, 125, 127, 0.1) 0%, rgba(79, 209, 197, 0.1) 100%);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      }

      .user-name {
        font-size: 14px;
        font-weight: 600;
        color: #374151;
      }

      .admin-tag {
        font-size: 11px;
        padding: 0 6px;
        height: 20px;
        line-height: 20px;
      }

      .arrow-icon {
        font-size: 12px;
        color: #9ca3af;
        transition: transform 0.25s;
      }
    }
  }
}

// 下拉菜单样式
:deep(.el-dropdown-menu) {
  padding: 8px 0;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);

  .el-dropdown-menu__item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    font-size: 14px;
    color: #374151;
    min-width: 140px;

    &:hover {
      background: linear-gradient(135deg, rgba(45, 125, 127, 0.08) 0%, rgba(79, 209, 197, 0.08) 100%);
      color: #2d7d7f;
    }

    &.divided {
      border-top: 1px solid #e5e7eb;
      margin-top: 4px;
      padding-top: 12px;
    }
  }
}

@keyframes bounce {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
}
</style>
