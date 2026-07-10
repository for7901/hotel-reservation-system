package daydream.hotel.reservation.system.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

public class CreateOrderRequest {

    @NotNull(message = "酒店ID不能为空")
    private Long hotelId;

    @NotNull(message = "房型ID不能为空")
    private Long roomTypeId;

    @NotNull(message = "入住日期不能为空")
    @FutureOrPresent(message = "入住日期不能早于今天")
    private LocalDate checkInDate;

    @NotNull(message = "离店日期不能为空")
    private LocalDate checkOutDate;

    @NotNull(message = "预订间数不能为空")
    @Min(value = 1, message = "预订间数至少为1")
    private Integer roomCount;

    @NotBlank(message = "联系手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    @NotEmpty(message = "入住人信息不能为空")
    @Valid
    private List<@Valid OrderGuestRequest> guests;

    private Long userCouponId;

    public Long getHotelId() {
        return hotelId;
    }

    public void setHotelId(Long hotelId) {
        this.hotelId = hotelId;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Integer getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(Integer roomCount) {
        this.roomCount = roomCount;
    }

    /** 兼容旧字段名：guestCount 视为预订间数 */
    public Integer getGuestCount() {
        return roomCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.roomCount = guestCount;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public List<OrderGuestRequest> getGuests() {
        return guests;
    }

    public void setGuests(List<OrderGuestRequest> guests) {
        this.guests = guests;
    }

    public Long getUserCouponId() {
        return userCouponId;
    }

    public void setUserCouponId(Long userCouponId) {
        this.userCouponId = userCouponId;
    }
}
