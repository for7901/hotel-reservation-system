package daydream.hotel.reservation.system.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** 订单退款金额计算：入住前 1 天及以上全额退，入住当天退 50%，已过入住日不退。 */
public final class RefundCalculator {

    private static final BigDecimal HALF = new BigDecimal("0.50");

    private RefundCalculator() {}

    public record RefundResult(BigDecimal refundAmount, BigDecimal feeAmount, String policy) {}

    public static RefundResult calculate(
            BigDecimal paidAmount, LocalDate checkInDate, LocalDateTime cancelTime) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new RefundResult(BigDecimal.ZERO, BigDecimal.ZERO, "未支付，无需退款");
        }
        long daysUntilCheckIn = ChronoUnit.DAYS.between(cancelTime.toLocalDate(), checkInDate);
        if (daysUntilCheckIn >= 1) {
            return new RefundResult(paidAmount, BigDecimal.ZERO, "入住前1天及以上免费取消");
        }
        if (daysUntilCheckIn == 0) {
            BigDecimal fee = paidAmount.multiply(HALF).setScale(2, RoundingMode.HALF_UP);
            return new RefundResult(paidAmount.subtract(fee), fee, "入住当日取消扣50%手续费");
        }
        return new RefundResult(BigDecimal.ZERO, paidAmount, "已过入住日，不可退款");
    }
}
