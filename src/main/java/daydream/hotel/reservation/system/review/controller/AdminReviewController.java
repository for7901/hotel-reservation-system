package daydream.hotel.reservation.system.review.controller;

import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.review.dto.ReviewVO;
import daydream.hotel.reservation.system.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "评价-平台端")
@RestController
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "评价列表")
    @GetMapping
    public Result<PageResult<ReviewVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(reviewService.listAdminReviews(keyword, page, size));
    }

    @Operation(summary = "隐藏/展示评价")
    @PutMapping("/{id}/visibility")
    public Result<ReviewVO> updateVisibility(@PathVariable Long id, @RequestParam Integer status) {
        return Result.ok(reviewService.updateVisibility(id, status));
    }
}
