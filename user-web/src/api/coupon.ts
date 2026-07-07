import request from '@/api/request'

export interface Coupon {
  id: number
  name: string
  amount: number
  minAmount: number
  claimed?: boolean
}

export interface UserCoupon {
  id: number
  couponId: number
  name: string
  amount: number
  minAmount: number
  status: string
  endTime: string | null
}

export function fetchAvailableCoupons(): Promise<Coupon[]> {
  return request.get('/coupons/available') as Promise<Coupon[]>
}

export function fetchMyCoupons(): Promise<UserCoupon[]> {
  return request.get('/coupons/my') as Promise<UserCoupon[]>
}

export function claimCoupon(id: number): Promise<UserCoupon> {
  return request.post(`/coupons/${id}/claim`) as Promise<UserCoupon>
}
