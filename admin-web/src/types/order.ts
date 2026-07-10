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
  roomCount?: number
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
  PAID: '待出行',
  CONFIRMED: '待出行',
  CHECKOUT_PENDING: '待出行',
  CHECKED_IN: '待出行',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '已退款',
  REFUNDED: '已退款',
}

/** 商家/管理端订单筛选：仅四种业务状态 */
export const ORDER_STATUS_FILTERS = [
  { value: 'PENDING_PAYMENT', label: '待支付' },
  { value: 'UPCOMING', label: '待出行' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'REFUNDED', label: '已退款' },
] as const
