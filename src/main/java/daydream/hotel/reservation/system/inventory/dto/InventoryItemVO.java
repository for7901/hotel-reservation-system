package daydream.hotel.reservation.system.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InventoryItemVO {

    private LocalDate invDate;
    private BigDecimal price;
    private Integer availableRooms;

    public LocalDate getInvDate() {
        return invDate;
    }

    public void setInvDate(LocalDate invDate) {
        this.invDate = invDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(Integer availableRooms) {
        this.availableRooms = availableRooms;
    }
}
