<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/hotels')) return '/hotels'
  if (route.path.startsWith('/audit')) return '/audit'
  if (route.path.startsWith('/orders')) return '/orders'
  if (route.path.startsWith('/users')) return '/users'
  if (route.path.startsWith('/reviews')) return '/reviews'
  if (route.path.startsWith('/banners')) return '/banners'
  if (route.path.startsWith('/coupons')) return '/coupons'
  if (route.path.startsWith('/audit-logs')) return '/audit-logs'
  return route.path
})

const menuItems = computed(() => {
  const items = [{ path: '/', title: '仪表盘' }]
  if (authStore.user?.role === 'ADMIN') {
    items.push({ path: '/audit', title: '酒店审核' })
    items.push({ path: '/orders', title: '订单管理' })
    items.push({ path: '/users', title: '用户管理' })
    items.push({ path: '/reviews', title: '评价管理' })
    items.push({ path: '/banners', title: 'Banner' })
    items.push({ path: '/coupons', title: '优惠券' })
    items.push({ path: '/audit-logs', title: '操作日志' })
  }
  if (authStore.user?.role === 'MERCHANT') {
    items.push({ path: '/hotels', title: '我的酒店' })
    items.push({ path: '/orders', title: '订单管理' })
  }
  return items
})

async function handleLogout() {
  await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">酒店管理系统</div>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ authStore.user?.nickname }}</span>
        <el-button type="danger" link @click="handleLogout">退出</el-button>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.aside {
  background: #304156;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.main {
  background: #f5f7fa;
}
</style>
