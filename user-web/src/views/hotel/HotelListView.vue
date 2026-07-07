<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { searchHotels } from '@/api/hotel'
import type { HotelListItem } from '@/types/hotel'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const hotels = ref<HotelListItem[]>([])
const finished = ref(false)
const page = ref(1)
const initialized = ref(false)
const starRating = ref<number>()
const sortBy = ref('')
const showFilter = ref(false)

const searchSummary = computed(() => {
  const parts: string[] = []
  if (route.query.checkInDate && route.query.checkOutDate) {
    parts.push(`${route.query.checkInDate} ~ ${route.query.checkOutDate}`)
  }
  if (route.query.keyword) parts.push(route.query.keyword as string)
  return parts.join(' · ') || '全部酒店'
})

async function loadHotels(reset = false) {
  if (loading.value) return
  loading.value = true
  try {
    if (reset) {
      page.value = 1
      hotels.value = []
      finished.value = false
    }
    const cityId = route.query.cityId ? Number(route.query.cityId) : undefined
    const keyword = (route.query.keyword as string) || undefined
    const result = await searchHotels({
      cityId,
      keyword,
      starRating: starRating.value,
      sortBy: sortBy.value || undefined,
      page: page.value,
      size: 10,
    })
    hotels.value.push(...result.list)
    if (hotels.value.length >= result.total) {
      finished.value = true
    } else {
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

function goDetail(id: number) {
  router.push({
    path: `/hotels/${id}`,
    query: {
      checkInDate: route.query.checkInDate as string,
      checkOutDate: route.query.checkOutDate as string,
    },
  })
}

function applyFilter() {
  showFilter.value = false
  loadHotels(true)
}

watch(() => route.query, () => loadHotels(true))
</script>

<template>
  <div class="page">
    <van-nav-bar title="酒店列表" left-arrow @click-left="router.push('/')">
      <template #right>
        <span class="filter-btn" @click="showFilter = true">筛选</span>
      </template>
    </van-nav-bar>
    <div class="summary">{{ searchSummary }}</div>
    <van-list
      v-if="initialized"
      v-model:loading="loading"
      :finished="finished"
      finished-text="没有更多了"
      @load="loadHotels()"
    >
      <div v-for="hotel in hotels" :key="hotel.id" class="card" @click="goDetail(hotel.id)">
        <img :src="hotel.coverImage || 'https://via.placeholder.com/120x90?text=Hotel'" class="cover" alt="" />
        <div class="info">
          <div class="name">{{ hotel.name }}</div>
          <div class="meta">{{ hotel.cityName }} · {{ hotel.starRating }}星 · {{ hotel.score }}分</div>
          <div class="address">{{ hotel.address }}</div>
          <div class="price">¥{{ hotel.minPrice }}<span>起</span></div>
        </div>
      </div>
      <van-empty v-if="!loading && hotels.length === 0" description="暂无酒店" />
    </van-list>
    <van-skeleton v-else title :row="4" style="padding: 16px" />

    <van-action-sheet v-model:show="showFilter" title="筛选排序">
      <div class="filter-panel">
        <div class="filter-label">星级</div>
        <van-radio-group v-model="starRating" direction="horizontal">
          <van-radio :name="undefined">不限</van-radio>
          <van-radio :name="3">三星</van-radio>
          <van-radio :name="4">四星</van-radio>
          <van-radio :name="5">五星</van-radio>
        </van-radio-group>
        <div class="filter-label">排序</div>
        <van-radio-group v-model="sortBy">
          <van-radio name="">默认（评分优先）</van-radio>
          <van-radio name="PRICE_ASC">价格从低到高</van-radio>
          <van-radio name="PRICE_DESC">价格从高到低</van-radio>
        </van-radio-group>
        <van-button type="primary" block class="filter-apply" @click="applyFilter">应用</van-button>
      </div>
    </van-action-sheet>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 16px;
}

.summary {
  padding: 8px 16px;
  font-size: 13px;
  color: #969799;
}

.filter-btn {
  color: #1989fa;
  font-size: 14px;
}

.card {
  display: flex;
  gap: 12px;
  margin: 12px 16px;
  padding: 12px;
  background: #fff;
  border-radius: 12px;
}

.cover {
  width: 110px;
  height: 88px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 16px;
  font-weight: 600;
}

.meta,
.address {
  margin-top: 4px;
  font-size: 12px;
  color: #969799;
}

.price {
  margin-top: 8px;
  color: #ee0a24;
  font-size: 18px;
  font-weight: 600;
}

.price span {
  font-size: 12px;
  font-weight: 400;
}

.filter-panel {
  padding: 16px;
}

.filter-label {
  margin: 12px 0 8px;
  font-weight: 600;
}

.filter-apply {
  margin-top: 24px;
}
</style>
