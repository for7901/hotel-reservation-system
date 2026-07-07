import request from '@/api/request'
import type { LoginRequest, LoginResponse } from '@/types/auth'

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request.post('/auth/login', data) as Promise<LoginResponse>
}
