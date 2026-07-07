<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { auditHotel, fetchAdminHotels } from '@/api/hotel'
import type { AdminHotel } from '@/types/hotel'

const authStore = useAuthStore()
const loading = ref(false)
const hotels = ref<AdminHotel[]>([])
const total = ref(0)
const query = reactive({ status: '', keyword: '', page: 1, size: 10 })

const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

const statusMap: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  OFFLINE: '已下架',
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchAdminHotels(query)
    hotels.value = result.list
    total.value = result.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleAudit(row: AdminHotel, status: string) {
  let rejectReason: string | undefined
  if (status === 'REJECTED') {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回酒店', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    rejectReason = value
  } else {
    await ElMessageBox.confirm(`确定通过「${row.name}」？`, '审核通过')
  }
  await auditHotel(row.id, { status, rejectReason })
  ElMessage.success('操作成功')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div>
    <el-card>
      <template #header>
        <div class="header">
          <span>酒店审核</span>
          <el-tag v-if="!isAdmin" type="warning">仅管理员可审核</el-tag>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent="loadData">
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="酒店名/地址" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="hotels" stripe>
        <el-table-column prop="name" label="酒店名称" min-width="160" />
        <el-table-column prop="cityName" label="城市" width="100" />
        <el-table-column prop="merchantName" label="商家" width="120" />
        <el-table-column prop="starRating" label="星级" width="70" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ statusMap[row.status] || row.status }}</template>
        </el-table-column>
        <el-table-column prop="minPrice" label="最低价" width="90" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="isAdmin && row.status === 'PENDING'">
              <el-button link type="success" @click="handleAudit(row, 'APPROVED')">通过</el-button>
              <el-button link type="danger" @click="handleAudit(row, 'REJECTED')">驳回</el-button>
            </template>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
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
  </div>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.muted {
  color: #909399;
}
</style>
