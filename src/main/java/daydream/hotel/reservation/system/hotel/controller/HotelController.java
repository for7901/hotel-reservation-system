package daydream.hotel.reservation.system.hotel.controller;

import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.hotel.dto.CityVO;
import daydream.hotel.reservation.system.hotel.dto.HotelDetailVO;
import daydream.hotel.reservation.system.hotel.dto.HotelListItemVO;
import daydream.hotel.reservation.system.hotel.dto.ProvinceVO;
import daydream.hotel.reservation.system.hotel.service.HotelService;
import daydream.hotel.reservation.system.inventory.dto.AvailabilityVO;
import daydream.hotel.reservation.system.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "酒店-用户端")
@RestController
@RequestMapping("/hotels")
public class HotelController {

    private final HotelService hotelService;
    private final InventoryService inventoryService;

    public HotelController(HotelService hotelService, InventoryService inventoryService) {
        this.hotelService = hotelService;
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "省份列表")
    @GetMapping("/provinces")
    public Result<List<ProvinceVO>> listProvinces() {
        return Result.ok(hotelService.listProvinces());
    }

    @Operation(summary = "城市列表")
    @GetMapping("/cities")
    public Result<List<CityVO>> listCities(@RequestParam(required = false) Long provinceId) {
        return Result.ok(hotelService.listCities(provinceId));
    }

    @Operation(summary = "搜索酒店")
    @GetMapping("/search")
    public Result<PageResult<HotelListItemVO>> search(
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer starRating,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) java.math.BigDecimal minScore,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(
                hotelService.searchHotels(
                        provinceId,
                        cityId,
                        keyword,
                        starRating,
                        minPrice,
                        maxPrice,
                        minScore,
                        sortBy,
                        page,
                        size));
    }

    @Operation(summary = "酒店详情")
    @GetMapping("/{id}")
    public Result<HotelDetailVO> detail(@PathVariable Long id) {
        return Result.ok(hotelService.getHotelDetail(id));
    }

    @Operation(summary = "查询房型可订状态")
    @GetMapping("/{id}/availability")
    public Result<AvailabilityVO> availability(
            @PathVariable Long id,
            @RequestParam Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
            @RequestParam(required = false, defaultValue = "1") Integer roomCount) {
        return Result.ok(
                inventoryService.checkAvailability(
                        id, roomTypeId, checkInDate, checkOutDate, roomCount));
    }
}
