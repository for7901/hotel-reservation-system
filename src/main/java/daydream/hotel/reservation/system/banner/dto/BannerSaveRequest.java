package daydream.hotel.reservation.system.banner.dto;

import jakarta.validation.constraints.NotBlank;

public class BannerSaveRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    private String linkUrl;
    private Integer sortOrder;
    private Integer status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
