import request from '@/api/request'
import type { LoginRequest, LoginResponse, RegisterRequest, UserProfile } from '@/types/auth'

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request.post('/auth/login', data) as Promise<LoginResponse>
}

export function register(data: RegisterRequest): Promise<LoginResponse> {
  return request.post('/auth/register', data) as Promise<LoginResponse>
}

export function getProfile(): Promise<UserProfile> {
  return request.get('/users/me') as Promise<UserProfile>
}

export function updateProfile(data: { nickname?: string; avatar?: string }): Promise<UserProfile> {
  return request.put('/users/me', data) as Promise<UserProfile>
}

export function changePassword(data: { oldPassword: string; newPassword: string }): Promise<void> {
  return request.put('/users/me/password', data) as Promise<void>
}
