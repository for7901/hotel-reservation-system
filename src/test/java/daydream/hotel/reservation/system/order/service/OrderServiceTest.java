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
import daydream.hotel.reservation.system.order.dto.OrderGuestRequest;
import daydream.hotel.reservation.system.order.dto.OrderVO;
import daydream.hotel.reservation.system.order.entity.HotelOrder;
import daydream.hotel.reservation.system.order.entity.OrderGuest;
import daydream.hotel.reservation.system.order.enums.OrderStatus;
import daydream.hotel.reservation.system.order.mapper.HotelOrderMapper;
import daydream.hotel.reservation.system.order.mapper.OrderGuestMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @Mock private OrderGuestMapper orderGuestMapper;

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

        OrderGuestRequest guest = new OrderGuestRequest();
        guest.setName("张三");
        guest.setPhone("13900001111");

        request = new CreateOrderRequest();
        request.setHotelId(1L);
        request.setRoomTypeId(2L);
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(2));
        request.setGuestCount(1);
        request.setGuests(List.of(guest));

        hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("测试酒店");
        hotel.setStatus(HotelStatus.APPROVED.name());

        roomType = new RoomType();
        roomType.setId(2L);
        roomType.setHotelId(1L);
        roomType.setName("大床房");
        roomType.setMaxGuests(2);
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
        verify(orderGuestMapper).insert(org.mockito.ArgumentMatchers.<OrderGuest>any());

        ArgumentCaptor<HotelOrder> captor = ArgumentCaptor.forClass(HotelOrder.class);
        verify(orderMapper).insert(captor.capture());
        HotelOrder saved = captor.getValue();
        assertEquals(100L, saved.getUserId());
        assertEquals(1, saved.getGuestCount());
        assertEquals(OrderStatus.PENDING_PAYMENT.name(), saved.getStatus());
        assertEquals(0, saved.getTotalAmount().compareTo(new BigDecimal("299.00")));

        assertNotNull(vo.getOrderNo());
        assertEquals(OrderStatus.PENDING_PAYMENT.name(), vo.getStatus());
    }

    @Test
    void createOrderShouldRejectGuestCountExceedingMaxGuests() {
        request.setGuestCount(3);
        OrderGuestRequest guest2 = new OrderGuestRequest();
        guest2.setName("李四");
        guest2.setPhone("13900002222");
        OrderGuestRequest guest3 = new OrderGuestRequest();
        guest3.setName("王五");
        guest3.setPhone("13900003333");
        request.setGuests(List.of(request.getGuests().get(0), guest2, guest3));

        when(hotelMapper.selectById(1L)).thenReturn(hotel);
        when(roomTypeMapper.selectOne(any())).thenReturn(roomType);

        assertThrows(BusinessException.class, () -> orderService.createOrder(request));
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
