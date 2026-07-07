<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { claimCoupon, fetchAvailableCoupons, fetchMyCoupons } from '@/api/coupon'
import type { Coupon, UserCoupon } from '@/api/coupon'

const router = useRouter()
const loading = ref(true)
const available = ref<Coupon[]>([])
const mine = ref<UserCoupon[]>([])
const activeTab = ref(0)

onMounted(async () => {
  try {
    ;[available.value, mine.value] = await Promise.all([fetchAvailableCoupons(), fetchMyCoupons()])
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
})

async function handleClaim(coupon: Coupon) {
  try {
    await claimCoupon(coupon.id)
    showSuccessToast('领取成功')
    ;[available.value, mine.value] = await Promise.all([fetchAvailableCoupons(), fetchMyCoupons()])
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '领取失败')
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="优惠券" left-arrow @click-left="router.back()" />
    <van-tabs v-model:active="activeTab">
      <van-tab title="可领取">
        <van-cell-group v-for="c in available" :key="c.id" inset class="card">
          <van-cell :title="c.name" :label="`满${c.minAmount}可用`">
            <template #value>
              <div class="right">
                <span class="amount">¥{{ c.amount }}</span>
                <van-button v-if="!c.claimed" size="small" type="primary" @click="handleClaim(c)">领取</van-button>
                <span v-else class="claimed">已领取</span>
              </div>
            </template>
          </van-cell>
        </van-cell-group>
        <van-empty v-if="!loading && available.length === 0" description="暂无可领优惠券" />
      </van-tab>
      <van-tab title="我的券">
        <van-cell-group v-for="c in mine" :key="c.id" inset class="card">
          <van-cell :title="c.name" :label="`满${c.minAmount}减${c.amount}`" :value="c.status === 'UNUSED' ? '未使用' : '已使用'" />
        </van-cell-group>
        <van-empty v-if="!loading && mine.length === 0" description="暂无优惠券" />
      </van-tab>
    </van-tabs>
  </div>
</template>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; }
.card { margin-top: 12px; }
.right { display: flex; align-items: center; gap: 8px; }
.amount { color: #ee0a24; font-weight: 600; }
.claimed { color: #969799; font-size: 12px; }
</style>
