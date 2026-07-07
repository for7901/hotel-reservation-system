<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import { cancelOrder, completeOrder, fetchOrderDetail, payOrder, applyCheckout } from '@/api/order'
import { ORDER_STATUS } from '@/types/order'
import type { Order } from '@/types/order'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const paying = ref(false)
const cancelling = ref(false)
const applyingCheckout = ref(false)
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
    showSuccessToast('支付成功，等待酒店审核入住人信息')
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
        <van-cell title="入住人数" :value="`${order.guestCount} 人`" />
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
          :title="`入住人 ${index + 1}`"
          :label="[guest.name, guest.phone, guest.idCard].filter(Boolean).join(' · ')"
        />
      </van-cell-group>

      <div v-if="order.status === 'PENDING_PAYMENT'" class="actions">
        <van-button type="primary" block :loading="paying" @click="handlePay">模拟支付</van-button>
        <van-button block plain class="mt" :loading="cancelling" @click="handleCancel">取消订单</van-button>
      </div>
      <div v-else-if="order.status === 'PAID'" class="actions">
        <van-notice-bar text="酒店正在审核入住人信息，请耐心等待" />
      </div>
      <div v-else-if="order.status === 'CONFIRMED'" class="actions">
        <van-button type="primary" block :loading="completing" @click="handleComplete">确认完成</van-button>
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
      <div v-else-if="order.status === 'COMPLETED'" class="actions">
        <van-button type="primary" block @click="goReview">去评价</van-button>
      </div>
      <div v-else-if="order.status === 'REFUNDED'" class="actions">
        <van-notice-bar color="#ed6a0c" background="#fffbe8" :text="`订单已退款${order.rejectReason ? '：' + order.rejectReason : ''}`" />
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

.status-text.PAID {
  color: #ff976a;
}

.status-text.CONFIRMED {
  color: #07c160;
}

.status-text.CHECKOUT_PENDING {
  color: #ff976a;
}

.status-text.COMPLETED {
  color: #1989fa;
}

.status-text.REFUNDED {
  color: #969799;
}
</style>
