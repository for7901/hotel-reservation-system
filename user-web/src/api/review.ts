import request from '@/api/request'
import type { PageResult } from '@/types/hotel'

export interface Review {
  id: number
  userNickname: string
  rating: number
  content: string
  merchantReply?: string | null
  replyAt?: string | null
  createdAt: string
}

export function fetchHotelReviews(hotelId: number, page = 1, size = 10): Promise<PageResult<Review>> {
  return request.get(`/reviews/hotels/${hotelId}`, { params: { page, size } }) as Promise<PageResult<Review>>
}

export function createReview(data: { orderId: number; rating: number; content?: string }): Promise<Review> {
  return request.post('/reviews', data) as Promise<Review>
}
