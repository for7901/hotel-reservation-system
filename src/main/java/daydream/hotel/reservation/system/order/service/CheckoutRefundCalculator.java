package daydream.hotel.reservation.system.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/** 提前退房退款：按剩余可退晚数比例退款，当日 18:00 前申请可退当天房费。 */
public final class CheckoutRefundCalculator {

    private static final LocalTime CHECKOUT_CUTOFF = LocalTime.of(18, 0);

    private CheckoutRefundCalculator() {}

    public record CheckoutRefundResult(
            BigDecimal refundAmount,
            BigDecimal retainedAmount,
            int refundableNights,
            LocalDate refundFromDate,
            String policy) {}

    public static CheckoutRefundResult calculate(
            BigDecimal paidAmount,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int orderNights,
            LocalDateTime applyTime) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new CheckoutRefundResult(
                    BigDecimal.ZERO, BigDecimal.ZERO, 0, null, "未支付，无法退款");
        }
        if (orderNights <= 0) {
            return new CheckoutRefundResult(
                    BigDecimal.ZERO, paidAmount, 0, null, "订单晚数异常，无法退款");
        }

        LocalDate refundFrom =
                applyTime.toLocalTime().isBefore(CHECKOUT_CUTOFF)
                        ? applyTime.toLocalDate()
                        : applyTime.toLocalDate().plusDays(1);
        if (refundFrom.isBefore(checkInDate)) {
            refundFrom = checkInDate;
        }

        if (!refundFrom.isBefore(checkOutDate)) {
            return new CheckoutRefundResult(
                    BigDecimal.ZERO,
                    paidAmount,
                    0,
                    refundFrom,
                    "当前无可退晚数（当日18:00后申请不退当天房费）");
        }

        int refundableNights = (int) ChronoUnit.DAYS.between(refundFrom, checkOutDate);
        BigDecimal refundAmount =
                paidAmount
                        .multiply(BigDecimal.valueOf(refundableNights))
                        .divide(BigDecimal.valueOf(orderNights), 2, RoundingMode.HALF_UP);
        BigDecimal retainedAmount = paidAmount.subtract(refundAmount);

        String timeHint =
                applyTime.toLocalTime().isBefore(CHECKOUT_CUTOFF)
                        ? "18:00前申请，含当天房费"
                        : "18:00后申请，不含当天房费";
        String policy =
                String.format(
                        "退还剩余%d晚房费¥%s（%s）", refundableNights, refundAmount, timeHint);

        return new CheckoutRefundResult(
                refundAmount, retainedAmount, refundableNights, refundFrom, policy);
    }
}
