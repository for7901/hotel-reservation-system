package daydream.hotel.reservation.system.coupon.controller;

import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.coupon.dto.CouponVO;
import daydream.hotel.reservation.system.coupon.dto.UserCouponVO;
import daydream.hotel.reservation.system.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "优惠券-用户端")
@RestController
@RequestMapping("/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @Operation(summary = "可领取优惠券")
    @GetMapping("/available")
    public Result<List<CouponVO>> available() {
        return Result.ok(couponService.listAvailableCoupons());
    }

    @Operation(summary = "我的优惠券")
    @GetMapping("/my")
    public Result<List<UserCouponVO>> my() {
        return Result.ok(couponService.listMyCoupons());
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/{id}/claim")
    public Result<UserCouponVO> claim(@PathVariable Long id) {
        return Result.ok(couponService.claim(id));
    }
}
