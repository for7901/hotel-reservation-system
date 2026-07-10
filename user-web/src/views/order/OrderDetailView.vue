<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import { cancelOrder, deleteOrder, fetchOrderDetail, payOrder, applyCheckout } from '@/api/order'
import { ORDER_STATUS } from '@/types/order'
import type { Order } from '@/types/order'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const paying = ref(false)
const cancelling = ref(false)
const applyingCheckout = ref(false)
const deleting = ref(false)
const order = ref<Order | null>(null)

const statusText = computed(() => {
  if (!order.value) return ''
  if (order.value.status === 'COMPLETED') {
    return order.value.reviewed ? '已点评' : '待点评'
  }
  return ORDER_STATUS[order.value.status] || order.value.status
})

const canDelete = computed(() => {
  if (!order.value) return false
  const s = order.value.status
  return s === 'COMPLETED' || s === 'CANCELLED' || s === 'REFUNDED' || s === 'REFUNDING'
})

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

async function handleApplyCheckout() {
  if (!order.value) return
  try {
    await showConfirmDialog({
      title: '申请退房',
      message:
        '按剩余住宿晚数退款；当天18:00前申请可退当天房费，18:00后不含当天。提交后需商家确认并退款。',
    })
    applyingCheckout.value = true
    order.value = await applyCheckout(order.value.id)
    showSuccessToast('退房申请已提交，等待商家确认')
  } catch (e) {
    if (e !== 'cancel') {
      showFailToast(e instanceof Error ? e.message : '申请失败')
    }
  } finally {
    applyingCheckout.value = false
  }
}

async function handleDelete() {
  if (!order.value) return
  try {
    await showConfirmDialog({
      title: '删除订单',
      message: '删除后将不再显示在「我的订单」中，确定删除？',
    })
    deleting.value = true
    await deleteOrder(order.value.id)
    showSuccessToast('已删除')
    router.replace('/orders')
  } catch (e) {
    if (e !== 'cancel') {
      showFailToast(e instanceof Error ? e.message : '删除失败')
    }
  } finally {
    deleting.value = false
  }
}

function goReview() {
  if (!order.value) return
  router.push(`/orders/${order.value.id}/review`)
}

function roomLabel(index: number) {
  return `房间 ${index + 1}`
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
            <span :class="['status-text', order.status]">{{ statusText }}</span>
          </template>
        </van-cell>
        <van-cell title="酒店" :value="order.hotelName" />
        <van-cell title="房型" :value="order.roomTypeName" />
        <van-cell title="入住" :value="`${order.checkInDate} ~ ${order.checkOutDate}（${order.nights}晚）`" />
        <van-cell title="预订间数" :value="`${order.roomCount || order.guestCount || 1} 间`" />
        <van-cell title="联系电话" :value="order.guestPhone" />
        <van-cell title="总价" :value="`¥${order.totalAmount}`" />
        <van-cell v-if="order.discountAmount > 0" title="优惠" :value="`-¥${order.discountAmount}`" />
        <van-cell v-if="order.createdAt" title="下单时间" :value="order.createdAt" />
        <van-cell v-if="order.paidAt" title="支付时间" :value="order.paidAt" />
        <van-cell v-if="order.rejectReason" title="备注/原因" :value="order.rejectReason" />
        <van-cell v-if="order.checkoutApplyAt" title="退房申请时间" :value="order.checkoutApplyAt" />
        <van-cell v-if="order.refundAmount != null" title="预计退款" :value="`¥${order.refundAmount}`" />
        <van-cell v-if="order.refundPolicy" title="退款说明" :value="order.refundPolicy" />
      </van-cell-group>

      <van-cell-group inset title="入住人信息">
        <van-cell
          v-for="(guest, index) in order.guests"
          :key="guest.id || index"
          :title="roomLabel(index)"
          :label="[guest.name, guest.idCard].filter(Boolean).join(' · ')"
        />
      </van-cell-group>

      <div v-if="order.status === 'PENDING_PAYMENT'" class="actions">
        <van-button type="primary" block :loading="paying" @click="handlePay">模拟支付</van-button>
        <van-button block plain class="mt" :loading="cancelling" @click="handleCancel">取消订单</van-button>
      </div>
      <div v-else-if="order.status === 'PAID' || order.status === 'CONFIRMED'" class="actions">
        <van-button type="primary" block @click="router.push('/')">返回首页</van-button>
        <van-button block plain class="mt" type="warning" :loading="applyingCheckout" @click="handleApplyCheckout">
          申请退房
        </van-button>
      </div>
      <div v-else-if="order.status === 'CHECKOUT_PENDING'" class="actions">
        <van-notice-bar
          color="#ed6a0c"
          background="#fffbe8"
          :text="`退房申请处理中，预计退款 ¥${order.refundAmount ?? '-'}。${order.refundPolicy || ''}`"
        />
      </div>
      <div v-else-if="order.status === 'COMPLETED' && !order.reviewed" class="actions">
        <van-button type="primary" block @click="goReview">去评价</van-button>
        <van-button block plain type="danger" class="mt" :loading="deleting" @click="handleDelete">
          删除订单
        </van-button>
      </div>
      <div v-else-if="order.status === 'COMPLETED' && order.reviewed" class="actions">
        <van-notice-bar color="#1989fa" background="#ecf9ff" text="您已评价过该订单" />
        <van-button block plain type="danger" class="mt" :loading="deleting" @click="handleDelete">
          删除订单
        </van-button>
      </div>
      <div v-else-if="order.status === 'REFUNDED' || order.status === 'REFUNDING'" class="actions">
        <van-notice-bar
          color="#ed6a0c"
          background="#fffbe8"
          :text="`订单已退款${order.refundAmount != null ? ' ¥' + order.refundAmount : ''}${order.refundPolicy ? '。' + order.refundPolicy : ''}`"
        />
        <van-button block plain type="danger" class="mt" :loading="deleting" @click="handleDelete">
          删除订单
        </van-button>
      </div>
      <div v-else-if="order.status === 'CANCELLED'" class="actions">
        <van-notice-bar
          color="#969799"
          background="#f7f8fa"
          :text="`订单已取消${order.rejectReason ? '：' + order.rejectReason : ''}`"
        />
        <van-button block plain type="danger" class="mt" :loading="deleting" @click="handleDelete">
          删除订单
        </van-button>
      </div>
      <div v-else-if="canDelete" class="actions">
        <van-button block plain type="danger" :loading="deleting" @click="handleDelete">删除订单</van-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 16px;
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

.status-text.PAID,
.status-text.CONFIRMED,
.status-text.CHECKOUT_PENDING {
  color: #07c160;
}

.status-text.COMPLETED {
  color: #1989fa;
}

.status-text.CANCELLED,
.status-text.REFUNDED {
  color: #969799;
}
</style>
