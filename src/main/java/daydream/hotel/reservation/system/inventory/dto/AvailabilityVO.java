package daydream.hotel.reservation.system.inventory.dto;

import java.math.BigDecimal;

public class AvailabilityVO {

    private boolean available;
    private int nights;
    private BigDecimal totalPrice;
    private BigDecimal unitPrice;
    private String message;
    private Integer availableRooms;

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(Integer availableRooms) {
        this.availableRooms = availableRooms;
    }
}
