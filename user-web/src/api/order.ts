import request from '@/api/request'
import type { PageResult } from '@/types/hotel'
import type { Availability, Order, OrderCreateRequest } from '@/types/order'

export function checkAvailability(
  hotelId: number,
  params: { roomTypeId: number; checkInDate: string; checkOutDate: string }
): Promise<Availability> {
  return request.get(`/hotels/${hotelId}/availability`, { params }) as Promise<Availability>
}

export function createOrder(data: OrderCreateRequest): Promise<Order> {
  return request.post('/orders', data) as Promise<Order>
}

export function fetchMyOrders(params: { status?: string; page?: number; size?: number }): Promise<PageResult<Order>> {
  return request.get('/orders/my', { params }) as Promise<PageResult<Order>>
}

export function fetchOrderDetail(id: number): Promise<Order> {
  return request.get(`/orders/${id}`) as Promise<Order>
}

export function payOrder(id: number): Promise<Order> {
  return request.post(`/orders/${id}/pay`) as Promise<Order>
}

export function cancelOrder(id: number): Promise<Order> {
  return request.post(`/orders/${id}/cancel`) as Promise<Order>
}

export function applyCheckout(id: number): Promise<Order> {
  return request.post(`/orders/${id}/apply-checkout`) as Promise<Order>
}

export function completeOrder(id: number): Promise<Order> {
  return request.post(`/orders/${id}/complete`) as Promise<Order>
}
