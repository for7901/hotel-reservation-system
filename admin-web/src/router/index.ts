import { createRouter, createWebHistory } from 'vue-router'
import { getUser, isLoggedIn } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'audit', name: 'hotel-audit', component: () => import('@/views/hotel/HotelAuditView.vue'), meta: { roles: ['ADMIN'] } },
        {
          path: 'hotels',
          name: 'merchant-hotels',
          component: () => import('@/views/hotel/MerchantHotelView.vue'),
          meta: { roles: ['MERCHANT'] },
        },
        {
          path: 'hotels/:hotelId/rooms',
          name: 'hotel-rooms',
          component: () => import('@/views/hotel/HotelRoomTypeView.vue'),
          meta: { roles: ['MERCHANT'] },
        },
        {
          path: 'hotels/:hotelId/rooms/:roomTypeId/inventory',
          name: 'hotel-inventory',
          component: () => import('@/views/hotel/HotelInventoryView.vue'),
          meta: { roles: ['MERCHANT'] },
        },
        { path: 'orders', name: 'orders', component: () => import('@/views/order/OrderListView.vue') },
        { path: 'users', name: 'users', component: () => import('@/views/user/UserListView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'reviews', name: 'reviews', component: () => import('@/views/review/ReviewListView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'banners', name: 'banners', component: () => import('@/views/banner/BannerListView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'coupons', name: 'coupons', component: () => import('@/views/coupon/CouponListView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'audit-logs', name: 'audit-logs', component: () => import('@/views/audit/AuditLogListView.vue'), meta: { roles: ['ADMIN'] } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.public) {
    if (to.path === '/login' && isLoggedIn()) return '/'
    return true
  }
  if (!isLoggedIn()) return { path: '/login', query: { redirect: to.fullPath } }

  const requiredRoles = to.matched
    .map((record) => record.meta.roles as string[] | undefined)
    .filter((roles): roles is string[] => !!roles)
    .flat()
  if (requiredRoles.length > 0) {
    const role = getUser()?.role
    if (!role || !requiredRoles.includes(role)) return '/'
  }
  return true
})

export default router
