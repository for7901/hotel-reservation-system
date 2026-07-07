import request from '@/api/request'
import type { PageResult } from '@/types/hotel'
import type { InventoryItem, Order } from '@/types/order'

export function fetchInventory(
  hotelId: number,
  roomTypeId: number,
  startDate: string,
  endDate: string
): Promise<InventoryItem[]> {
  return request.get(`/merchant/hotels/${hotelId}/room-types/${roomTypeId}/inventory`, {
    params: { startDate, endDate },
  }) as Promise<InventoryItem[]>
}

export function saveInventory(
  hotelId: number,
  roomTypeId: number,
  items: InventoryItem[]
): Promise<void> {
  return request.put(`/merchant/hotels/${hotelId}/room-types/${roomTypeId}/inventory`, items) as Promise<void>
}

export function fetchMerchantOrders(params: {
  status?: string
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<Order>> {
  return request.get('/merchant/orders', { params }) as Promise<PageResult<Order>>
}

export function fetchAdminOrders(params: {
  status?: string
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<Order>> {
  return request.get('/admin/orders', { params }) as Promise<PageResult<Order>>
}

export function fetchOrderDetail(id: number): Promise<Order> {
  return request.get(`/orders/${id}`) as Promise<Order>
}
