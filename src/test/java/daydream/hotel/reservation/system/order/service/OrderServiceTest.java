package daydream.hotel.reservation.system.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import daydream.hotel.reservation.system.auth.security.LoginUser;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.config.AppProperties;
import daydream.hotel.reservation.system.coupon.service.CouponService;
import daydream.hotel.reservation.system.hotel.entity.Hotel;
import daydream.hotel.reservation.system.hotel.entity.RoomType;
import daydream.hotel.reservation.system.hotel.enums.HotelStatus;
import daydream.hotel.reservation.system.hotel.mapper.HotelMapper;
import daydream.hotel.reservation.system.hotel.mapper.RoomTypeMapper;
import daydream.hotel.reservation.system.inventory.service.InventoryService;
import daydream.hotel.reservation.system.order.dto.CreateOrderRequest;
import daydream.hotel.reservation.system.order.dto.OrderVO;
import daydream.hotel.reservation.system.order.entity.HotelOrder;
import daydream.hotel.reservation.system.order.enums.OrderStatus;
import daydream.hotel.reservation.system.order.mapper.HotelOrderMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private HotelOrderMapper orderMapper;

    @Mock private HotelMapper hotelMapper;

    @Mock private RoomTypeMapper roomTypeMapper;

    @Mock private InventoryService inventoryService;

    @Mock private CouponService couponService;

    @Mock private AppProperties appProperties;

    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks private OrderService orderService;

    private CreateOrderRequest request;
    private Hotel hotel;
    private RoomType roomType;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new LoginUser(100L, "13900001111", "pwd", "USER", true),
                                null,
                                null));

        request = new CreateOrderRequest();
        request.setHotelId(1L);
        request.setRoomTypeId(2L);
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(2));
        request.setGuestName("张三");
        request.setGuestPhone("13900001111");

        hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("测试酒店");
        hotel.setStatus(HotelStatus.APPROVED.name());

        roomType = new RoomType();
        roomType.setId(2L);
        roomType.setHotelId(1L);
        roomType.setName("大床房");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrderShouldReserveInventoryAndPersistOrder() {
        when(hotelMapper.selectById(1L)).thenReturn(hotel);
        when(roomTypeMapper.selectOne(any())).thenReturn(roomType);
        when(inventoryService.calculatePrice(any(), any(), any()))
                .thenReturn(
                        new InventoryService.PriceSummary(
                                1, new BigDecimal("299.00"), new BigDecimal("299.00")));
        when(couponService.applyDiscount(null, 100L, new BigDecimal("299.00")))
                .thenReturn(
                        new CouponService.DiscountResult(
                                null, BigDecimal.ZERO, new BigDecimal("299.00")));

        OrderVO vo = orderService.createOrder(request);

        verify(inventoryService)
                .reserveInventory(2L, request.getCheckInDate(), request.getCheckOutDate());

        ArgumentCaptor<HotelOrder> captor = ArgumentCaptor.forClass(HotelOrder.class);
        verify(orderMapper).insert(captor.capture());
        HotelOrder saved = captor.getValue();
        assertEquals(100L, saved.getUserId());
        assertEquals(OrderStatus.PENDING_PAYMENT.name(), saved.getStatus());
        assertEquals(0, saved.getTotalAmount().compareTo(new BigDecimal("299.00")));

        assertNotNull(vo.getOrderNo());
        assertEquals(OrderStatus.PENDING_PAYMENT.name(), vo.getStatus());
    }

    @Test
    void createOrderShouldRejectInvalidDateRange() {
        request.setCheckOutDate(request.getCheckInDate());
        assertThrows(BusinessException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrderShouldRejectUnapprovedHotel() {
        hotel.setStatus(HotelStatus.PENDING.name());
        when(hotelMapper.selectById(1L)).thenReturn(hotel);
        assertThrows(BusinessException.class, () -> orderService.createOrder(request));
    }
}
