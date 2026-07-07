<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAuditLogs } from '@/api/audit'
import type { AuditLog } from '@/api/audit'

const loading = ref(false)
const logs = ref<AuditLog[]>([])
const total = ref(0)
const query = reactive({ action: '', keyword: '', page: 1, size: 10 })

const actionMap: Record<string, string> = {
  HOTEL_AUDIT: '酒店审核',
  USER_STATUS: '用户状态',
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchAuditLogs(query)
    logs.value = result.list
    total.value = result.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>操作日志</template>
    <el-form :inline="true" @submit.prevent="loadData">
      <el-form-item label="动作">
        <el-select v-model="query.action" clearable style="width: 140px">
          <el-option label="酒店审核" value="HOTEL_AUDIT" />
          <el-option label="用户状态" value="USER_STATUS" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="操作人/详情" />
      </el-form-item>
      <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="logs" stripe>
      <el-table-column prop="operatorName" label="操作人" width="120" />
      <el-table-column label="动作" width="110">
        <template #default="{ row }">{{ actionMap[row.action] || row.action }}</template>
      </el-table-column>
      <el-table-column prop="detail" label="详情" min-width="240" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="时间" width="170" />
      <template #empty><el-empty description="暂无日志" /></template>
    </el-table>
    <el-pagination
      v-model:current-page="query.page"
      :page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="loadData"
    />
  </el-card>
</template>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
