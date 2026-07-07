package daydream.hotel.reservation.system.hotel.controller;

import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.hotel.dto.AdminHotelVO;
import daydream.hotel.reservation.system.hotel.dto.HotelAuditRequest;
import daydream.hotel.reservation.system.hotel.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "酒店-管理端")
@RestController
@RequestMapping("/admin/hotels")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHotelController {

    private final HotelService hotelService;

    public AdminHotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @Operation(summary = "酒店列表")
    @GetMapping
    public Result<PageResult<AdminHotelVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(hotelService.adminListHotels(status, keyword, page, size));
    }

    @Operation(summary = "审核酒店")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(
            @PathVariable Long id, @Valid @RequestBody HotelAuditRequest request) {
        hotelService.auditHotel(id, request);
        return Result.ok();
    }
}
