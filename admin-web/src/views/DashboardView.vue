<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { fetchAdminStats, fetchMerchantStats } from '@/api/dashboard'
import type { DashboardStats } from '@/api/dashboard'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const stats = ref<DashboardStats | null>(null)

const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

const statCards = computed(() => {
  if (!stats.value) return []
  if (isAdmin.value) {
    return [
      { title: '今日订单', value: stats.value.todayOrderCount, color: '#409eff' },
      { title: '今日营收', value: `¥${stats.value.todayRevenue}`, color: '#67c23a' },
      { title: '待审核酒店', value: stats.value.pendingHotelCount, color: '#e6a23c' },
      { title: '待支付订单', value: stats.value.pendingPaymentCount, color: '#f56c6c' },
      { title: '平台用户', value: stats.value.totalUserCount, color: '#909399' },
      { title: '上架酒店', value: stats.value.hotelCount, color: '#409eff' },
    ]
  }
  return [
    { title: '我的酒店', value: stats.value.hotelCount, color: '#409eff' },
    { title: '今日订单', value: stats.value.todayOrderCount, color: '#67c23a' },
    { title: '今日营收', value: `¥${stats.value.todayRevenue}`, color: '#e6a23c' },
    { title: '待支付', value: stats.value.pendingPaymentCount, color: '#f56c6c' },
    { title: '累计订单', value: stats.value.totalOrderCount, color: '#909399' },
    { title: '审核中酒店', value: stats.value.pendingHotelCount, color: '#409eff' },
  ]
})

const shortcuts = computed(() => {
  if (isAdmin.value) {
    return [
      { label: '酒店审核', path: '/audit' },
      { label: '订单管理', path: '/orders' },
      { label: '用户管理', path: '/users' },
    ]
  }
  return [
    { label: '我的酒店', path: '/hotels' },
    { label: '订单管理', path: '/orders' },
  ]
})

onMounted(async () => {
  loading.value = true
  try {
    stats.value = isAdmin.value ? await fetchAdminStats() : await fetchMerchantStats()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载统计失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <el-row :gutter="16">
      <el-col v-for="card in statCards" :key="card.title" :span="8" class="stat-col">
        <el-card shadow="hover">
          <div class="stat-title">{{ card.title }}</div>
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="welcome-card">
      <p>欢迎回来，{{ authStore.user?.nickname }}</p>
      <div class="shortcuts">
        <el-button v-for="item in shortcuts" :key="item.path" type="primary" plain @click="router.push(item.path)">
          {{ item.label }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-col {
  margin-bottom: 16px;
}

.stat-title {
  color: #909399;
  font-size: 14px;
}

.stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 600;
}

.welcome-card p {
  margin: 0 0 16px;
  color: #606266;
}

.shortcuts {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
