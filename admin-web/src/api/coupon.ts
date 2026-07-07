import request from '@/api/request'

export interface Coupon {
  id: number
  name: string
  amount: number
  minAmount: number
  totalCount: number
  claimedCount: number
  startTime: string | null
  endTime: string | null
  status: number
}

export interface CouponSaveRequest {
  name: string
  amount: number
  minAmount?: number
  totalCount?: number
  startTime?: string
  endTime?: string
  status?: number
}

export function fetchCoupons(): Promise<Coupon[]> {
  return request.get('/admin/coupons') as Promise<Coupon[]>
}

export function createCoupon(data: CouponSaveRequest): Promise<Coupon> {
  return request.post('/admin/coupons', data) as Promise<Coupon>
}
