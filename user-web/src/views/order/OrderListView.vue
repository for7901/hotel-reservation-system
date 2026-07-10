<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { fetchMyOrders } from '@/api/order'
import { ORDER_STATUS } from '@/types/order'
import type { Order } from '@/types/order'

const router = useRouter()
const loading = ref(false)
const fetching = ref(false)
const orders = ref<Order[]>([])
const finished = ref(false)
const page = ref(1)
const activeTab = ref('ALL')
const initialized = ref(false)

/** 全部 / 待支付 / 待出行 / 待点评 / 退款单（全部含已取消、已点评） */
const tabs = [
  { name: 'ALL', title: '全部' },
  { name: 'PENDING_PAYMENT', title: '待支付' },
  { name: 'UPCOMING', title: '待出行' },
  { name: 'PENDING_REVIEW', title: '待点评' },
  { name: 'REFUND', title: '退款单' },
]

async function loadOrders(reset = false) {
  if (fetching.value) return
  fetching.value = true
  if (reset) {
    page.value = 1
    orders.value = []
    finished.value = false
  }
  loading.value = true
  try {
    const result = await fetchMyOrders({
      status: activeTab.value || 'ALL',
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
    fetching.value = false
    initialized.value = true
  }
}

function onTabChange(name: string | number) {
  activeTab.value = String(name)
  loadOrders(true)
}

function statusLabel(order: Order) {
  if (order.status === 'PAID' || order.status === 'CONFIRMED') return '待出行'
  if (order.status === 'COMPLETED') return order.reviewed ? '已点评' : '待点评'
  if (order.status === 'CHECKOUT_PENDING') return '退款中'
  if (order.status === 'REFUNDED' || order.status === 'REFUNDING') return '已退款'
  if (order.status === 'CANCELLED') return '已取消'
  return ORDER_STATUS[order.status] || order.status
}

onMounted(() => {
  initialized.value = true
  loadOrders(true)
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
      :immediate-check="false"
      finished-text="没有更多了"
      @load="loadOrders()"
    >
      <van-cell-group
        v-for="order in orders"
        :key="order.id"
        inset
        class="card"
        @click="router.push(`/orders/${order.id}`)"
      >
        <van-cell :title="order.hotelName" :label="order.roomTypeName" is-link>
          <template #value>
            <span :class="['status', order.status]">{{ statusLabel(order) }}</span>
          </template>
        </van-cell>
        <van-cell
          :title="`${order.checkInDate} ~ ${order.checkOutDate}`"
          :label="`${order.nights}晚 · ${order.roomCount || order.guestCount || 1}间`"
          :value="`¥${order.totalAmount}`"
        />
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

.status.PAID,
.status.CONFIRMED {
  color: #07c160;
}

.status.COMPLETED {
  color: #1989fa;
}

.status.CHECKOUT_PENDING {
  color: #ff976a;
}

.status.CANCELLED,
.status.REFUNDED {
  color: #969799;
}
</style>
