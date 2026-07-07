<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchUsers, updateUserStatus } from '@/api/user'
import type { AdminUser } from '@/api/user'

const loading = ref(false)
const users = ref<AdminUser[]>([])
const total = ref(0)
const query = reactive({ role: '', status: undefined as number | undefined, keyword: '', page: 1, size: 10 })

const roleMap: Record<string, string> = {
  USER: '普通用户',
  MERCHANT: '商家',
  ADMIN: '管理员',
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchUsers({
      role: query.role || undefined,
      status: query.status,
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size,
    })
    users.value = result.list
    total.value = result.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row: AdminUser) {
  const nextStatus = row.status === 1 ? 0 : 1
  const action = nextStatus === 0 ? '禁用' : '启用'
  try {
    await updateUserStatus(row.id, nextStatus)
    ElMessage.success(`已${action}`)
    loadData()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : `${action}失败`)
  }
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>用户管理</template>
    <el-form :inline="true" @submit.prevent="loadData">
      <el-form-item label="角色">
        <el-select v-model="query.role" clearable placeholder="全部" style="width: 120px">
          <el-option label="普通用户" value="USER" />
          <el-option label="商家" value="MERCHANT" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 100px">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="手机号/昵称" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="users" stripe>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }">{{ roleMap[row.role] || row.role }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" min-width="160" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.role !== 'ADMIN'"
            link
            :type="row.status === 1 ? 'danger' : 'primary'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无用户" />
      </template>
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
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
