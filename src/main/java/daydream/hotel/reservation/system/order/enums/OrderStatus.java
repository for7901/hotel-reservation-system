package daydream.hotel.reservation.system.order.enums;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    CHECKED_IN,
    CHECKOUT_PENDING,
    COMPLETED,
    CANCELLED,
    REFUNDING,
    REFUNDED;

    public static boolean canPay(String status) {
        return PENDING_PAYMENT.name().equals(status);
    }

    public static boolean canCancel(String status) {
        return PENDING_PAYMENT.name().equals(status);
    }

    public static boolean canApplyCheckout(String status) {
        return PAID.name().equals(status) || CONFIRMED.name().equals(status);
    }

    public static boolean canApproveCheckout(String status) {
        return CHECKOUT_PENDING.name().equals(status);
    }

    /** 已支付（含历史已确认）可确认完成入住 */
    public static boolean canComplete(String status) {
        return PAID.name().equals(status) || CONFIRMED.name().equals(status);
    }

    /** 用户可删除：待点评/已点评/已取消/已退款 */
    public static boolean canUserDelete(String status) {
        return COMPLETED.name().equals(status)
                || CANCELLED.name().equals(status)
                || REFUNDED.name().equals(status)
                || REFUNDING.name().equals(status);
    }

    /**
     * @deprecated 支付后无需商家审核入住人，保留兼容旧数据
     */
    @Deprecated
    public static boolean canReviewGuests(String status) {
        return PAID.name().equals(status);
    }
}
