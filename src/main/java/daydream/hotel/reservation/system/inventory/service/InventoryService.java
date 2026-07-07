package daydream.hotel.reservation.system.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.hotel.entity.Hotel;
import daydream.hotel.reservation.system.hotel.entity.RoomType;
import daydream.hotel.reservation.system.hotel.enums.HotelStatus;
import daydream.hotel.reservation.system.hotel.mapper.HotelMapper;
import daydream.hotel.reservation.system.hotel.mapper.RoomTypeMapper;
import daydream.hotel.reservation.system.hotel.service.HotelService;
import daydream.hotel.reservation.system.inventory.dto.AvailabilityVO;
import daydream.hotel.reservation.system.inventory.dto.InventoryItemRequest;
import daydream.hotel.reservation.system.inventory.dto.InventoryItemVO;
import daydream.hotel.reservation.system.inventory.entity.InventoryCalendar;
import daydream.hotel.reservation.system.inventory.mapper.InventoryCalendarMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryCalendarMapper inventoryMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final HotelMapper hotelMapper;
    private final HotelService hotelService;

    public InventoryService(
            InventoryCalendarMapper inventoryMapper,
            RoomTypeMapper roomTypeMapper,
            HotelMapper hotelMapper,
            HotelService hotelService) {
        this.inventoryMapper = inventoryMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.hotelMapper = hotelMapper;
        this.hotelService = hotelService;
    }

    public List<InventoryItemVO> listInventory(
            Long hotelId, Long roomTypeId, LocalDate startDate, LocalDate endDate) {
        validateRoomType(hotelId, roomTypeId);
        return inventoryMapper
                .selectList(
                        new LambdaQueryWrapper<InventoryCalendar>()
                                .eq(InventoryCalendar::getRoomTypeId, roomTypeId)
                                .ge(InventoryCalendar::getInvDate, startDate)
                                .lt(InventoryCalendar::getInvDate, endDate)
                                .orderByAsc(InventoryCalendar::getInvDate))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public void saveInventory(Long hotelId, Long roomTypeId, List<InventoryItemRequest> items) {
        validateRoomType(hotelId, roomTypeId);
        for (InventoryItemRequest item : items) {
            InventoryCalendar existing =
                    inventoryMapper.selectOne(
                            new LambdaQueryWrapper<InventoryCalendar>()
                                    .eq(InventoryCalendar::getRoomTypeId, roomTypeId)
                                    .eq(InventoryCalendar::getInvDate, item.getInvDate()));
            if (existing != null) {
                existing.setPrice(item.getPrice());
                existing.setAvailableRooms(item.getAvailableRooms());
                inventoryMapper.updateById(existing);
            } else {
                InventoryCalendar calendar = new InventoryCalendar();
                calendar.setRoomTypeId(roomTypeId);
                calendar.setInvDate(item.getInvDate());
                calendar.setPrice(item.getPrice());
                calendar.setAvailableRooms(item.getAvailableRooms());
                inventoryMapper.insert(calendar);
            }
        }
        hotelService.refreshMinPrice(hotelId);
    }

    public AvailabilityVO checkAvailability(
            Long hotelId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        AvailabilityVO vo = new AvailabilityVO();
        if (!checkIn.isBefore(checkOut)) {
            vo.setAvailable(false);
            vo.setMessage("离店日期必须晚于入住日期");
            return vo;
        }
        if (checkIn.isBefore(LocalDate.now())) {
            vo.setAvailable(false);
            vo.setMessage("入住日期不能早于今天");
            return vo;
        }

        Hotel hotel = hotelMapper.selectById(hotelId);
        if (hotel == null || !HotelStatus.APPROVED.name().equals(hotel.getStatus())) {
            vo.setAvailable(false);
            vo.setMessage("酒店不可预订");
            return vo;
        }

        RoomType roomType =
                roomTypeMapper.selectOne(
                        new LambdaQueryWrapper<RoomType>()
                                .eq(RoomType::getId, roomTypeId)
                                .eq(RoomType::getHotelId, hotelId)
                                .eq(RoomType::getStatus, 1));
        if (roomType == null) {
            vo.setAvailable(false);
            vo.setMessage("房型不可预订");
            return vo;
        }

        try {
            PriceSummary summary = calculatePrice(roomType, checkIn, checkOut);
            vo.setAvailable(true);
            vo.setNights(summary.nights());
            vo.setTotalPrice(summary.totalPrice());
            vo.setUnitPrice(summary.unitPrice());
            vo.setMessage("可预订");
        } catch (BusinessException ex) {
            vo.setAvailable(false);
            vo.setMessage(ex.getMessage());
        }
        return vo;
    }

    public PriceSummary calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        List<LocalDate> dates = getStayDates(checkIn, checkOut);
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate date : dates) {
            InventoryCalendar inv =
                    inventoryMapper.selectOne(
                            new LambdaQueryWrapper<InventoryCalendar>()
                                    .eq(InventoryCalendar::getRoomTypeId, roomType.getId())
                                    .eq(InventoryCalendar::getInvDate, date));
            if (inv == null || inv.getAvailableRooms() == null || inv.getAvailableRooms() <= 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_INVENTORY);
            }
            total = total.add(inv.getPrice());
        }
        BigDecimal unitPrice =
                total.divide(BigDecimal.valueOf(dates.size()), 2, RoundingMode.HALF_UP);
        return new PriceSummary(dates.size(), total, unitPrice);
    }

    @Transactional
    public void reserveInventory(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        for (LocalDate date : getStayDates(checkIn, checkOut)) {
            int updated = inventoryMapper.decrementStock(roomTypeId, date.toString());
            if (updated == 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_INVENTORY);
            }
        }
    }

    @Transactional
    public void releaseInventory(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        for (LocalDate date : getStayDates(checkIn, checkOut)) {
            inventoryMapper.incrementStock(roomTypeId, date.toString());
        }
    }

    public static List<LocalDate> getStayDates(LocalDate checkIn, LocalDate checkOut) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    private void validateRoomType(Long hotelId, Long roomTypeId) {
        RoomType roomType =
                roomTypeMapper.selectOne(
                        new LambdaQueryWrapper<RoomType>()
                                .eq(RoomType::getId, roomTypeId)
                                .eq(RoomType::getHotelId, hotelId));
        if (roomType == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private InventoryItemVO toVO(InventoryCalendar item) {
        InventoryItemVO vo = new InventoryItemVO();
        vo.setInvDate(item.getInvDate());
        vo.setPrice(item.getPrice());
        vo.setAvailableRooms(item.getAvailableRooms());
        return vo;
    }

    public record PriceSummary(int nights, BigDecimal totalPrice, BigDecimal unitPrice) {}
}
