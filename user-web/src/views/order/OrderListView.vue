<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { fetchMyOrders } from '@/api/order'
import { ORDER_STATUS } from '@/types/order'
import type { Order } from '@/types/order'

const router = useRouter()
const loading = ref(false)
const orders = ref<Order[]>([])
const finished = ref(false)
const page = ref(1)
const activeTab = ref('')
const initialized = ref(false)

const tabs = [
  { name: '', title: '全部' },
  { name: 'PENDING_PAYMENT', title: '待支付' },
  { name: 'CONFIRMED', title: '已确认' },
  { name: 'CANCELLED', title: '已取消' },
]

async function loadOrders(reset = false) {
  try {
    if (reset) {
      page.value = 1
      orders.value = []
      finished.value = false
    }
    const result = await fetchMyOrders({
      status: activeTab.value || undefined,
      page: page.value,
      size: 10,
    })
    orders.value.push(...result.list)
    finished.value = orders.value.length >= result.total || result.list.length === 0
    if (!finished.value) {
      page.value += 1
    }
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '加载失败')
    finished.value = true
  } finally {
    loading.value = false
    initialized.value = true
  }
}

function onTabChange(name: string | number) {
  activeTab.value = String(name)
  loadOrders(true)
}

onMounted(async () => {
  await loadOrders(true)
  initialized.value = true
})
</script>

<template>
  <div class="page">
    <van-nav-bar title="我的订单" />
    <van-tabs v-model:active="activeTab" @change="onTabChange">
      <van-tab v-for="tab in tabs" :key="tab.name" :name="tab.name" :title="tab.title" />
    </van-tabs>
    <van-list
      v-if="initialized"
      v-model:loading="loading"
      :finished="finished"
      finished-text="没有更多了"
      @load="loadOrders()"
    >
      <van-cell-group v-for="order in orders" :key="order.id" inset class="card" @click="router.push(`/orders/${order.id}`)">
        <van-cell :title="order.hotelName" :label="order.roomTypeName" is-link>
          <template #value>
            <span :class="['status', order.status]">{{ ORDER_STATUS[order.status] }}</span>
          </template>
        </van-cell>
        <van-cell :title="`${order.checkInDate} ~ ${order.checkOutDate}`" :label="`${order.nights}晚`" :value="`¥${order.totalAmount}`" />
      </van-cell-group>
      <van-empty v-if="!loading && orders.length === 0" description="暂无订单">
        <van-button type="primary" size="small" @click="router.push('/')">去预订</van-button>
      </van-empty>
    </van-list>
    <van-skeleton v-else title :row="4" style="padding: 16px" />
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.card {
  margin-top: 12px;
}

.status {
  font-size: 13px;
}

.status.PENDING_PAYMENT {
  color: #ee0a24;
}

.status.CONFIRMED {
  color: #07c160;
}

.status.CANCELLED {
  color: #969799;
}
</style>
