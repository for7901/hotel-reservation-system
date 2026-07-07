package daydream.hotel.reservation.system.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RefundCalculatorTest {

    private static final BigDecimal PAID = new BigDecimal("600.00");

    @Test
    void unpaidOrderShouldNotRefund() {
        RefundCalculator.RefundResult result =
                RefundCalculator.calculate(
                        BigDecimal.ZERO, LocalDate.now().plusDays(3), LocalDateTime.now());
        assertEquals(0, result.refundAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void cancelOneDayBeforeCheckInShouldFullRefund() {
        LocalDate checkIn = LocalDate.of(2026, 7, 10);
        LocalDateTime cancelTime = LocalDateTime.of(2026, 7, 8, 10, 0);

        RefundCalculator.RefundResult result =
                RefundCalculator.calculate(PAID, checkIn, cancelTime);

        assertEquals(0, result.refundAmount().compareTo(PAID));
        assertEquals(0, result.feeAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void cancelOnCheckInDayShouldHalfRefund() {
        LocalDate checkIn = LocalDate.of(2026, 7, 10);
        LocalDateTime cancelTime = LocalDateTime.of(2026, 7, 10, 8, 0);

        RefundCalculator.RefundResult result =
                RefundCalculator.calculate(PAID, checkIn, cancelTime);

        assertEquals(0, result.refundAmount().compareTo(new BigDecimal("300.00")));
        assertEquals(0, result.feeAmount().compareTo(new BigDecimal("300.00")));
    }

    @Test
    void cancelAfterCheckInShouldNotRefund() {
        LocalDate checkIn = LocalDate.of(2026, 7, 10);
        LocalDateTime cancelTime = LocalDateTime.of(2026, 7, 11, 8, 0);

        RefundCalculator.RefundResult result =
                RefundCalculator.calculate(PAID, checkIn, cancelTime);

        assertEquals(0, result.refundAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.feeAmount().compareTo(PAID));
        assertTrue(result.policy().contains("不可退款"));
    }
}
