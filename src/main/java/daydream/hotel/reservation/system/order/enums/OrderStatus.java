package daydream.hotel.reservation.system.order.enums;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED,
    REFUNDING,
    REFUNDED;

    public static boolean canPay(String status) {
        return PENDING_PAYMENT.name().equals(status);
    }

    public static boolean canCancel(String status) {
        return PENDING_PAYMENT.name().equals(status)
                || PAID.name().equals(status)
                || CONFIRMED.name().equals(status);
    }

    public static boolean canComplete(String status) {
        return CONFIRMED.name().equals(status);
    }
}
