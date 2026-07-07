import request from '@/api/request'

export interface Banner {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
}

export function fetchBanners(): Promise<Banner[]> {
  return request.get('/banners') as Promise<Banner[]>
}
