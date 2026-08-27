import { ref } from 'vue'

// 全局侧边栏状态
const collapsed = ref(false)

export function useSidebar() {
  const toggle = () => {
    collapsed.value = !collapsed.value
  }

  const setCollapsed = (value) => {
    collapsed.value = value
  }

  return {
    collapsed,
    toggle,
    setCollapsed
  }
}
