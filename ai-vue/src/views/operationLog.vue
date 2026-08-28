<template>
  <div class="op-log">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1>📋 操作日志</h1>
        <p class="page-desc">记录管理端敏感操作(禁用账号/知识库变更/预警处理等),所有操作可追溯</p>
      </div>
      <div class="header-right">
        <el-button @click="loadLogs" :loading="loading" circle>
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 日志列表 -->
    <div class="chart-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-select v-model="query.module" placeholder="全部模块" clearable style="width: 150px" @change="handleSearch">
            <el-option label="用户管理" value="用户管理" />
            <el-option label="知识库管理" value="知识库管理" />
            <el-option label="风险预警" value="风险预警" />
          </el-select>
          <el-input
            v-model="query.keyword"
            placeholder="搜索操作类型 / 详情 / 操作人"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
      </div>

      <el-table :data="logs" v-loading="loading" stripe>
        <el-table-column prop="createdAt" label="操作时间" width="170" fixed>
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="adminUsername" label="操作人" width="130">
          <template #default="{ row }">
            <span class="admin-name">{{ row.adminUsername || '未知' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="120" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ row.module || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作类型" width="160">
          <template #default="{ row }">{{ row.operation || '—' }}</template>
        </el-table-column>
        <el-table-column prop="detail" label="操作详情" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="detail-text">{{ row.detail || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="操作IP" width="130">
          <template #default="{ row }">{{ row.ip || '—' }}</template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadLogs"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getOperationLogPage } from '@/api/admin.js'

const loading = ref(false)
const query = reactive({ module: null, keyword: '', page: 1, size: 10 })
const total = ref(0)
const logs = ref([])

// ---------- 列表加载 ----------
const loadLogs = async () => {
  loading.value = true
  try {
    const res = await getOperationLogPage({
      module: query.module,
      keyword: query.keyword,
      page: query.page,
      size: query.size
    })
    logs.value = res.records || []
    total.value = Number(res.total) || 0
  } catch (e) {
    logs.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.page = 1
  loadLogs()
}

const handleSizeChange = () => {
  query.page = 1
  loadLogs()
}

// 时间格式化
const formatTime = (t) => {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.op-log {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.page-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1a202c;
  margin: 0 0 4px;
}
.page-header .page-desc {
  font-size: 14px;
  color: #718096;
  margin: 0;
}
.page-header .el-button {
  width: 40px;
  height: 40px;
}

/* 列表卡片 */
.chart-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}
.table-toolbar {
  margin-bottom: 16px;
}
.toolbar-left {
  display: flex;
  gap: 12px;
}
.search-input {
  width: 280px;
}
.admin-name {
  font-weight: 600;
  color: #2d3748;
}
.detail-text {
  font-size: 12px;
  color: #718096;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 900px) {
  .toolbar-left {
    flex-wrap: wrap;
  }
  .search-input {
    width: 180px;
  }
}
</style>
