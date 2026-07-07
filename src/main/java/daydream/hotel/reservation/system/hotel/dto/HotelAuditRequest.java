package daydream.hotel.reservation.system.hotel.dto;

import jakarta.validation.constraints.NotBlank;

public class HotelAuditRequest {

    @NotBlank(message = "审核状态不能为空")
    private String status;

    private String rejectReason;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
