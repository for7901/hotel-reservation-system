import type { AxiosError } from 'axios'
import type { ApiResult } from '@/types/api'

const PHONE_PATTERN = /^1[3-9]\d{9}$/
const MASKED_PHONE_PATTERN = /^1[3-9]\d\*{4}\d{4}$/

export function isValidPhone(phone: string): boolean {
  return PHONE_PATTERN.test(phone)
}

export function isMaskedPhone(phone: string): boolean {
  return MASKED_PHONE_PATTERN.test(phone)
}

export function extractErrorMessage(error: unknown, fallback = '请求失败'): string {
  if (error instanceof Error && error.message && !error.message.startsWith('Request failed with status code')) {
    return error.message
  }
  const axiosError = error as AxiosError<ApiResult<unknown>>
  const message = axiosError.response?.data?.message
  if (message) {
    return message
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}
