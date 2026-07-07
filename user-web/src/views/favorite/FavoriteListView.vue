<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { fetchFavorites } from '@/api/favorite'
import type { Favorite } from '@/api/favorite'

const router = useRouter()
const loading = ref(true)
const favorites = ref<Favorite[]>([])

onMounted(async () => {
  try {
    favorites.value = await fetchFavorites()
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <van-nav-bar title="我的收藏" left-arrow @click-left="router.back()" />
    <van-skeleton v-if="loading" title :row="4" style="padding: 16px" />
    <template v-else>
      <div v-for="item in favorites" :key="item.id" class="card" @click="router.push(`/hotels/${item.hotelId}`)">
        <img :src="item.coverImage || 'https://via.placeholder.com/100x80'" class="cover" alt="" />
        <div class="info">
          <div class="name">{{ item.hotelName }}</div>
          <div class="meta">{{ item.cityName }} · {{ item.score }}分</div>
          <div class="price">¥{{ item.minPrice }}<span>起</span></div>
        </div>
      </div>
      <van-empty v-if="favorites.length === 0" description="暂无收藏" />
    </template>
  </div>
</template>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; padding-bottom: 60px; }
.card { display: flex; gap: 12px; margin: 12px 16px; padding: 12px; background: #fff; border-radius: 12px; }
.cover { width: 100px; height: 80px; border-radius: 8px; object-fit: cover; }
.name { font-weight: 600; }
.meta { margin-top: 4px; font-size: 12px; color: #969799; }
.price { margin-top: 8px; color: #ee0a24; font-weight: 600; }
.price span { font-size: 12px; font-weight: 400; }
</style>
