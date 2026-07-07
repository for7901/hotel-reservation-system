package daydream.hotel.reservation.system.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CheckoutRefundCalculatorTest {

    private static final BigDecimal PAID = new BigDecimal("600.00");
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 7, 10);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 7, 13);
    private static final int NIGHTS = 3;

    @Test
    void beforeCheckInFullRefund() {
        var result =
                CheckoutRefundCalculator.calculate(
                        PAID, CHECK_IN, CHECK_OUT, NIGHTS, LocalDateTime.of(2026, 7, 9, 10, 0));
        assertEquals(0, result.refundAmount().compareTo(PAID));
        assertEquals(3, result.refundableNights());
    }

    @Test
    void onCheckInBeforeSixPmIncludesTonight() {
        var result =
                CheckoutRefundCalculator.calculate(
                        PAID, CHECK_IN, CHECK_OUT, NIGHTS, LocalDateTime.of(2026, 7, 10, 17, 59));
        assertEquals(0, result.refundAmount().compareTo(PAID));
        assertEquals(3, result.refundableNights());
        assertEquals(CHECK_IN, result.refundFromDate());
    }

    @Test
    void onCheckInAfterSixPmExcludesTonight() {
        var result =
                CheckoutRefundCalculator.calculate(
                        PAID, CHECK_IN, CHECK_OUT, NIGHTS, LocalDateTime.of(2026, 7, 10, 18, 0));
        assertEquals(0, result.refundAmount().compareTo(new BigDecimal("400.00")));
        assertEquals(2, result.refundableNights());
        assertEquals(LocalDate.of(2026, 7, 11), result.refundFromDate());
    }

    @Test
    void midStayBeforeSixPm() {
        var result =
                CheckoutRefundCalculator.calculate(
                        PAID, CHECK_IN, CHECK_OUT, NIGHTS, LocalDateTime.of(2026, 7, 11, 12, 0));
        assertEquals(0, result.refundAmount().compareTo(new BigDecimal("400.00")));
        assertEquals(2, result.refundableNights());
    }

    @Test
    void lastNightAfterSixPmNoRefund() {
        var result =
                CheckoutRefundCalculator.calculate(
                        PAID, CHECK_IN, CHECK_OUT, NIGHTS, LocalDateTime.of(2026, 7, 12, 19, 0));
        assertEquals(0, result.refundAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.refundableNights());
    }
}
