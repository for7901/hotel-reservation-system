import request from '@/api/request'
import type { PageResult } from '@/types/hotel'

export interface Review {
  id: number
  userId: number
  hotelId: number
  orderId: number
  hotelName?: string
  userNickname: string
  rating: number
  content: string
  merchantReply?: string | null
  replyAt?: string | null
  status: number
  createdAt: string
}

export function fetchReviews(params: {
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<Review>> {
  return request.get('/admin/reviews', { params }) as Promise<PageResult<Review>>
}

export function fetchMerchantReviews(params: {
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<Review>> {
  return request.get('/merchant/reviews', { params }) as Promise<PageResult<Review>>
}

export function updateReviewVisibility(id: number, status: number): Promise<Review> {
  return request.put(`/admin/reviews/${id}/visibility`, null, { params: { status } }) as Promise<Review>
}

export function replyReview(id: number, content: string): Promise<Review> {
  return request.put(`/merchant/reviews/${id}/reply`, { content }) as Promise<Review>
}
