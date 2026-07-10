package daydream.hotel.reservation.system.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import daydream.hotel.reservation.system.auth.security.LoginUser;
import daydream.hotel.reservation.system.auth.security.SecurityUtils;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.dashboard.dto.DashboardStatsVO;
import daydream.hotel.reservation.system.hotel.entity.Hotel;
import daydream.hotel.reservation.system.hotel.enums.HotelStatus;
import daydream.hotel.reservation.system.hotel.mapper.HotelMapper;
import daydream.hotel.reservation.system.order.entity.HotelOrder;
import daydream.hotel.reservation.system.order.enums.OrderStatus;
import daydream.hotel.reservation.system.order.mapper.HotelOrderMapper;
import daydream.hotel.reservation.system.user.entity.User;
import daydream.hotel.reservation.system.user.enums.UserRole;
import daydream.hotel.reservation.system.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final HotelOrderMapper orderMapper;
    private final HotelMapper hotelMapper;
    private final UserMapper userMapper;

    public DashboardService(
            HotelOrderMapper orderMapper, HotelMapper hotelMapper, UserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.hotelMapper = hotelMapper;
        this.userMapper = userMapper;
    }

    public DashboardStatsVO getAdminStats() {
        requireRole(UserRole.ADMIN);
        DashboardStatsVO vo = new DashboardStatsVO();
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        vo.setTodayOrderCount(countOrdersSince(dayStart, null));
        vo.setTodayRevenue(sumRevenueSince(dayStart, null));
        vo.setPendingHotelCount(
                hotelMapper.selectCount(
                        new LambdaQueryWrapper<Hotel>()
                                .eq(Hotel::getStatus, HotelStatus.PENDING.name())));
        vo.setPendingPaymentCount(
                orderMapper.selectCount(
                        new LambdaQueryWrapper<HotelOrder>()
                                .eq(HotelOrder::getStatus, OrderStatus.PENDING_PAYMENT.name())));
        vo.setTotalOrderCount(orderMapper.selectCount(null));
        vo.setTotalUserCount(
                userMapper.selectCount(
                        new LambdaQueryWrapper<User>().eq(User::getRole, UserRole.USER.name())));
        vo.setHotelCount(
                hotelMapper.selectCount(
                        new LambdaQueryWrapper<Hotel>()
                                .eq(Hotel::getStatus, HotelStatus.APPROVED.name())));
        return vo;
    }

    public DashboardStatsVO getMerchantStats() {
        requireRole(UserRole.MERCHANT);
        Long merchantId = SecurityUtils.getCurrentUserId();
        List<Long> hotelIds =
                hotelMapper
                        .selectList(
                                new LambdaQueryWrapper<Hotel>()
                                        .eq(Hotel::getMerchantId, merchantId))
                        .stream()
                        .map(Hotel::getId)
                        .toList();

        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setHotelCount(hotelIds.size());
        if (hotelIds.isEmpty()) {
            return vo;
        }

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        vo.setTodayOrderCount(countOrdersSince(dayStart, hotelIds));
        vo.setTodayRevenue(sumRevenueSince(dayStart, hotelIds));
        vo.setPendingPaymentCount(
                orderMapper.selectCount(
                        new LambdaQueryWrapper<HotelOrder>()
                                .in(HotelOrder::getHotelId, hotelIds)
                                .eq(HotelOrder::getStatus, OrderStatus.PENDING_PAYMENT.name())));
        vo.setTotalOrderCount(
                orderMapper.selectCount(
                        new LambdaQueryWrapper<HotelOrder>().in(HotelOrder::getHotelId, hotelIds)));
        vo.setPendingHotelCount(
                hotelMapper.selectCount(
                        new LambdaQueryWrapper<Hotel>()
                                .eq(Hotel::getMerchantId, merchantId)
                                .eq(Hotel::getStatus, HotelStatus.PENDING.name())));
        return vo;
    }

    private long countOrdersSince(LocalDateTime start, List<Long> hotelIds) {
        LambdaQueryWrapper<HotelOrder> wrapper =
                new LambdaQueryWrapper<HotelOrder>()
                        .ge(HotelOrder::getCreatedAt, start)
                        .lt(HotelOrder::getCreatedAt, LocalDate.now().plusDays(1).atStartOfDay());
        if (hotelIds != null) {
            wrapper.in(HotelOrder::getHotelId, hotelIds);
        }
        return orderMapper.selectCount(wrapper);
    }

    private BigDecimal sumRevenueSince(LocalDateTime start, List<Long> hotelIds) {
        LocalDateTime dayEnd = LocalDate.now().plusDays(1).atStartOfDay();
        // 今日营收：按支付时间统计；排除未支付、取消支付、退款中/已退款
        LambdaQueryWrapper<HotelOrder> wrapper =
                new LambdaQueryWrapper<HotelOrder>()
                        .ge(HotelOrder::getPaidAt, start)
                        .lt(HotelOrder::getPaidAt, dayEnd)
                        .notIn(
                                HotelOrder::getStatus,
                                List.of(
                                        OrderStatus.PENDING_PAYMENT.name(),
                                        OrderStatus.CANCELLED.name(),
                                        OrderStatus.REFUNDING.name(),
                                        OrderStatus.REFUNDED.name()));
        if (hotelIds != null) {
            wrapper.in(HotelOrder::getHotelId, hotelIds);
        }
        return orderMapper.selectList(wrapper).stream()
                .map(HotelOrder::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireRole(UserRole role) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!role.name().equals(loginUser.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
