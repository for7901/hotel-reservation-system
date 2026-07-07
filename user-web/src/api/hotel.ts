import request from '@/api/request'
import type { City, HotelDetail, HotelListItem, PageResult } from '@/types/hotel'

export function fetchCities(): Promise<City[]> {
  return request.get('/hotels/cities') as Promise<City[]>
}

export function searchHotels(params: {
  cityId?: number
  keyword?: string
  starRating?: number
  minPrice?: number
  maxPrice?: number
  minScore?: number
  sortBy?: string
  page?: number
  size?: number
}): Promise<PageResult<HotelListItem>> {
  return request.get('/hotels/search', { params }) as Promise<PageResult<HotelListItem>>
}

export function fetchHotelDetail(id: number): Promise<HotelDetail> {
  return request.get(`/hotels/${id}`) as Promise<HotelDetail>
}
