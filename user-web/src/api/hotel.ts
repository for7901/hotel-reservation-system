import request from '@/api/request'
import type { City, HotelDetail, HotelListItem, PageResult, Province } from '@/types/hotel'

export function fetchProvinces(): Promise<Province[]> {
  return request.get('/hotels/provinces') as Promise<Province[]>
}

export function fetchCities(provinceId?: number): Promise<City[]> {
  return request.get('/hotels/cities', { params: provinceId ? { provinceId } : undefined }) as Promise<City[]>
}

export function searchHotels(params: {
  provinceId?: number
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
