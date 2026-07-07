import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'
import type { LoginRequest } from '@/types/auth'
import { clearAuth, getUser, setToken, setUser, type StoredUser } from '@/utils/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('hotel_admin_token'))
  const user = ref<StoredUser | null>(getUser())

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN' || user.value?.role === 'MERCHANT')

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    if (res.role !== 'ADMIN' && res.role !== 'MERCHANT') {
      throw new Error('无管理端访问权限')
    }
    token.value = res.token
    const storedUser: StoredUser = {
      userId: res.userId,
      phone: res.phone,
      nickname: res.nickname,
      role: res.role,
    }
    user.value = storedUser
    setToken(res.token)
    setUser(storedUser)
    return res
  }

  function logout() {
    token.value = null
    user.value = null
    clearAuth()
  }

  return { token, user, isLoggedIn, isAdmin, login, logout }
})
