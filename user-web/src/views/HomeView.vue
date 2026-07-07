<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { fetchBanners } from '@/api/banner'
import { fetchCities } from '@/api/hotel'
import { defaultCheckIn, defaultCheckOut } from '@/utils/date'
import type { Banner } from '@/api/banner'
import type { City } from '@/types/hotel'

const router = useRouter()
const cities = ref<City[]>([])
const banners = ref<Banner[]>([])
const cityId = ref<number>()
const keyword = ref('')
const checkInDate = ref(defaultCheckIn())
const checkOutDate = ref(defaultCheckOut())
const showCityPicker = ref(false)

onMounted(async () => {
  try {
    const [cityList, bannerList] = await Promise.all([fetchCities(), fetchBanners()])
    cities.value = cityList
    banners.value = bannerList
    if (cities.value.length > 0) {
      cityId.value = cities.value[0].id
    }
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '加载失败')
  }
})

function onCityConfirm({ selectedOptions }: { selectedOptions: { text: string; value: number }[] }) {
  cityId.value = selectedOptions[0]?.value
  showCityPicker.value = false
}

function handleSearch() {
  if (!cityId.value) {
    showFailToast('请选择城市')
    return
  }
  if (checkOutDate.value <= checkInDate.value) {
    showFailToast('离店日期须晚于入住日期')
    return
  }
  router.push({
    path: '/hotels',
    query: {
      cityId: String(cityId.value),
      keyword: keyword.value || undefined,
      checkInDate: checkInDate.value,
      checkOutDate: checkOutDate.value,
    },
  })
}

function openBanner(linkUrl: string) {
  if (!linkUrl) return
  if (linkUrl.startsWith('http')) {
    window.open(linkUrl, '_blank')
    return
  }
  router.push(linkUrl)
}

const cityColumns = () =>
  cities.value.map((c) => ({ text: c.name, value: c.id }))

const selectedCityName = () =>
  cities.value.find((c) => c.id === cityId.value)?.name || '请选择城市'
</script>

<template>
  <div class="page">
    <van-nav-bar title="酒店预订" />
    <van-swipe v-if="banners.length" class="banner-swipe" :autoplay="4000" indicator-color="#fff">
      <van-swipe-item v-for="item in banners" :key="item.id" @click="openBanner(item.linkUrl)">
        <img :src="item.imageUrl" class="banner-img" :alt="item.title" />
        <div class="banner-title">{{ item.title }}</div>
      </van-swipe-item>
    </van-swipe>
    <div v-else class="banner">
      <h2>发现好酒店</h2>
      <p>搜索 · 预订 · 入住</p>
    </div>
    <van-cell-group inset>
      <van-field
        :model-value="selectedCityName()"
        label="城市"
        placeholder="请选择城市"
        readonly
        is-link
        @click="showCityPicker = true"
      />
      <van-field v-model="checkInDate" label="入住" type="date" />
      <van-field v-model="checkOutDate" label="离店" type="date" />
      <van-field v-model="keyword" label="关键词" placeholder="酒店名/地址" />
    </van-cell-group>
    <div class="action">
      <van-button type="primary" block @click="handleSearch">搜索酒店</van-button>
    </div>
    <van-popup v-model:show="showCityPicker" position="bottom">
      <van-picker
        :columns="cityColumns()"
        @confirm="onCityConfirm"
        @cancel="showCityPicker = false"
      />
    </van-popup>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.banner-swipe {
  margin: 16px;
  border-radius: 12px;
  overflow: hidden;
}

.banner-img {
  width: 100%;
  height: 160px;
  object-fit: cover;
  display: block;
}

.banner-title {
  position: absolute;
  left: 12px;
  bottom: 12px;
  color: #fff;
  font-weight: 600;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
}

.banner {
  margin: 16px;
  padding: 24px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1989fa, #07c160);
  color: #fff;
}

.banner h2 {
  margin: 0 0 8px;
  font-size: 22px;
}

.banner p {
  margin: 0;
  opacity: 0.9;
}

.action {
  margin: 24px 16px;
}
</style>
