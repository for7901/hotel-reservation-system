import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'

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
      path: '/register',
      name: 'register',
      component: () => import('@/views/login/RegisterView.vue'),
      meta: { public: true },
    },
    {
      path: '/hotels',
      meta: { public: true },
      children: [
        {
          path: '',
          name: 'hotel-list',
          component: () => import('@/views/hotel/HotelListView.vue'),
        },
        {
          path: ':id',
          name: 'hotel-detail',
          component: () => import('@/views/hotel/HotelDetailView.vue'),
        },
        {
          path: ':hotelId/book',
          name: 'booking',
          component: () => import('@/views/order/BookingView.vue'),
        },
      ],
    },
    {
      path: '/',
      component: () => import('@/layouts/UserLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { public: true },
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/order/OrderListView.vue'),
        },
        {
          path: 'orders/:id',
          name: 'order-detail',
          component: () => import('@/views/order/OrderDetailView.vue'),
        },
        {
          path: 'orders/:id/review',
          name: 'order-review',
          component: () => import('@/views/order/ReviewSubmitView.vue'),
        },
        {
          path: 'favorites',
          name: 'favorites',
          component: () => import('@/views/favorite/FavoriteListView.vue'),
        },
        {
          path: 'coupons',
          name: 'coupons',
          component: () => import('@/views/coupon/CouponListView.vue'),
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/profile/ProfileView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const isPublic = to.matched.some((record) => record.meta.public)
  if (isPublic) {
    if (isLoggedIn() && (to.path === '/login' || to.path === '/register')) {
      return '/'
    }
    return true
  }
  if (!isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
