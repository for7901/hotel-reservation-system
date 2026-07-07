export interface InventoryItem {
  invDate: string
  price: number
  availableRooms: number
}

export interface OrderGuestInfo {
  id: number
  name: string
  phone: string
  idCard?: string
  sortOrder: number
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
  guestCount: number
  guestName: string
  guestPhone: string
  guests: OrderGuestInfo[]
  unitPrice: number
  totalAmount: number
  status: string
  rejectReason?: string | null
  paidAt: string | null
  cancelledAt: string | null
  checkoutApplyAt?: string | null
  refundAmount?: number | null
  refundPolicy?: string | null
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
  PAID: '待审核',
  CONFIRMED: '已确认',
  CHECKOUT_PENDING: '退房申请中',
  CHECKED_IN: '已入住',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
}
