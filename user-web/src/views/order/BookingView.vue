<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { getProfile } from '@/api/auth'
import { fetchMyCoupons } from '@/api/coupon'
import { fetchHotelDetail } from '@/api/hotel'
import { checkAvailability, createOrder } from '@/api/order'
import { isLoggedIn } from '@/utils/auth'
import { defaultCheckIn, defaultCheckOut } from '@/utils/date'
import { isValidPhone } from '@/utils/error'
import type { UserCoupon } from '@/api/coupon'
import type { Availability } from '@/types/order'
import type { HotelDetail, RoomType } from '@/types/hotel'

const route = useRoute()
const router = useRouter()
const hotelId = Number(route.params.hotelId)
const roomTypeId = Number(route.query.roomTypeId)
const loading = ref(true)
const submitting = ref(false)
const hotel = ref<HotelDetail | null>(null)
const selectedRoom = ref<RoomType | null>(null)
const availability = ref<Availability | null>(null)
const coupons = ref<UserCoupon[]>([])
const selectedCouponId = ref<number>()
const showCouponPicker = ref(false)

const form = reactive({
  checkInDate: (route.query.checkInDate as string) || defaultCheckIn(),
  checkOutDate: (route.query.checkOutDate as string) || defaultCheckOut(),
  guestName: '',
  guestPhone: '',
})

const nights = computed(() => {
  if (!form.checkInDate || !form.checkOutDate) return 0
  const start = new Date(form.checkInDate)
  const end = new Date(form.checkOutDate)
  return Math.max(0, Math.round((end.getTime() - start.getTime()) / 86400000))
})

const selectedCoupon = computed(() => coupons.value.find((c) => c.id === selectedCouponId.value))

const payableAmount = computed(() => {
  const total = availability.value?.totalPrice ?? 0
  if (!selectedCoupon.value) return total
  if (total < selectedCoupon.value.minAmount) return total
  return Math.max(0, total - selectedCoupon.value.amount)
})

const canSubmit = computed(() => availability.value?.available === true && !!form.guestName && isValidPhone(form.guestPhone))

const couponColumns = computed(() =>
  coupons.value
    .filter((c) => c.status === 'UNUSED')
    .map((c) => ({
      text: `${c.name}（满${c.minAmount}减${c.amount}）`,
      value: c.id,
      disabled: (availability.value?.totalPrice ?? 0) < c.minAmount,
    }))
)

const selectedCouponLabel = computed(() => {
  if (!selectedCoupon.value) return '不使用优惠券'
  if ((availability.value?.totalPrice ?? 0) < selectedCoupon.value.minAmount) {
    return `${selectedCoupon.value.name}（未达门槛）`
  }
  return `${selectedCoupon.value.name}（-¥${selectedCoupon.value.amount}）`
})

onMounted(async () => {
  try {
    hotel.value = await fetchHotelDetail(hotelId)
    selectedRoom.value = hotel.value.roomTypes.find((r) => r.id === roomTypeId) || hotel.value.roomTypes[0] || null
    if (isLoggedIn()) {
      try {
        const [profile, myCoupons] = await Promise.all([getProfile(), fetchMyCoupons()])
        if (!form.guestName) form.guestName = profile.nickname || ''
        if (!form.guestPhone) form.guestPhone = profile.phone || ''
        coupons.value = myCoupons
      } catch {
        // ignore
      }
    }
    await refreshPrice()
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
})

async function refreshPrice() {
  if (!selectedRoom.value || !form.checkInDate || !form.checkOutDate) return
  if (form.checkOutDate <= form.checkInDate) {
    availability.value = { available: false, nights: 0, totalPrice: 0, unitPrice: 0, message: '离店日期须晚于入住日期' }
    return
  }
  try {
    availability.value = await checkAvailability(hotelId, {
      roomTypeId: selectedRoom.value.id,
      checkInDate: form.checkInDate,
      checkOutDate: form.checkOutDate,
    })
  } catch (e) {
    availability.value = { available: false, nights: 0, totalPrice: 0, unitPrice: 0, message: e instanceof Error ? e.message : '价格查询失败' }
  }
}

function onCouponConfirm({ selectedOptions }: { selectedOptions: { text: string; value: number }[] }) {
  const val = selectedOptions[0]?.value
  if (!val) {
    clearCoupon()
    return
  }
  selectedCouponId.value = val
  showCouponPicker.value = false
}

function clearCoupon() {
  selectedCouponId.value = undefined
  showCouponPicker.value = false
}

async function handleSubmit() {
  if (!isLoggedIn()) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!selectedRoom.value) {
    showFailToast('请选择房型')
    return
  }
  if (!form.guestName.trim()) {
    showFailToast('请输入入住人姓名')
    return
  }
  if (!isValidPhone(form.guestPhone)) {
    showFailToast('请输入正确的手机号')
    return
  }
  if (!canSubmit.value) {
    showFailToast(availability.value?.message || '当前不可预订')
    return
  }
  const coupon = selectedCoupon.value
  if (coupon && (availability.value?.totalPrice ?? 0) < coupon.minAmount) {
    showFailToast('订单金额未达优惠券使用门槛')
    return
  }
  submitting.value = true
  try {
    const order = await createOrder({
      hotelId,
      roomTypeId: selectedRoom.value.id,
      checkInDate: form.checkInDate,
      checkOutDate: form.checkOutDate,
      guestName: form.guestName.trim(),
      guestPhone: form.guestPhone,
      userCouponId: selectedCouponId.value,
    })
    showSuccessToast('下单成功')
    router.replace(`/orders/${order.id}`)
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '下单失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="填写订单" left-arrow @click-left="router.back()" />
    <van-skeleton v-if="loading" title :row="5" style="padding: 16px" />
    <template v-else-if="hotel && selectedRoom">
      <van-cell-group inset title="酒店信息">
        <van-cell :title="hotel.name" :label="selectedRoom.name" />
      </van-cell-group>
      <van-cell-group inset title="入住信息">
        <van-field v-model="form.checkInDate" label="入住" type="date" @change="refreshPrice" />
        <van-field v-model="form.checkOutDate" label="离店" type="date" @change="refreshPrice" />
        <van-cell title="晚数" :value="`${nights} 晚`" />
        <van-cell title="原价" :value="availability?.available ? `¥${availability.totalPrice}` : availability?.message" />
        <van-cell
          title="优惠券"
          :value="selectedCouponLabel"
          is-link
          @click="showCouponPicker = true"
        />
        <van-cell v-if="selectedCoupon && payableAmount < (availability?.totalPrice ?? 0)" title="实付" :value="`¥${payableAmount}`" />
        <van-field v-model="form.guestName" label="入住人" placeholder="请输入姓名" />
        <van-field v-model="form.guestPhone" label="手机号" type="tel" maxlength="11" placeholder="请输入手机号" />
      </van-cell-group>
      <div class="policy">
        <p>退改说明：入住前 24 小时可免费取消；超时取消可能收取首晚房费。</p>
      </div>
      <div class="action">
        <van-button type="primary" block :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
          提交订单
        </van-button>
      </div>
      <van-popup v-model:show="showCouponPicker" position="bottom">
        <van-picker
          :columns="[{ text: '不使用优惠券', value: 0 }, ...couponColumns]"
          @confirm="onCouponConfirm"
          @cancel="showCouponPicker = false"
        />
      </van-popup>
    </template>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 24px;
}

.policy {
  margin: 12px 16px;
  padding: 12px;
  background: #fff7e6;
  border-radius: 8px;
  font-size: 12px;
  color: #969799;
}

.policy p {
  margin: 0;
}

.action {
  margin: 24px 16px;
}
</style>
