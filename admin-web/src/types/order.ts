export interface InventoryItem {
  invDate: string
  price: number
  availableRooms: number
}

export interface Order {
  id: number
  orderNo: string
  hotelId: number
  roomTypeId: number
  hotelName: string
  roomTypeName: string
  checkInDate: string
  checkOutDate: string
  nights: number
  guestName: string
  guestPhone: string
  unitPrice: number
  totalAmount: number
  status: string
  paidAt: string | null
  cancelledAt: string | null
  createdAt: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export const ORDER_STATUS: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  CONFIRMED: '已确认',
  CHECKED_IN: '已入住',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
}
