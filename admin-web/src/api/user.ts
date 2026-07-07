import request from '@/api/request'
import type { PageResult } from '@/types/hotel'

export interface AdminUser {
  id: number
  phone: string
  nickname: string
  role: string
  status: number
  createdAt: string
}

export function fetchUsers(params: {
  role?: string
  status?: number
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<AdminUser>> {
  return request.get('/admin/users', { params }) as Promise<PageResult<AdminUser>>
}

export function updateUserStatus(id: number, status: number): Promise<AdminUser> {
  return request.put(`/admin/users/${id}/status`, { status }) as Promise<AdminUser>
}
