<template>
  <div class="site-layout">
    <!-- 左侧可折叠侧边栏 -->
    <AppSidebar />
    
    <!-- 主内容区域 -->
    <div class="main-wrapper" :class="{ collapsed: isCollapsed }">
      <SiteNavbar />
      <main class="site-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AppSidebar from './AppSidebar.vue'
import SiteNavbar from './SiteNavbar.vue'
import { useSidebar } from '@/composables/useSidebar'

const { collapsed } = useSidebar()

const isCollapsed = computed(() => collapsed.value)
</script>

<style lang="scss" scoped>
.site-layout {
  min-height: 100vh;
  background: #f7f8fa;
  display: flex;
}

.main-wrapper {
  margin-left: 220px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  transition: margin-left 0.3s ease;

  &.collapsed {
    margin-left: 64px;
  }
}

.site-main {
  flex: 1;
  width: 100%;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
