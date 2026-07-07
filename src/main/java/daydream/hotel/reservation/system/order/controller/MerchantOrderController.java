package daydream.hotel.reservation.system.order.controller;

import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.order.dto.OrderVO;
import daydream.hotel.reservation.system.order.dto.RejectCheckoutRequest;
import daydream.hotel.reservation.system.order.dto.RejectGuestReviewRequest;
import daydream.hotel.reservation.system.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单-商家端")
@RestController
@RequestMapping("/merchant/orders")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantOrderController {

    private final OrderService orderService;

    public MerchantOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "商家订单列表")
    @GetMapping
    public Result<PageResult<OrderVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(orderService.listMerchantOrders(status, keyword, page, size));
    }

    @Operation(summary = "审核通过入住人信息")
    @PostMapping("/{id}/approve-guests")
    public Result<OrderVO> approveGuests(@PathVariable Long id) {
        return Result.ok(orderService.approveGuestReview(id));
    }

    @Operation(summary = "拒绝入住人信息")
    @PostMapping("/{id}/reject-guests")
    public Result<OrderVO> rejectGuests(
            @PathVariable Long id, @Valid @RequestBody RejectGuestReviewRequest request) {
        return Result.ok(orderService.rejectGuestReview(id, request.getReason()));
    }

    @Operation(summary = "确认退房并退款")
    @PostMapping("/{id}/approve-checkout")
    public Result<OrderVO> approveCheckout(@PathVariable Long id) {
        return Result.ok(orderService.approveCheckout(id));
    }

    @Operation(summary = "拒绝退房申请")
    @PostMapping("/{id}/reject-checkout")
    public Result<OrderVO> rejectCheckout(
            @PathVariable Long id, @Valid @RequestBody RejectCheckoutRequest request) {
        return Result.ok(orderService.rejectCheckout(id, request.getReason()));
    }
}
