<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { addFavorite, fetchFavoriteStatus, removeFavorite } from '@/api/favorite'
import { fetchHotelDetail } from '@/api/hotel'
import { fetchHotelReviews } from '@/api/review'
import { isLoggedIn } from '@/utils/auth'
import type { Review } from '@/api/review'
import type { HotelDetail } from '@/types/hotel'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const hotel = ref<HotelDetail | null>(null)
const favorited = ref(false)
const favoriteLoading = ref(false)
const reviews = ref<Review[]>([])

onMounted(async () => {
  const hotelId = Number(route.params.id)
  try {
    const [detail, reviewPage] = await Promise.all([
      fetchHotelDetail(hotelId),
      fetchHotelReviews(hotelId, 1, 5),
    ])
    hotel.value = detail
    reviews.value = reviewPage.list
    if (isLoggedIn()) {
      try {
        favorited.value = await fetchFavoriteStatus(hotelId)
      } catch {
        // ignore
      }
    }
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
})

function handleBook(roomTypeId: number) {
  const query: Record<string, string> = { roomTypeId: String(roomTypeId) }
  if (route.query.checkInDate) query.checkInDate = route.query.checkInDate as string
  if (route.query.checkOutDate) query.checkOutDate = route.query.checkOutDate as string
  const bookPath = `/hotels/${route.params.id}/book?${new URLSearchParams(query).toString()}`
  if (!isLoggedIn()) {
    router.push({ path: '/login', query: { redirect: bookPath } })
    return
  }
  router.push(`/hotels/${route.params.id}/book?${new URLSearchParams(query).toString()}`)
}

async function toggleFavorite() {
  if (!isLoggedIn()) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  favoriteLoading.value = true
  try {
    const hotelId = Number(route.params.id)
    if (favorited.value) {
      await removeFavorite(hotelId)
      favorited.value = false
      showSuccessToast('已取消收藏')
    } else {
      await addFavorite(hotelId)
      favorited.value = true
      showSuccessToast('收藏成功')
    }
  } catch (e) {
    showFailToast(e instanceof Error ? e.message : '操作失败')
  } finally {
    favoriteLoading.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="酒店详情" left-arrow @click-left="router.back()">
      <template #right>
        <van-icon
          :name="favorited ? 'star' : 'star-o'"
          :color="favorited ? '#ffb300' : '#323233'"
          size="20"
          :class="{ loading: favoriteLoading }"
          @click="toggleFavorite"
        />
      </template>
    </van-nav-bar>
    <van-skeleton v-if="loading" title :row="6" style="padding: 16px" />
    <template v-else-if="hotel">
      <img :src="hotel.coverImage" class="hero" alt="" />
      <div class="content">
        <h2>{{ hotel.name }}</h2>
        <p class="meta">{{ hotel.cityName }} · {{ hotel.starRating }}星 · {{ hotel.score }}分</p>
        <p class="address">{{ hotel.address }}</p>
        <p class="desc">{{ hotel.description }}</p>
        <div class="tags">
          <van-tag v-for="item in hotel.facilities" :key="item" plain type="primary">{{ item }}</van-tag>
        </div>
      </div>
      <van-cell-group inset title="可选房型">
        <van-cell
          v-for="room in hotel.roomTypes"
          :key="room.id"
          :title="room.name"
          :label="`${room.bedType} · ${room.area || '-'}㎡ · 可住${room.maxGuests}人`"
          is-link
          @click="handleBook(room.id)"
        >
          <template #value>
            <div class="room-price">¥{{ room.basePrice }}</div>
          </template>
        </van-cell>
      </van-cell-group>
      <van-cell-group inset title="住客评价">
        <van-cell v-for="r in reviews" :key="r.id" :title="r.userNickname || '匿名用户'" :label="r.content || '暂无文字评价'">
          <template #value>
            <van-rate :model-value="r.rating" readonly size="14" />
          </template>
        </van-cell>
        <van-empty v-if="reviews.length === 0" description="暂无评价" image-size="60" />
      </van-cell-group>
      <div class="footer">
        <div class="from-price">¥{{ hotel.minPrice }}<span>起</span></div>
        <van-button type="primary" @click="hotel.roomTypes[0] && handleBook(hotel.roomTypes[0].id)">立即预订</van-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 72px;
}

.loading {
  opacity: 0.5;
}

.hero {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.content {
  padding: 16px;
  background: #fff;
}

.content h2 {
  margin: 0;
  font-size: 20px;
}

.meta,
.address,
.desc {
  margin: 8px 0 0;
  color: #646566;
  font-size: 14px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.room-price {
  color: #ee0a24;
  font-weight: 600;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}

.from-price {
  color: #ee0a24;
  font-size: 22px;
  font-weight: 600;
}

.from-price span {
  font-size: 12px;
}
</style>
