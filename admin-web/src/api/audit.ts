import request from '@/api/request'
import type { PageResult } from '@/types/hotel'

export interface AuditLog {
  id: number
  operatorId: number
  operatorName: string
  operatorRole: string
  action: string
  targetType: string
  targetId: number
  detail: string
  createdAt: string
}

export function fetchAuditLogs(params: {
  action?: string
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<AuditLog>> {
  return request.get('/admin/audit-logs', { params }) as Promise<PageResult<AuditLog>>
}
