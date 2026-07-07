import request from '@/api/request'
import type {
  AdminHotel,
  City,
  HotelSaveRequest,
  PageResult,
  RoomType,
  RoomTypeSaveRequest,
} from '@/types/hotel'

export function fetchCities(): Promise<City[]> {
  return request.get('/hotels/cities') as Promise<City[]>
}

export function fetchAdminHotels(params: {
  status?: string
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<AdminHotel>> {
  return request.get('/admin/hotels', { params }) as Promise<PageResult<AdminHotel>>
}

export function auditHotel(id: number, data: { status: string; rejectReason?: string }): Promise<void> {
  return request.put(`/admin/hotels/${id}/audit`, data) as Promise<void>
}

export function fetchMerchantHotels(params: { page?: number; size?: number }): Promise<PageResult<AdminHotel>> {
  return request.get('/merchant/hotels', { params }) as Promise<PageResult<AdminHotel>>
}

export function createHotel(data: HotelSaveRequest): Promise<AdminHotel> {
  return request.post('/merchant/hotels', data) as Promise<AdminHotel>
}

export function updateHotel(id: number, data: HotelSaveRequest): Promise<AdminHotel> {
  return request.put(`/merchant/hotels/${id}`, data) as Promise<AdminHotel>
}

export function deleteHotel(id: number): Promise<void> {
  return request.delete(`/merchant/hotels/${id}`) as Promise<void>
}

export function fetchRoomTypes(hotelId: number): Promise<RoomType[]> {
  return request.get(`/merchant/hotels/${hotelId}/room-types`) as Promise<RoomType[]>
}

export function createRoomType(hotelId: number, data: RoomTypeSaveRequest): Promise<RoomType> {
  return request.post(`/merchant/hotels/${hotelId}/room-types`, data) as Promise<RoomType>
}

export function updateRoomType(hotelId: number, roomTypeId: number, data: RoomTypeSaveRequest): Promise<RoomType> {
  return request.put(`/merchant/hotels/${hotelId}/room-types/${roomTypeId}`, data) as Promise<RoomType>
}

export function deleteRoomType(hotelId: number, roomTypeId: number): Promise<void> {
  return request.delete(`/merchant/hotels/${hotelId}/room-types/${roomTypeId}`) as Promise<void>
}
