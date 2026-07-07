import request from '@/api/request'

export interface Favorite {
  id: number
  hotelId: number
  hotelName: string
  cityName: string
  coverImage: string
  minPrice: number
  score: number
}

export function fetchFavorites(): Promise<Favorite[]> {
  return request.get('/favorites') as Promise<Favorite[]>
}

export function fetchFavoriteStatus(hotelId: number): Promise<boolean> {
  return request.get(`/favorites/${hotelId}/status`) as Promise<boolean>
}

export function addFavorite(hotelId: number): Promise<void> {
  return request.post(`/favorites/${hotelId}`) as Promise<void>
}

export function removeFavorite(hotelId: number): Promise<void> {
  return request.delete(`/favorites/${hotelId}`) as Promise<void>
}
