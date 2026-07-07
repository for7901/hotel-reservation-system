import axios from 'axios'
import type { ApiResult } from '@/types/api'
import { clearAuth, getToken } from '@/utils/auth'
import { extractErrorMessage } from '@/utils/error'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>
    if (result.code === 401) {
      clearAuth()
      router.push('/login')
      return Promise.reject(new Error(result.message || '登录已过期'))
    }
    if (result.code !== 0) {
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return result.data
  },
  (error) => {
    if (error.response?.status === 401) {
      clearAuth()
      router.push('/login')
    }
    return Promise.reject(new Error(extractErrorMessage(error)))
  }
)

export default request
