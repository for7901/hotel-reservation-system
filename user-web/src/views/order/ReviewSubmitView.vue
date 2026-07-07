<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { fetchOrderDetail } from '@/api/order'
import { createReview } from '@/api/review'
import type { Order } from '@/types/order'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const submitting = ref(false)
const order = ref<Order | null>(null)
const rating = ref(5)
const content = ref('')

onMounted(async () => {
  try {
    order.value = await fetchOrderDetail(Number(route.params.id))
    if (order.value.status !== 'COMPLETED') {
      showFailToast('仅已完成订单可评价')
      router.replace(`/orders/${route.params.id}`)
    }
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '加载失败')
    router.back()
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (!order.value) return
  submitting.value = true
  try {
    await createReview({
      orderId: order.value.id,
      rating: rating.value,
      content: content.value.trim() || undefined,
    })
    showSuccessToast('评价成功')
    router.replace(`/hotels/${order.value.hotelId}`)
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="发表评价" left-arrow @click-left="router.back()" />
    <van-skeleton v-if="loading" title :row="4" style="padding: 16px" />
    <template v-else-if="order">
      <van-cell-group inset>
        <van-cell :title="order.hotelName" :label="order.roomTypeName" />
      </van-cell-group>
      <van-cell-group inset class="form">
        <van-cell title="评分">
          <template #value>
            <van-rate v-model="rating" />
          </template>
        </van-cell>
        <van-field
          v-model="content"
          rows="4"
          autosize
          type="textarea"
          maxlength="500"
          show-word-limit
          placeholder="分享入住体验（选填）"
        />
      </van-cell-group>
      <div class="action">
        <van-button type="primary" block :loading="submitting" @click="handleSubmit">提交评价</van-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
}

.form {
  margin-top: 12px;
}

.action {
  margin: 24px 16px;
}
</style>
