package daydream.hotel.reservation.system.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import daydream.hotel.reservation.system.auth.security.LoginUser;
import daydream.hotel.reservation.system.auth.security.SecurityUtils;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.common.util.SensitiveUtils;
import daydream.hotel.reservation.system.config.AppProperties;
import daydream.hotel.reservation.system.coupon.service.CouponService;
import daydream.hotel.reservation.system.hotel.entity.Hotel;
import daydream.hotel.reservation.system.hotel.entity.RoomType;
import daydream.hotel.reservation.system.hotel.enums.HotelStatus;
import daydream.hotel.reservation.system.hotel.mapper.HotelMapper;
import daydream.hotel.reservation.system.hotel.mapper.RoomTypeMapper;
import daydream.hotel.reservation.system.inventory.service.InventoryService;
import daydream.hotel.reservation.system.order.dto.CreateOrderRequest;
import daydream.hotel.reservation.system.order.dto.OrderGuestRequest;
import daydream.hotel.reservation.system.order.dto.OrderGuestVO;
import daydream.hotel.reservation.system.order.dto.OrderVO;
import daydream.hotel.reservation.system.order.entity.HotelOrder;
import daydream.hotel.reservation.system.order.entity.OrderGuest;
import daydream.hotel.reservation.system.order.enums.OrderStatus;
import daydream.hotel.reservation.system.order.mapper.HotelOrderMapper;
import daydream.hotel.reservation.system.order.mapper.OrderGuestMapper;
import daydream.hotel.reservation.system.review.entity.HotelReview;
import daydream.hotel.reservation.system.review.mapper.HotelReviewMapper;
import daydream.hotel.reservation.system.user.enums.UserRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final HotelOrderMapper orderMapper;
    private final OrderGuestMapper orderGuestMapper;
    private final HotelMapper hotelMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final AppProperties appProperties;
    private final TransactionTemplate transactionTemplate;
    private final HotelReviewMapper hotelReviewMapper;

    public OrderService(
            HotelOrderMapper orderMapper,
            OrderGuestMapper orderGuestMapper,
            HotelMapper hotelMapper,
            RoomTypeMapper roomTypeMapper,
            InventoryService inventoryService,
            CouponService couponService,
            AppProperties appProperties,
            TransactionTemplate transactionTemplate,
            HotelReviewMapper hotelReviewMapper) {
        this.orderMapper = orderMapper;
        this.orderGuestMapper = orderGuestMapper;
        this.hotelMapper = hotelMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.inventoryService = inventoryService;
        this.couponService = couponService;
        this.appProperties = appProperties;
        this.transactionTemplate = transactionTemplate;
        this.hotelReviewMapper = hotelReviewMapper;
    }

    @Transactional
    public OrderVO createOrder(CreateOrderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        validateGuestInfo(request);
        int roomCount = request.getRoomCount();

        Hotel hotel = hotelMapper.selectById(request.getHotelId());
        if (hotel == null || !HotelStatus.APPROVED.name().equals(hotel.getStatus())) {
            throw new BusinessException(ErrorCode.HOTEL_NOT_FOUND);
        }

        RoomType roomType =
                roomTypeMapper.selectOne(
                        new LambdaQueryWrapper<RoomType>()
                                .eq(RoomType::getId, request.getRoomTypeId())
                                .eq(RoomType::getHotelId, request.getHotelId())
                                .eq(RoomType::getStatus, 1));
        if (roomType == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        InventoryService.PriceSummary priceSummary =
                inventoryService.calculatePrice(
                        roomType, request.getCheckInDate(), request.getCheckOutDate(), roomCount);
        inventoryService.reserveInventory(
                roomType.getId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                roomCount);

        CouponService.DiscountResult discount =
                couponService.applyDiscount(
                        request.getUserCouponId(), userId, priceSummary.totalPrice());

        OrderGuestRequest primaryGuest = request.getGuests().get(0);

        HotelOrder order = new HotelOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setHotelId(hotel.getId());
        order.setRoomTypeId(roomType.getId());
        order.setHotelName(hotel.getName());
        order.setRoomTypeName(roomType.getName());
        order.setCheckInDate(request.getCheckInDate());
        order.setCheckOutDate(request.getCheckOutDate());
        order.setNights(priceSummary.nights());
        order.setRoomCount(roomCount);
        order.setGuestCount(request.getGuests().size());
        order.setGuestName(primaryGuest.getName().trim());
        order.setGuestPhone(request.getContactPhone().trim());
        order.setUnitPrice(priceSummary.unitPrice());
        order.setDiscountAmount(discount.discountAmount());
        order.setCouponId(discount.userCouponId());
        order.setTotalAmount(discount.payAmount());
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        orderMapper.insert(order);
        saveOrderGuests(order.getId(), request.getGuests(), request.getContactPhone());
        return toVO(order);
    }

    @Transactional
    public OrderVO payOrder(Long orderId) {
        HotelOrder order = getUserOrder(orderId);
        if (!OrderStatus.canPay(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(OrderStatus.PAID.name());
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return toVO(order);
    }

    @Transactional
    public OrderVO approveGuestReview(Long orderId) {
        HotelOrder order = getMerchantOrder(orderId);
        if (!OrderStatus.canReviewGuests(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(OrderStatus.CONFIRMED.name());
        order.setRejectReason(null);
        orderMapper.updateById(order);
        return toVO(order);
    }

    @Transactional
    public OrderVO rejectGuestReview(Long orderId, String reason) {
        HotelOrder order = getMerchantOrder(orderId);
        if (!OrderStatus.canReviewGuests(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        inventoryService.releaseInventory(
                order.getRoomTypeId(),
                order.getCheckInDate(),
                order.getCheckOutDate(),
                resolveRoomCount(order));
        couponService.releaseCoupon(order.getCouponId());
        order.setStatus(OrderStatus.REFUNDED.name());
        order.setRejectReason(reason.trim());
        order.setCancelledAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return toVO(order);
    }

    @Transactional
    public OrderVO applyCheckout(Long orderId) {
        HotelOrder order = getUserOrder(orderId);
        if (!OrderStatus.canApplyCheckout(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        CheckoutRefundCalculator.CheckoutRefundResult refundResult =
                CheckoutRefundCalculator.calculate(
                        order.getTotalAmount(),
                        order.getCheckInDate(),
                        order.getCheckOutDate(),
                        order.getNights(),
                        now);
        if (refundResult.refundableNights() <= 0
                || refundResult.refundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), refundResult.policy());
        }
        order.setStatus(OrderStatus.CHECKOUT_PENDING.name());
        order.setCheckoutApplyAt(now);
        order.setRefundAmount(refundResult.refundAmount());
        order.setRefundPolicy(refundResult.policy());
        order.setRejectReason(null);
        orderMapper.updateById(order);
        return toVO(order);
    }

    @Transactional
    public OrderVO approveCheckout(Long orderId) {
        HotelOrder order = getMerchantOrder(orderId);
        if (!OrderStatus.canApproveCheckout(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        CheckoutRefundCalculator.CheckoutRefundResult refundResult =
                CheckoutRefundCalculator.calculate(
                        order.getTotalAmount(),
                        order.getCheckInDate(),
                        order.getCheckOutDate(),
                        order.getNights(),
                        order.getCheckoutApplyAt() != null
                                ? order.getCheckoutApplyAt()
                                : LocalDateTime.now());
        if (refundResult.refundFromDate() != null) {
            inventoryService.releaseInventory(
                    order.getRoomTypeId(),
                    refundResult.refundFromDate(),
                    order.getCheckOutDate(),
                    resolveRoomCount(order));
        }
        if (refundResult.refundAmount().compareTo(order.getTotalAmount()) >= 0) {
            couponService.releaseCoupon(order.getCouponId());
        }
        order.setStatus(OrderStatus.REFUNDED.name());
        order.setRefundAmount(refundResult.refundAmount());
        order.setRefundPolicy(refundResult.policy());
        order.setCancelledAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return toVO(order);
    }

    @Transactional
    public OrderVO rejectCheckout(Long orderId, String reason) {
        HotelOrder order = getMerchantOrder(orderId);
        if (!OrderStatus.canApproveCheckout(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(OrderStatus.PAID.name());
        order.setCheckoutApplyAt(null);
        order.setRefundAmount(null);
        order.setRefundPolicy(null);
        order.setRejectReason(reason.trim());
        orderMapper.updateById(order);
        return toVO(order);
    }

    @Transactional
    public OrderVO completeOrder(Long orderId) {
        HotelOrder order = getUserOrder(orderId);
        if (!OrderStatus.canComplete(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(OrderStatus.COMPLETED.name());
        orderMapper.updateById(order);
        return toVO(order);
    }

    @Transactional
    public OrderVO cancelOrder(Long orderId) {
        HotelOrder order = getUserOrder(orderId);
        if (!OrderStatus.canCancel(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        inventoryService.releaseInventory(
                order.getRoomTypeId(),
                order.getCheckInDate(),
                order.getCheckOutDate(),
                resolveRoomCount(order));
        couponService.releaseCoupon(order.getCouponId());
        order.setStatus(OrderStatus.CANCELLED.name());
        order.setCancelledAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return toVO(order);
    }

    /** 用户删除订单（软删除）：待点评/已点评/已取消/已退款可删，删除后不在我的订单中显示 */
    @Transactional
    public void deleteMyOrder(Long orderId) {
        HotelOrder order = getUserOrder(orderId);
        if (order.getUserDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!OrderStatus.canUserDelete(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setUserDeletedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    public int cancelExpiredPendingOrders() {
        LocalDateTime deadline =
                LocalDateTime.now()
                        .minusMinutes(appProperties.getOrder().getPaymentTimeoutMinutes());
        List<HotelOrder> expiredOrders =
                orderMapper.selectList(
                        new LambdaQueryWrapper<HotelOrder>()
                                .eq(HotelOrder::getStatus, OrderStatus.PENDING_PAYMENT.name())
                                .lt(HotelOrder::getCreatedAt, deadline));
        int count = 0;
        for (HotelOrder order : expiredOrders) {
            try {
                transactionTemplate.executeWithoutResult(
                        status -> systemCancelPendingOrder(order.getId()));
                count++;
            } catch (Exception e) {
                log.warn("Failed to auto-cancel order {}", order.getId(), e);
            }
        }
        return count;
    }

    /** 离店日当天及之后，将已支付订单自动标记为已完成 */
    public int autoCompleteDueOrders() {
        LocalDate today = LocalDate.now();
        List<HotelOrder> dueOrders =
                orderMapper.selectList(
                        new LambdaQueryWrapper<HotelOrder>()
                                .in(
                                        HotelOrder::getStatus,
                                        Arrays.asList(
                                                OrderStatus.PAID.name(),
                                                OrderStatus.CONFIRMED.name()))
                                .le(HotelOrder::getCheckOutDate, today));
        int count = 0;
        for (HotelOrder order : dueOrders) {
            try {
                transactionTemplate.executeWithoutResult(
                        status -> systemCompleteDueOrder(order.getId()));
                count++;
            } catch (Exception e) {
                log.warn("Failed to auto-complete order {}", order.getId(), e);
            }
        }
        return count;
    }

    private void systemCancelPendingOrder(Long orderId) {
        HotelOrder order = orderMapper.selectById(orderId);
        if (order == null || !OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            return;
        }
        inventoryService.releaseInventory(
                order.getRoomTypeId(),
                order.getCheckInDate(),
                order.getCheckOutDate(),
                resolveRoomCount(order));
        couponService.releaseCoupon(order.getCouponId());
        order.setStatus(OrderStatus.CANCELLED.name());
        order.setCancelledAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private void systemCompleteDueOrder(Long orderId) {
        HotelOrder order = orderMapper.selectById(orderId);
        if (order == null || !OrderStatus.canComplete(order.getStatus())) {
            return;
        }
        if (order.getCheckOutDate() == null || order.getCheckOutDate().isAfter(LocalDate.now())) {
            return;
        }
        order.setStatus(OrderStatus.COMPLETED.name());
        orderMapper.updateById(order);
    }

    public OrderVO getOrderDetail(Long orderId) {
        return toVO(getAccessibleOrder(orderId));
    }

    /**
     * 用户订单列表。status 支持：
     * ALL / 空：全部（含已取消、已点评；不含用户已删除）
     * PENDING_PAYMENT：待支付
     * UPCOMING：待出行（PAID / CONFIRMED）
     * PENDING_REVIEW：待点评（COMPLETED 且未评价）
     * REFUND：退款单（CHECKOUT_PENDING / REFUNDED）
     */
    public PageResult<OrderVO> listMyOrders(String status, long page, long size) {
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<HotelOrder> wrapper =
                new LambdaQueryWrapper<HotelOrder>()
                        .eq(HotelOrder::getUserId, userId)
                        .isNull(HotelOrder::getUserDeletedAt)
                        .orderByDesc(HotelOrder::getCreatedAt);

        String tab = StringUtils.hasText(status) ? status.trim() : "ALL";
        switch (tab) {
            case "ALL", "" -> {
                // 全部：含已取消支付、已点评等，仅排除用户已删除
            }
            case "PENDING_PAYMENT" ->
                    wrapper.eq(HotelOrder::getStatus, OrderStatus.PENDING_PAYMENT.name());
            case "UPCOMING" ->
                    wrapper.in(
                            HotelOrder::getStatus,
                            Arrays.asList(
                                    OrderStatus.PAID.name(), OrderStatus.CONFIRMED.name()));
            case "PENDING_REVIEW" -> {
                wrapper.eq(HotelOrder::getStatus, OrderStatus.COMPLETED.name());
                List<Long> reviewedOrderIds = listReviewedOrderIds(userId);
                if (!reviewedOrderIds.isEmpty()) {
                    wrapper.notIn(HotelOrder::getId, reviewedOrderIds);
                }
            }
            case "REFUND" ->
                    wrapper.in(
                            HotelOrder::getStatus,
                            Arrays.asList(
                                    OrderStatus.CHECKOUT_PENDING.name(),
                                    OrderStatus.REFUNDED.name(),
                                    OrderStatus.REFUNDING.name()));
            default -> wrapper.eq(HotelOrder::getStatus, tab);
        }

        Page<HotelOrder> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);
        return toPage(orderPage);
    }

    private List<Long> listReviewedOrderIds(Long userId) {
        return hotelReviewMapper
                .selectList(
                        new LambdaQueryWrapper<HotelReview>()
                                .eq(HotelReview::getUserId, userId)
                                .select(HotelReview::getOrderId))
                .stream()
                .map(HotelReview::getOrderId)
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    public PageResult<OrderVO> listMerchantOrders(
            String status, String keyword, long page, long size) {
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
        Page<HotelOrder> orderPage =
                orderMapper.selectPage(
                        new Page<>(page, size), buildOrderQuery(hotelIds, status, keyword));
        return toPage(orderPage);
    }

    public PageResult<OrderVO> listAdminOrders(
            String status, String keyword, long page, long size) {
        if (!UserRole.ADMIN.name().equals(SecurityUtils.getLoginUser().getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Page<HotelOrder> orderPage =
                orderMapper.selectPage(
                        new Page<>(page, size), buildOrderQuery(null, status, keyword));
        return toPage(orderPage);
    }

    private void validateGuestInfo(CreateOrderRequest request) {
        if (request.getRoomCount() == null || request.getRoomCount() < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if (!request.getRoomCount().equals(request.getGuests().size())) {
            throw new BusinessException(ErrorCode.GUEST_INFO_MISMATCH);
        }
        List<OrderGuestRequest> guests = request.getGuests();
        for (OrderGuestRequest guest : guests) {
            if (StringUtils.hasText(guest.getIdCard())
                    && !guest.getIdCard().matches("^\\d{17}[\\dXx]$")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
        }
    }

    private void saveOrderGuests(
            Long orderId, List<OrderGuestRequest> guests, String contactPhone) {
        for (int i = 0; i < guests.size(); i++) {
            OrderGuestRequest guest = guests.get(i);
            OrderGuest entity = new OrderGuest();
            entity.setOrderId(orderId);
            entity.setName(guest.getName().trim());
            String phone =
                    StringUtils.hasText(guest.getPhone())
                            ? guest.getPhone().trim()
                            : contactPhone;
            entity.setPhone(phone);
            entity.setIdCard(
                    StringUtils.hasText(guest.getIdCard()) ? guest.getIdCard().trim() : null);
            entity.setSortOrder(i);
            orderGuestMapper.insert(entity);
        }
    }

    private int resolveRoomCount(HotelOrder order) {
        if (order.getRoomCount() != null && order.getRoomCount() > 0) {
            return order.getRoomCount();
        }
        if (order.getGuestCount() != null && order.getGuestCount() > 0) {
            return order.getGuestCount();
        }
        return 1;
    }

    private LambdaQueryWrapper<HotelOrder> buildOrderQuery(
            List<Long> hotelIds, String status, String keyword) {
        LambdaQueryWrapper<HotelOrder> wrapper =
                new LambdaQueryWrapper<HotelOrder>()
                        .and(
                                StringUtils.hasText(keyword),
                                w ->
                                        w.like(HotelOrder::getOrderNo, keyword)
                                                .or()
                                                .like(HotelOrder::getHotelName, keyword)
                                                .or()
                                                .like(HotelOrder::getGuestName, keyword))
                        .orderByDesc(HotelOrder::getCreatedAt);
        if (StringUtils.hasText(status)) {
            String tab = status.trim();
            switch (tab) {
                case "UPCOMING" ->
                        wrapper.in(
                                HotelOrder::getStatus,
                                Arrays.asList(
                                        OrderStatus.PAID.name(),
                                        OrderStatus.CONFIRMED.name(),
                                        OrderStatus.CHECKED_IN.name(),
                                        OrderStatus.CHECKOUT_PENDING.name()));
                case "PENDING_PAYMENT" ->
                        wrapper.eq(HotelOrder::getStatus, OrderStatus.PENDING_PAYMENT.name());
                case "COMPLETED" ->
                        wrapper.eq(HotelOrder::getStatus, OrderStatus.COMPLETED.name());
                case "REFUNDED" ->
                        wrapper.in(
                                HotelOrder::getStatus,
                                Arrays.asList(
                                        OrderStatus.REFUNDED.name(),
                                        OrderStatus.REFUNDING.name()));
                default -> wrapper.eq(HotelOrder::getStatus, tab);
            }
        }
        if (hotelIds != null) {
            wrapper.in(HotelOrder::getHotelId, hotelIds);
        }
        return wrapper;
    }

    private HotelOrder getUserOrder(Long orderId) {
        HotelOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getUserDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return order;
    }

    private HotelOrder getMerchantOrder(Long orderId) {
        HotelOrder order = getAccessibleOrder(orderId);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!UserRole.MERCHANT.name().equals(loginUser.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Hotel hotel = hotelMapper.selectById(order.getHotelId());
        if (hotel == null || !hotel.getMerchantId().equals(loginUser.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return order;
    }

    private HotelOrder getAccessibleOrder(Long orderId) {
        HotelOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (order.getUserId().equals(loginUser.getUserId())) {
            if (order.getUserDeletedAt() != null) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }
            return order;
        }
        if (UserRole.ADMIN.name().equals(loginUser.getRole())) {
            return order;
        }
        if (UserRole.MERCHANT.name().equals(loginUser.getRole())) {
            Hotel hotel = hotelMapper.selectById(order.getHotelId());
            if (hotel != null && hotel.getMerchantId().equals(loginUser.getUserId())) {
                return order;
            }
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private String generateOrderNo() {
        return "ORD"
                + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private PageResult<OrderVO> toPage(Page<HotelOrder> orderPage) {
        List<OrderVO> list = orderPage.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(
                list, orderPage.getTotal(), orderPage.getCurrent(), orderPage.getSize());
    }

    private OrderVO toVO(HotelOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setHotelId(order.getHotelId());
        vo.setRoomTypeId(order.getRoomTypeId());
        vo.setHotelName(order.getHotelName());
        vo.setRoomTypeName(order.getRoomTypeName());
        vo.setCheckInDate(order.getCheckInDate());
        vo.setCheckOutDate(order.getCheckOutDate());
        vo.setNights(order.getNights());
        vo.setGuestCount(order.getGuestCount());
        vo.setRoomCount(resolveRoomCount(order));
        vo.setGuestName(order.getGuestName());
        vo.setGuestPhone(SensitiveUtils.maskPhone(order.getGuestPhone()));
        vo.setUnitPrice(order.getUnitPrice());
        vo.setDiscountAmount(
                order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setRejectReason(order.getRejectReason());
        vo.setPaidAt(order.getPaidAt());
        vo.setCancelledAt(order.getCancelledAt());
        vo.setCheckoutApplyAt(order.getCheckoutApplyAt());
        vo.setRefundAmount(order.getRefundAmount());
        vo.setRefundPolicy(order.getRefundPolicy());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setReviewed(isOrderReviewed(order.getId()));
        vo.setGuests(loadGuestVOs(order.getId()));
        return vo;
    }

    private boolean isOrderReviewed(Long orderId) {
        Long count =
                hotelReviewMapper.selectCount(
                        new LambdaQueryWrapper<HotelReview>().eq(HotelReview::getOrderId, orderId));
        return count != null && count > 0;
    }

    private List<OrderGuestVO> loadGuestVOs(Long orderId) {
        return orderGuestMapper
                .selectList(
                        new LambdaQueryWrapper<OrderGuest>()
                                .eq(OrderGuest::getOrderId, orderId)
                                .orderByAsc(OrderGuest::getSortOrder))
                .stream()
                .map(this::toGuestVO)
                .toList();
    }

    private OrderGuestVO toGuestVO(OrderGuest guest) {
        OrderGuestVO vo = new OrderGuestVO();
        vo.setId(guest.getId());
        vo.setName(guest.getName());
        vo.setPhone(SensitiveUtils.maskPhone(guest.getPhone()));
        vo.setIdCard(guest.getIdCard());
        vo.setSortOrder(guest.getSortOrder());
        return vo;
    }
}
