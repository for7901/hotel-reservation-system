import request from '@/api/request'

export interface Banner {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  sortOrder: number
  status: number
}

export interface BannerSaveRequest {
  title: string
  imageUrl: string
  linkUrl?: string
  sortOrder?: number
  status?: number
}

export function fetchBanners(): Promise<Banner[]> {
  return request.get('/admin/banners') as Promise<Banner[]>
}

export function createBanner(data: BannerSaveRequest): Promise<Banner> {
  return request.post('/admin/banners', data) as Promise<Banner>
}

export function updateBanner(id: number, data: BannerSaveRequest): Promise<Banner> {
  return request.put(`/admin/banners/${id}`, data) as Promise<Banner>
}

export function deleteBanner(id: number): Promise<void> {
  return request.delete(`/admin/banners/${id}`) as Promise<void>
}
