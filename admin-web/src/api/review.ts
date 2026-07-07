import request from '@/api/request'
import type { PageResult } from '@/types/hotel'

export interface Review {
  id: number
  userId: number
  hotelId: number
  orderId: number
  userNickname: string
  rating: number
  content: string
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

export function updateReviewVisibility(id: number, status: number): Promise<Review> {
  return request.put(`/admin/reviews/${id}/visibility`, null, { params: { status } }) as Promise<Review>
}
