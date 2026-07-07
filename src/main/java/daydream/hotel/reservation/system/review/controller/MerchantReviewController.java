package daydream.hotel.reservation.system.review.controller;

import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.review.dto.ReplyReviewRequest;
import daydream.hotel.reservation.system.review.dto.ReviewVO;
import daydream.hotel.reservation.system.review.service.ReviewService;
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

@Tag(name = "评价-商家端")
@RestController
@RequestMapping("/merchant/reviews")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantReviewController {

    private final ReviewService reviewService;

    public MerchantReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "商家评价列表")
    @GetMapping
    public Result<PageResult<ReviewVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(reviewService.listMerchantReviews(keyword, page, size));
    }

    @Operation(summary = "回复评价")
    @PutMapping("/{id}/reply")
    public Result<ReviewVO> reply(
            @PathVariable Long id, @Valid @RequestBody ReplyReviewRequest request) {
        return Result.ok(reviewService.replyToReview(id, request.getContent()));
    }
}
