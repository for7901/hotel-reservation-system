package daydream.hotel.reservation.system.dashboard.dto;

import java.math.BigDecimal;

public class DashboardStatsVO {

    private long todayOrderCount;
    private BigDecimal todayRevenue;
    private long pendingHotelCount;
    private long pendingPaymentCount;
    private long totalOrderCount;
    private long totalUserCount;
    private long hotelCount;

    public long getTodayOrderCount() {
        return todayOrderCount;
    }

    public void setTodayOrderCount(long todayOrderCount) {
        this.todayOrderCount = todayOrderCount;
    }

    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(BigDecimal todayRevenue) {
        this.todayRevenue = todayRevenue;
    }

    public long getPendingHotelCount() {
        return pendingHotelCount;
    }

    public void setPendingHotelCount(long pendingHotelCount) {
        this.pendingHotelCount = pendingHotelCount;
    }

    public long getPendingPaymentCount() {
        return pendingPaymentCount;
    }

    public void setPendingPaymentCount(long pendingPaymentCount) {
        this.pendingPaymentCount = pendingPaymentCount;
    }

    public long getTotalOrderCount() {
        return totalOrderCount;
    }

    public void setTotalOrderCount(long totalOrderCount) {
        this.totalOrderCount = totalOrderCount;
    }

    public long getTotalUserCount() {
        return totalUserCount;
    }

    public void setTotalUserCount(long totalUserCount) {
        this.totalUserCount = totalUserCount;
    }

    public long getHotelCount() {
        return hotelCount;
    }

    public void setHotelCount(long hotelCount) {
        this.hotelCount = hotelCount;
    }
}
