export interface Availability {
  available: boolean
  nights: number
  totalPrice: number
  unitPrice: number
  availableRooms?: number
  message: string
}

export interface OrderGuest {
  name: string
  phone?: string
  idCard?: string
}

export interface OrderGuestInfo extends OrderGuest {
  id: number
  sortOrder: number
}

export interface OrderCreateRequest {
  hotelId: number
  roomTypeId: number
  checkInDate: string
  checkOutDate: string
  roomCount: number
  contactPhone: string
  guests: OrderGuest[]
  userCouponId?: number
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
  roomCount: number
  guestName: string
  guestPhone: string
  guests: OrderGuestInfo[]
  unitPrice: number
  totalAmount: number
  discountAmount: number
  status: string
  rejectReason?: string | null
  paidAt: string | null
  cancelledAt: string | null
  checkoutApplyAt?: string | null
  refundAmount?: number | null
  refundPolicy?: string | null
  createdAt: string
  reviewed?: boolean
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
  CHECKOUT_PENDING: '退款中',
  CHECKED_IN: '待出行',
  COMPLETED: '待点评',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
}
