package daydream.hotel.reservation.system.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import daydream.hotel.reservation.system.auth.security.SecurityUtils;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.coupon.dto.CouponSaveRequest;
import daydream.hotel.reservation.system.coupon.dto.CouponVO;
import daydream.hotel.reservation.system.coupon.dto.UserCouponVO;
import daydream.hotel.reservation.system.coupon.entity.Coupon;
import daydream.hotel.reservation.system.coupon.entity.UserCoupon;
import daydream.hotel.reservation.system.coupon.mapper.CouponMapper;
import daydream.hotel.reservation.system.coupon.mapper.UserCouponMapper;
import daydream.hotel.reservation.system.user.enums.UserRole;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

    public static final String STATUS_UNUSED = "UNUSED";
    public static final String STATUS_USED = "USED";

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public CouponService(CouponMapper couponMapper, UserCouponMapper userCouponMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
    }

    public List<CouponVO> listAvailableCoupons() {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        return couponMapper
                .selectList(
                        new LambdaQueryWrapper<Coupon>()
                                .eq(Coupon::getStatus, 1)
                                .and(
                                        w ->
                                                w.isNull(Coupon::getStartTime)
                                                        .or()
                                                        .le(Coupon::getStartTime, now))
                                .and(
                                        w ->
                                                w.isNull(Coupon::getEndTime)
                                                        .or()
                                                        .ge(Coupon::getEndTime, now))
                                .orderByDesc(Coupon::getId))
                .stream()
                .filter(c -> c.getClaimedCount() < c.getTotalCount())
                .map(c -> toCouponVO(c, hasClaimed(userId, c.getId())))
                .toList();
    }

    public List<UserCouponVO> listMyCoupons() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userCouponMapper
                .selectList(
                        new LambdaQueryWrapper<UserCoupon>()
                                .eq(UserCoupon::getUserId, userId)
                                .orderByDesc(UserCoupon::getCreatedAt))
                .stream()
                .map(this::toUserCouponVO)
                .toList();
    }

    @Transactional
    public UserCouponVO claim(Long couponId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Coupon coupon = getActiveCoupon(couponId);
        if (hasClaimed(userId, couponId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "已领取过该优惠券");
        }
        if (coupon.getClaimedCount() >= coupon.getTotalCount()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "优惠券已领完");
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(STATUS_UNUSED);
        userCouponMapper.insert(userCoupon);
        coupon.setClaimedCount(coupon.getClaimedCount() + 1);
        couponMapper.updateById(coupon);
        return toUserCouponVO(userCoupon);
    }

    public DiscountResult applyDiscount(Long userCouponId, Long userId, BigDecimal orderAmount) {
        if (userCouponId == null) {
            return new DiscountResult(null, BigDecimal.ZERO, orderAmount);
        }
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "优惠券无效");
        }
        if (!STATUS_UNUSED.equals(userCoupon.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "优惠券已使用");
        }
        Coupon coupon = getActiveCoupon(userCoupon.getCouponId());
        if (orderAmount.compareTo(coupon.getMinAmount()) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "未达到优惠券使用门槛");
        }
        BigDecimal discount = coupon.getAmount().min(orderAmount);
        userCoupon.setStatus(STATUS_USED);
        userCoupon.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(userCoupon);
        return new DiscountResult(userCouponId, discount, orderAmount.subtract(discount));
    }

    @Transactional
    public void releaseCoupon(Long userCouponId) {
        if (userCouponId == null) {
            return;
        }
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !STATUS_USED.equals(userCoupon.getStatus())) {
            return;
        }
        userCoupon.setStatus(STATUS_UNUSED);
        userCoupon.setUsedAt(null);
        userCouponMapper.updateById(userCoupon);
    }

    public List<CouponVO> listAdminCoupons() {
        requireAdmin();
        return couponMapper
                .selectList(new LambdaQueryWrapper<Coupon>().orderByDesc(Coupon::getId))
                .stream()
                .map(c -> toCouponVO(c, null))
                .toList();
    }

    @Transactional
    public CouponVO createCoupon(CouponSaveRequest request) {
        requireAdmin();
        Coupon coupon = new Coupon();
        coupon.setName(request.getName());
        coupon.setCouponType("FIXED");
        coupon.setAmount(request.getAmount());
        coupon.setMinAmount(
                request.getMinAmount() != null ? request.getMinAmount() : BigDecimal.ZERO);
        coupon.setTotalCount(request.getTotalCount() != null ? request.getTotalCount() : 100);
        coupon.setClaimedCount(0);
        coupon.setStartTime(request.getStartTime());
        coupon.setEndTime(request.getEndTime());
        coupon.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        couponMapper.insert(coupon);
        return toCouponVO(coupon, null);
    }

    @Transactional
    public CouponVO updateCouponStatus(Long id, Integer status) {
        requireAdmin();
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
        return toCouponVO(coupon, null);
    }

    private Coupon getActiveCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "优惠券不存在或已下架");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "优惠券未到使用时间");
        }
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "优惠券已过期");
        }
        return coupon;
    }

    private boolean hasClaimed(Long userId, Long couponId) {
        return userCouponMapper.selectCount(
                        new LambdaQueryWrapper<UserCoupon>()
                                .eq(UserCoupon::getUserId, userId)
                                .eq(UserCoupon::getCouponId, couponId))
                > 0;
    }

    private void requireAdmin() {
        if (!UserRole.ADMIN.name().equals(SecurityUtils.getLoginUser().getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private CouponVO toCouponVO(Coupon coupon, Boolean claimed) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setName(coupon.getName());
        vo.setAmount(coupon.getAmount());
        vo.setMinAmount(coupon.getMinAmount());
        vo.setTotalCount(coupon.getTotalCount());
        vo.setClaimedCount(coupon.getClaimedCount());
        vo.setStartTime(coupon.getStartTime());
        vo.setEndTime(coupon.getEndTime());
        vo.setStatus(coupon.getStatus());
        vo.setClaimed(claimed);
        return vo;
    }

    private UserCouponVO toUserCouponVO(UserCoupon userCoupon) {
        UserCouponVO vo = new UserCouponVO();
        vo.setId(userCoupon.getId());
        vo.setCouponId(userCoupon.getCouponId());
        vo.setStatus(userCoupon.getStatus());
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon != null) {
            vo.setName(coupon.getName());
            vo.setAmount(coupon.getAmount());
            vo.setMinAmount(coupon.getMinAmount());
            vo.setEndTime(coupon.getEndTime());
        }
        return vo;
    }

    public record DiscountResult(
            Long userCouponId, BigDecimal discountAmount, BigDecimal payAmount) {}
}
