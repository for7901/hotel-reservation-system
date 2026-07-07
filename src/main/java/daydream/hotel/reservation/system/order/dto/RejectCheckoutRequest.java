package daydream.hotel.reservation.system.order.dto;

import jakarta.validation.constraints.NotBlank;

public class RejectCheckoutRequest {

    @NotBlank(message = "请填写拒绝原因")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
