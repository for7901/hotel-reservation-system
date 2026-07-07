package daydream.hotel.reservation.system.hotel.controller;

import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.hotel.dto.AdminHotelVO;
import daydream.hotel.reservation.system.hotel.dto.HotelSaveRequest;
import daydream.hotel.reservation.system.hotel.dto.RoomTypeSaveRequest;
import daydream.hotel.reservation.system.hotel.dto.RoomTypeVO;
import daydream.hotel.reservation.system.hotel.service.HotelService;
import daydream.hotel.reservation.system.inventory.dto.InventoryItemRequest;
import daydream.hotel.reservation.system.inventory.dto.InventoryItemVO;
import daydream.hotel.reservation.system.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "酒店-商家端")
@RestController
@RequestMapping("/merchant/hotels")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantHotelController {

    private final HotelService hotelService;
    private final InventoryService inventoryService;

    public MerchantHotelController(HotelService hotelService, InventoryService inventoryService) {
        this.hotelService = hotelService;
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "我的酒店列表")
    @GetMapping
    public Result<PageResult<AdminHotelVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(hotelService.merchantListHotels(page, size));
    }

    @Operation(summary = "创建酒店")
    @PostMapping
    public Result<AdminHotelVO> create(@Valid @RequestBody HotelSaveRequest request) {
        return Result.ok(hotelService.createHotel(request));
    }

    @Operation(summary = "更新酒店")
    @PutMapping("/{id}")
    public Result<AdminHotelVO> update(
            @PathVariable Long id, @Valid @RequestBody HotelSaveRequest request) {
        return Result.ok(hotelService.updateHotel(id, request));
    }

    @Operation(summary = "删除酒店")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return Result.ok();
    }

    @Operation(summary = "房型列表")
    @GetMapping("/{hotelId}/room-types")
    public Result<List<RoomTypeVO>> listRoomTypes(@PathVariable Long hotelId) {
        return Result.ok(hotelService.listRoomTypes(hotelId));
    }

    @Operation(summary = "新增房型")
    @PostMapping("/{hotelId}/room-types")
    public Result<RoomTypeVO> createRoomType(
            @PathVariable Long hotelId, @Valid @RequestBody RoomTypeSaveRequest request) {
        return Result.ok(hotelService.createRoomType(hotelId, request));
    }

    @Operation(summary = "更新房型")
    @PutMapping("/{hotelId}/room-types/{roomTypeId}")
    public Result<RoomTypeVO> updateRoomType(
            @PathVariable Long hotelId,
            @PathVariable Long roomTypeId,
            @Valid @RequestBody RoomTypeSaveRequest request) {
        return Result.ok(hotelService.updateRoomType(hotelId, roomTypeId, request));
    }

    @Operation(summary = "删除房型")
    @DeleteMapping("/{hotelId}/room-types/{roomTypeId}")
    public Result<Void> deleteRoomType(@PathVariable Long hotelId, @PathVariable Long roomTypeId) {
        hotelService.deleteRoomType(hotelId, roomTypeId);
        return Result.ok();
    }

    @Operation(summary = "查询库存日历")
    @GetMapping("/{hotelId}/room-types/{roomTypeId}/inventory")
    public Result<List<InventoryItemVO>> listInventory(
            @PathVariable Long hotelId,
            @PathVariable Long roomTypeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return Result.ok(inventoryService.listInventory(hotelId, roomTypeId, startDate, endDate));
    }

    @Operation(summary = "保存库存日历")
    @PutMapping("/{hotelId}/room-types/{roomTypeId}/inventory")
    public Result<Void> saveInventory(
            @PathVariable Long hotelId,
            @PathVariable Long roomTypeId,
            @Valid @RequestBody List<InventoryItemRequest> items) {
        inventoryService.saveInventory(hotelId, roomTypeId, items);
        return Result.ok();
    }
}
