import request from '@/api/request'

export interface DashboardStats {
  todayOrderCount: number
  todayRevenue: number
  pendingHotelCount: number
  pendingPaymentCount: number
  totalOrderCount: number
  totalUserCount: number
  hotelCount: number
}

export function fetchAdminStats(): Promise<DashboardStats> {
  return request.get('/admin/dashboard/stats') as Promise<DashboardStats>
}

export function fetchMerchantStats(): Promise<DashboardStats> {
  return request.get('/merchant/dashboard/stats') as Promise<DashboardStats>
}
