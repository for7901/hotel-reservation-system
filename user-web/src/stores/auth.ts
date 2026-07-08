import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'
import type { LoginRequest, RegisterRequest } from '@/types/auth'
import { clearAuth, getUser, setToken, setUser, type StoredUser } from '@/utils/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('hotel_user_token'))
  const user = ref<StoredUser | null>(getUser())

  const isLoggedIn = computed(() => !!token.value)

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    saveAuth(res, data.phone)
    return res
  }

  async function register(data: RegisterRequest) {
    const res = await registerApi(data)
    saveAuth(res, data.phone)
    return res
  }

  function saveAuth(
    res: { token: string; userId: number; phone: string; nickname: string; role: string },
    fullPhone?: string,
  ) {
    token.value = res.token
    const storedUser: StoredUser = {
      userId: res.userId,
      phone: fullPhone || res.phone,
      nickname: res.nickname,
      role: res.role,
    }
    user.value = storedUser
    setToken(res.token)
    setUser(storedUser)
  }

  function logout() {
    token.value = null
    user.value = null
    clearAuth()
  }

  return { token, user, isLoggedIn, login, register, logout }
})
