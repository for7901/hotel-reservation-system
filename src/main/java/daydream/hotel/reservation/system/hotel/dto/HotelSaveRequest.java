package daydream.hotel.reservation.system.hotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class HotelSaveRequest {

    @NotNull(message = "城市不能为空")
    private Long cityId;

    @NotBlank(message = "酒店名称不能为空")
    @Size(max = 100, message = "酒店名称过长")
    private String name;

    @NotBlank(message = "地址不能为空")
    @Size(max = 255, message = "地址过长")
    private String address;

    @Min(value = 1, message = "星级范围1-5")
    @Max(value = 5, message = "星级范围1-5")
    private Integer starRating = 3;

    @Size(max = 500, message = "封面地址过长")
    private String coverImage;

    @Size(max = 2000, message = "描述过长")
    private String description;

    private List<String> facilities;

    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getStarRating() {
        return starRating;
    }

    public void setStarRating(Integer starRating) {
        this.starRating = starRating;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<String> facilities) {
        this.facilities = facilities;
    }
}
