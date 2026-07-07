package daydream.hotel.reservation.system.coupon.controller;

import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.coupon.dto.CouponSaveRequest;
import daydream.hotel.reservation.system.coupon.dto.CouponVO;
import daydream.hotel.reservation.system.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "优惠券-平台端")
@RestController
@RequestMapping("/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @Operation(summary = "优惠券列表")
    @GetMapping
    public Result<List<CouponVO>> list() {
        return Result.ok(couponService.listAdminCoupons());
    }

    @Operation(summary = "创建优惠券")
    @PostMapping
    public Result<CouponVO> create(@Valid @RequestBody CouponSaveRequest request) {
        return Result.ok(couponService.createCoupon(request));
    }
}
