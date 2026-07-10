package daydream.hotel.reservation.system.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.hotel.entity.RoomType;
import daydream.hotel.reservation.system.hotel.mapper.HotelMapper;
import daydream.hotel.reservation.system.hotel.mapper.RoomTypeMapper;
import daydream.hotel.reservation.system.hotel.service.HotelService;
import daydream.hotel.reservation.system.inventory.entity.InventoryCalendar;
import daydream.hotel.reservation.system.inventory.mapper.InventoryCalendarMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryCalendarMapper inventoryMapper;

    @Mock private RoomTypeMapper roomTypeMapper;

    @Mock private HotelMapper hotelMapper;

    @Mock private HotelService hotelService;

    @InjectMocks private InventoryService inventoryService;

    private RoomType roomType;
    private LocalDate checkIn;
    private LocalDate checkOut;

    @BeforeEach
    void setUp() {
        roomType = new RoomType();
        roomType.setId(1L);
        roomType.setHotelId(10L);
        checkIn = LocalDate.of(2026, 8, 1);
        checkOut = LocalDate.of(2026, 8, 3);
    }

    @Test
    void calculatePriceShouldSumInventoryPrices() {
        InventoryCalendar day1 = inventory("2026-08-01", "299.00", 3);
        InventoryCalendar day2 = inventory("2026-08-02", "399.00", 2);
        when(inventoryMapper.selectOne(any())).thenReturn(day1, day2);

        InventoryService.PriceSummary summary =
                inventoryService.calculatePrice(roomType, checkIn, checkOut);

        assertEquals(2, summary.nights());
        assertEquals(0, summary.totalPrice().compareTo(new BigDecimal("698.00")));
        assertEquals(0, summary.unitPrice().compareTo(new BigDecimal("349.00")));
    }

    @Test
    void calculatePriceShouldFailWhenInventoryInsufficient() {
        InventoryCalendar day1 = inventory("2026-08-01", "299.00", 0);
        when(inventoryMapper.selectOne(any())).thenReturn(day1);

        assertThrows(
                BusinessException.class,
                () -> inventoryService.calculatePrice(roomType, checkIn, checkOut));
    }

    @Test
    void reserveInventoryShouldDecrementEachStayDate() {
        when(inventoryMapper.decrementStock(any(), any(), eq(1))).thenReturn(1);

        inventoryService.reserveInventory(roomType.getId(), checkIn, checkOut);

        verify(inventoryMapper).decrementStock(eq(1L), eq("2026-08-01"), eq(1));
        verify(inventoryMapper).decrementStock(eq(1L), eq("2026-08-02"), eq(1));
    }

    @Test
    void reserveInventoryShouldFailWhenDecrementReturnsZero() {
        when(inventoryMapper.decrementStock(any(), any(), eq(1))).thenReturn(0);

        assertThrows(
                BusinessException.class,
                () -> inventoryService.reserveInventory(roomType.getId(), checkIn, checkOut));
    }

    @Test
    void releaseInventoryShouldIncrementEachStayDate() {
        inventoryService.releaseInventory(roomType.getId(), checkIn, checkOut);

        verify(inventoryMapper).incrementStock(eq(1L), eq("2026-08-01"), eq(1));
        verify(inventoryMapper).incrementStock(eq(1L), eq("2026-08-02"), eq(1));
    }

    @Test
    void getStayDatesShouldExcludeCheckOutDate() {
        List<LocalDate> dates = InventoryService.getStayDates(checkIn, checkOut);
        assertEquals(List.of(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2)), dates);
    }

    private InventoryCalendar inventory(String date, String price, int rooms) {
        InventoryCalendar inv = new InventoryCalendar();
        inv.setRoomTypeId(roomType.getId());
        inv.setInvDate(LocalDate.parse(date));
        inv.setPrice(new BigDecimal(price));
        inv.setAvailableRooms(rooms);
        return inv;
    }
}
