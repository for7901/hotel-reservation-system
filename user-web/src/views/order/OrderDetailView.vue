<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import { cancelOrder, completeOrder, fetchOrderDetail, payOrder } from '@/api/order'
import { ORDER_STATUS } from '@/types/order'
import type { Order } from '@/types/order'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const paying = ref(false)
const cancelling = ref(false)
const completing = ref(false)
const order = ref<Order | null>(null)

onMounted(async () => {
  try {
    order.value = await fetchOrderDetail(Number(route.params.id))
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
})

async function handlePay() {
  if (!order.value) return
  paying.value = true
  try {
    order.value = await payOrder(order.value.id)
    showSuccessToast('支付成功')
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '支付失败')
  } finally {
    paying.value = false
  }
}

async function handleCancel() {
  if (!order.value) return
  try {
    await showConfirmDialog({ title: '确定取消订单？' })
    cancelling.value = true
    order.value = await cancelOrder(order.value.id)
    showSuccessToast('已取消')
  } catch (e) {
    if (e !== 'cancel') {
      showFailToast(e instanceof Error ? e.message : '取消失败')
    }
  } finally {
    cancelling.value = false
  }
}

async function handleComplete() {
  if (!order.value) return
  try {
    await showConfirmDialog({ title: '确认已完成入住？' })
    completing.value = true
    order.value = await completeOrder(order.value.id)
    showSuccessToast('订单已完成')
  } catch (e) {
    if (e !== 'cancel') {
      showFailToast(e instanceof Error ? e.message : '操作失败')
    }
  } finally {
    completing.value = false
  }
}

function goReview() {
  if (!order.value) return
  router.push(`/orders/${order.value.id}/review`)
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="订单详情" left-arrow @click-left="router.back()" />
    <van-skeleton v-if="loading" title :row="6" style="padding: 16px" />
    <template v-else-if="order">
      <van-cell-group inset>
        <van-cell title="订单号" :value="order.orderNo" />
        <van-cell title="状态">
          <template #value>
            <span :class="['status-text', order.status]">{{ ORDER_STATUS[order.status] || order.status }}</span>
          </template>
        </van-cell>
        <van-cell title="酒店" :value="order.hotelName" />
        <van-cell title="房型" :value="order.roomTypeName" />
        <van-cell title="入住" :value="`${order.checkInDate} ~ ${order.checkOutDate}（${order.nights}晚）`" />
        <van-cell title="入住人" :value="`${order.guestName} ${order.guestPhone}`" />
        <van-cell title="总价" :value="`¥${order.totalAmount}`" />
        <van-cell v-if="order.discountAmount > 0" title="优惠" :value="`-¥${order.discountAmount}`" />
        <van-cell v-if="order.createdAt" title="下单时间" :value="order.createdAt" />
        <van-cell v-if="order.paidAt" title="支付时间" :value="order.paidAt" />
      </van-cell-group>
      <div v-if="order.status === 'PENDING_PAYMENT'" class="actions">
        <van-button type="primary" block :loading="paying" @click="handlePay">模拟支付</van-button>
        <van-button block plain class="mt" :loading="cancelling" @click="handleCancel">取消订单</van-button>
      </div>
      <div v-else-if="order.status === 'CONFIRMED'" class="actions">
        <van-button type="primary" block :loading="completing" @click="handleComplete">确认完成</van-button>
        <van-button block plain class="mt" type="danger" :loading="cancelling" @click="handleCancel">取消订单</van-button>
      </div>
      <div v-else-if="order.status === 'COMPLETED'" class="actions">
        <van-button type="primary" block @click="goReview">去评价</van-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
}

.actions {
  margin: 24px 16px;
}

.mt {
  margin-top: 12px;
}

.status-text.PENDING_PAYMENT {
  color: #ee0a24;
}

.status-text.CONFIRMED {
  color: #07c160;
}

.status-text.COMPLETED {
  color: #1989fa;
}
</style>
