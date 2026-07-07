export interface Province {
  id: number
  name: string
  code: string
}

export interface City {
  id: number
  provinceId: number
  name: string
  code: string
}

export interface HotelListItem {
  id: number
  name: string
  cityName: string
  address: string
  starRating: number
  coverImage: string
  minPrice: number
  score: number
}

export interface RoomType {
  id: number
  hotelId: number
  name: string
  bedType: string
  area: number
  maxGuests: number
  basePrice: number
  coverImage: string
  description: string
  breakfast: number
  status: number
}

export interface HotelDetail {
  id: number
  name: string
  cityName: string
  address: string
  starRating: number
  coverImage: string
  description: string
  minPrice: number
  score: number
  facilities: string[]
  roomTypes: RoomType[]
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface AdminHotel {
  id: number
  merchantId: number
  merchantName: string
  cityId: number
  provinceId?: number
  cityName: string
  name: string
  address: string
  starRating: number
  coverImage: string
  description?: string
  status: string
  rejectReason: string
  minPrice: number
  score: number
  facilities: string[]
  createdAt: string
}

export interface HotelSaveRequest {
  cityId: number
  name: string
  address: string
  starRating: number
  coverImage?: string
  description?: string
  facilities?: string[]
}

export interface RoomTypeSaveRequest {
  name: string
  bedType: string
  area?: number
  maxGuests?: number
  basePrice: number
  coverImage?: string
  description?: string
  breakfast?: number
}
