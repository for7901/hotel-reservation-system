package daydream.hotel.reservation.system.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import daydream.hotel.reservation.system.auth.security.LoginUser;
import daydream.hotel.reservation.system.auth.security.SecurityUtils;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.hotel.entity.Hotel;
import daydream.hotel.reservation.system.hotel.mapper.HotelMapper;
import daydream.hotel.reservation.system.order.entity.HotelOrder;
import daydream.hotel.reservation.system.order.enums.OrderStatus;
import daydream.hotel.reservation.system.order.mapper.HotelOrderMapper;
import daydream.hotel.reservation.system.review.dto.CreateReviewRequest;
import daydream.hotel.reservation.system.review.dto.ReviewVO;
import daydream.hotel.reservation.system.review.entity.HotelReview;
import daydream.hotel.reservation.system.review.mapper.HotelReviewMapper;
import daydream.hotel.reservation.system.user.entity.User;
import daydream.hotel.reservation.system.user.enums.UserRole;
import daydream.hotel.reservation.system.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReviewService {

    private final HotelReviewMapper reviewMapper;
    private final HotelOrderMapper orderMapper;
    private final HotelMapper hotelMapper;
    private final UserMapper userMapper;

    public ReviewService(
            HotelReviewMapper reviewMapper,
            HotelOrderMapper orderMapper,
            HotelMapper hotelMapper,
            UserMapper userMapper) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
        this.hotelMapper = hotelMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public ReviewVO createReview(CreateReviewRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        HotelOrder order = orderMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID.getCode(), "仅已完成订单可评价");
        }
        Long exists =
                reviewMapper.selectCount(
                        new LambdaQueryWrapper<HotelReview>()
                                .eq(HotelReview::getOrderId, order.getId()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "该订单已评价");
        }

        User user = userMapper.selectById(userId);
        HotelReview review = new HotelReview();
        review.setUserId(userId);
        review.setHotelId(order.getHotelId());
        review.setOrderId(order.getId());
        review.setUserNickname(user != null ? user.getNickname() : null);
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setStatus(1);
        reviewMapper.insert(review);
        refreshHotelScore(order.getHotelId());
        return toVO(review);
    }

    public PageResult<ReviewVO> listHotelReviews(Long hotelId, long page, long size) {
        Page<HotelReview> reviewPage =
                reviewMapper.selectPage(
                        new Page<>(page, size),
                        new LambdaQueryWrapper<HotelReview>()
                                .eq(HotelReview::getHotelId, hotelId)
                                .eq(HotelReview::getStatus, 1)
                                .orderByDesc(HotelReview::getCreatedAt));
        return toPage(reviewPage);
    }

    public PageResult<ReviewVO> listAdminReviews(String keyword, long page, long size) {
        if (!UserRole.ADMIN.name().equals(SecurityUtils.getLoginUser().getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Page<HotelReview> reviewPage =
                reviewMapper.selectPage(
                        new Page<>(page, size),
                        new LambdaQueryWrapper<HotelReview>()
                                .and(
                                        StringUtils.hasText(keyword),
                                        w ->
                                                w.like(HotelReview::getContent, keyword)
                                                        .or()
                                                        .like(
                                                                HotelReview::getUserNickname,
                                                                keyword))
                                .orderByDesc(HotelReview::getCreatedAt));
        return toPage(reviewPage, loadHotelNameMap(reviewPage.getRecords()));
    }

    public PageResult<ReviewVO> listMerchantReviews(String keyword, long page, long size) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!UserRole.MERCHANT.name().equals(loginUser.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        List<Long> hotelIds =
                hotelMapper
                        .selectList(
                                new LambdaQueryWrapper<Hotel>()
                                        .eq(Hotel::getMerchantId, loginUser.getUserId()))
                        .stream()
                        .map(Hotel::getId)
                        .toList();
        if (hotelIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, page, size);
        }
        Page<HotelReview> reviewPage =
                reviewMapper.selectPage(
                        new Page<>(page, size),
                        new LambdaQueryWrapper<HotelReview>()
                                .in(HotelReview::getHotelId, hotelIds)
                                .eq(HotelReview::getStatus, 1)
                                .and(
                                        StringUtils.hasText(keyword),
                                        w ->
                                                w.like(HotelReview::getContent, keyword)
                                                        .or()
                                                        .like(
                                                                HotelReview::getUserNickname,
                                                                keyword))
                                .orderByDesc(HotelReview::getCreatedAt));
        return toPage(reviewPage, loadHotelNameMap(reviewPage.getRecords()));
    }

    @Transactional
    public ReviewVO replyToReview(Long id, String content) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!UserRole.MERCHANT.name().equals(loginUser.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        HotelReview review = reviewMapper.selectById(id);
        if (review == null || review.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Hotel hotel = hotelMapper.selectById(review.getHotelId());
        if (hotel == null || !hotel.getMerchantId().equals(loginUser.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        review.setMerchantReply(content.trim());
        review.setReplyAt(LocalDateTime.now());
        reviewMapper.updateById(review);
        return toVO(review, hotel.getName());
    }

    @Transactional
    public ReviewVO updateVisibility(Long id, Integer status) {
        if (!UserRole.ADMIN.name().equals(SecurityUtils.getLoginUser().getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        HotelReview review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        review.setStatus(status);
        reviewMapper.updateById(review);
        refreshHotelScore(review.getHotelId());
        return toVO(review);
    }

    private void refreshHotelScore(Long hotelId) {
        List<HotelReview> reviews =
                reviewMapper.selectList(
                        new LambdaQueryWrapper<HotelReview>()
                                .eq(HotelReview::getHotelId, hotelId)
                                .eq(HotelReview::getStatus, 1));
        Hotel hotel = hotelMapper.selectById(hotelId);
        if (hotel == null) {
            return;
        }
        if (reviews.isEmpty()) {
            hotel.setScore(BigDecimal.valueOf(4.5));
        } else {
            double avg = reviews.stream().mapToInt(HotelReview::getRating).average().orElse(4.5);
            hotel.setScore(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        }
        hotelMapper.updateById(hotel);
    }

    private Map<Long, String> loadHotelNameMap(List<HotelReview> reviews) {
        List<Long> hotelIds = reviews.stream().map(HotelReview::getHotelId).distinct().toList();
        if (hotelIds.isEmpty()) {
            return Map.of();
        }
        return hotelMapper.selectBatchIds(hotelIds).stream()
                .collect(Collectors.toMap(Hotel::getId, Hotel::getName));
    }

    private PageResult<ReviewVO> toPage(Page<HotelReview> reviewPage) {
        return toPage(reviewPage, loadHotelNameMap(reviewPage.getRecords()));
    }

    private PageResult<ReviewVO> toPage(Page<HotelReview> reviewPage, Map<Long, String> hotelNameMap) {
        List<ReviewVO> list =
                reviewPage.getRecords().stream()
                        .map(
                                review ->
                                        toVO(
                                                review,
                                                hotelNameMap.get(review.getHotelId())))
                        .toList();
        return new PageResult<>(
                list, reviewPage.getTotal(), reviewPage.getCurrent(), reviewPage.getSize());
    }

    private ReviewVO toVO(HotelReview review) {
        return toVO(review, null);
    }

    private ReviewVO toVO(HotelReview review, String hotelName) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setUserId(review.getUserId());
        vo.setHotelId(review.getHotelId());
        vo.setOrderId(review.getOrderId());
        vo.setUserNickname(review.getUserNickname());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setMerchantReply(review.getMerchantReply());
        vo.setReplyAt(review.getReplyAt());
        vo.setStatus(review.getStatus());
        vo.setCreatedAt(review.getCreatedAt());
        vo.setHotelName(hotelName);
        return vo;
    }
}
