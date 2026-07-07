<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const active = computed(() => {
  if (route.path.startsWith('/orders') || route.path.startsWith('/profile')) {
    return route.path.startsWith('/orders') ? 'orders' : 'profile'
  }
  return 'home'
})

function onChange(name: string | number) {
  if (name === 'home') router.push('/')
  if (name === 'orders') router.push('/orders')
  if (name === 'profile') router.push('/profile')
}
</script>

<template>
  <div class="layout">
    <main class="layout-content">
      <router-view />
    </main>
    <van-tabbar :model-value="active" @change="onChange">
      <van-tabbar-item name="home" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item name="orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item name="profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.layout-content {
  min-height: 100vh;
  padding-bottom: calc(var(--van-tabbar-height, 50px) + env(safe-area-inset-bottom, 0px));
  box-sizing: border-box;
}
</style>
