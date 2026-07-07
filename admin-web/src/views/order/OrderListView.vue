<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { fetchAdminOrders, fetchMerchantOrders, fetchOrderDetail } from '@/api/order'
import { ORDER_STATUS, type Order } from '@/types/order'

const authStore = useAuthStore()
const loading = ref(false)
const detailLoading = ref(false)
const orders = ref<Order[]>([])
const total = ref(0)
const detailVisible = ref(false)
const currentOrder = ref<Order | null>(null)
const query = reactive({ status: '', keyword: '', page: 1, size: 10 })

const statusTagType: Record<string, string> = {
  PENDING_PAYMENT: 'warning',
  CONFIRMED: 'success',
  CANCELLED: 'info',
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      status: query.status || undefined,
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size,
    }
    const result = authStore.user?.role === 'ADMIN'
      ? await fetchAdminOrders(params)
      : await fetchMerchantOrders(params)
    orders.value = result.list
    total.value = result.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(row: Order) {
  detailLoading.value = true
  detailVisible.value = true
  try {
    currentOrder.value = await fetchOrderDetail(row.id)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>订单管理</template>
    <el-form :inline="true" @submit.prevent="loadData">
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="(label, key) in ORDER_STATUS" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="订单号/酒店/入住人" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="orders" stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column prop="hotelName" label="酒店" min-width="140" />
      <el-table-column prop="roomTypeName" label="房型" width="120" />
      <el-table-column prop="guestName" label="入住人" width="100" />
      <el-table-column label="入住日期" width="180">
        <template #default="{ row }">{{ row.checkInDate }} ~ {{ row.checkOutDate }}</template>
      </el-table-column>
      <el-table-column label="金额" width="100">
        <template #default="{ row }">¥{{ row.totalAmount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(statusTagType[row.status] as any) || 'info'">
            {{ ORDER_STATUS[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无订单" />
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

  <el-drawer v-model="detailVisible" title="订单详情" size="420px">
    <div v-loading="detailLoading">
      <template v-if="currentOrder">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ ORDER_STATUS[currentOrder.status] }}</el-descriptions-item>
          <el-descriptions-item label="酒店">{{ currentOrder.hotelName }}</el-descriptions-item>
          <el-descriptions-item label="房型">{{ currentOrder.roomTypeName }}</el-descriptions-item>
          <el-descriptions-item label="入住">{{ currentOrder.checkInDate }} ~ {{ currentOrder.checkOutDate }}</el-descriptions-item>
          <el-descriptions-item label="晚数">{{ currentOrder.nights }} 晚</el-descriptions-item>
          <el-descriptions-item label="入住人">{{ currentOrder.guestName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentOrder.guestPhone }}</el-descriptions-item>
          <el-descriptions-item label="总价">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ currentOrder.createdAt }}</el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.paidAt" label="支付时间">{{ currentOrder.paidAt }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </div>
  </el-drawer>
</template>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
