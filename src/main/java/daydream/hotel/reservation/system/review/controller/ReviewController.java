package daydream.hotel.reservation.system.review.controller;

import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.common.result.Result;
import daydream.hotel.reservation.system.review.dto.CreateReviewRequest;
import daydream.hotel.reservation.system.review.dto.ReviewVO;
import daydream.hotel.reservation.system.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "评价-用户端")
@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "提交评价")
    @PostMapping
    public Result<ReviewVO> create(@Valid @RequestBody CreateReviewRequest request) {
        return Result.ok(reviewService.createReview(request));
    }

    @Operation(summary = "酒店评价列表")
    @GetMapping("/hotels/{hotelId}")
    public Result<PageResult<ReviewVO>> listByHotel(
            @PathVariable Long hotelId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(reviewService.listHotelReviews(hotelId, page, size));
    }
}
