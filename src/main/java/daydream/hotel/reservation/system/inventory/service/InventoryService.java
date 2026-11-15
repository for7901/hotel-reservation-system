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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    public static final int DEFAULT_AVAILABLE_ROOMS = 10;
    public static final int DEFAULT_INVENTORY_DAYS = 60;

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
        RoomType roomType = getRoomType(hotelId, roomTypeId);
        Map<LocalDate, InventoryItemVO> byDate = new LinkedHashMap<>();
        inventoryMapper
                .selectList(
                        new LambdaQueryWrapper<InventoryCalendar>()
                                .eq(InventoryCalendar::getRoomTypeId, roomTypeId)
                                .ge(InventoryCalendar::getInvDate, startDate)
                                .lt(InventoryCalendar::getInvDate, endDate)
                                .orderByAsc(InventoryCalendar::getInvDate))
                .forEach(item -> byDate.put(item.getInvDate(), toVO(item)));

        List<InventoryItemVO> result = new ArrayList<>();
        LocalDate current = startDate;
        while (current.isBefore(endDate)) {
            InventoryItemVO item = byDate.get(current);
            if (item == null) {
                item = defaultInventoryItem(current, roomType.getBasePrice());
            }
            result.add(item);
            current = current.plusDays(1);
        }
        return result;
    }

    @Transactional
    public int ensureDefaultInventory(Long roomTypeId, LocalDate startDate, int days) {
        return ensureDefaultInventory(roomTypeId, startDate, days, DEFAULT_AVAILABLE_ROOMS);
    }

    @Transactional
    public int ensureDefaultInventory(
            Long roomTypeId, LocalDate startDate, int days, int defaultAvailableRooms) {
        RoomType roomType = roomTypeMapper.selectById(roomTypeId);
        if (roomType == null || roomType.getStatus() == null || roomType.getStatus() != 1) {
            return 0;
        }
        int quantity = Math.max(defaultAvailableRooms, 0);
        int created = 0;
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            InventoryCalendar existing =
                    inventoryMapper.selectOne(
                            new LambdaQueryWrapper<InventoryCalendar>()
                                    .eq(InventoryCalendar::getRoomTypeId, roomTypeId)
                                    .eq(InventoryCalendar::getInvDate, date));
            if (existing != null) {
                continue;
            }
            InventoryCalendar calendar = new InventoryCalendar();
            calendar.setRoomTypeId(roomTypeId);
            calendar.setInvDate(date);
            calendar.setPrice(roomType.getBasePrice());
            calendar.setAvailableRooms(quantity);
            inventoryMapper.insert(calendar);
            created++;
        }
        if (created > 0) {
            hotelService.refreshMinPrice(roomType.getHotelId());
        }
        return created;
    }

    @Transactional
    public int ensureDefaultInventoryForAllRoomTypes(int days) {
        return ensureDefaultInventoryForAllRoomTypes(days, DEFAULT_AVAILABLE_ROOMS);
    }

    @Transactional
    public int ensureDefaultInventoryForAllRoomTypes(int days, int defaultAvailableRooms) {
        List<RoomType> roomTypes =
                roomTypeMapper.selectList(
                        new LambdaQueryWrapper<RoomType>().eq(RoomType::getStatus, 1));
        LocalDate today = LocalDate.now();
        int total = 0;
        for (RoomType roomType : roomTypes) {
            total += ensureDefaultInventory(roomType.getId(), today, days, defaultAvailableRooms);
        }
        return total;
    }

    @Transactional
    public void saveInventory(Long hotelId, Long roomTypeId, List<InventoryItemRequest> items) {
        getRoomType(hotelId, roomTypeId);
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
        return checkAvailability(hotelId, roomTypeId, checkIn, checkOut, 1);
    }

    public AvailabilityVO checkAvailability(
            Long hotelId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int roomCount) {
        AvailabilityVO vo = new AvailabilityVO();
        int quantity = Math.max(roomCount, 1);
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
            PriceSummary summary = calculatePrice(roomType, checkIn, checkOut, quantity);
            vo.setAvailable(true);
            vo.setNights(summary.nights());
            vo.setTotalPrice(summary.totalPrice());
            vo.setUnitPrice(summary.unitPrice());
            vo.setAvailableRooms(summary.availableRooms());
            vo.setMessage("可预订");
        } catch (BusinessException ex) {
            vo.setAvailable(false);
            vo.setMessage(ex.getMessage());
        }
        return vo;
    }

    public PriceSummary calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        return calculatePrice(roomType, checkIn, checkOut, 1);
    }

    public PriceSummary calculatePrice(
            RoomType roomType, LocalDate checkIn, LocalDate checkOut, int roomCount) {
        int quantity = Math.max(roomCount, 1);
        List<LocalDate> dates = getStayDates(checkIn, checkOut);
        BigDecimal total = BigDecimal.ZERO;
        int minAvailable = Integer.MAX_VALUE;
        for (LocalDate date : dates) {
            InventoryCalendar inv =
                    inventoryMapper.selectOne(
                            new LambdaQueryWrapper<InventoryCalendar>()
                                    .eq(InventoryCalendar::getRoomTypeId, roomType.getId())
                                    .eq(InventoryCalendar::getInvDate, date));
            if (inv == null
                    || inv.getAvailableRooms() == null
                    || inv.getAvailableRooms() < quantity) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_INVENTORY);
            }
            minAvailable = Math.min(minAvailable, inv.getAvailableRooms());
            total = total.add(inv.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        BigDecimal unitPrice =
                total.divide(
                        BigDecimal.valueOf((long) dates.size() * quantity),
                        2,
                        RoundingMode.HALF_UP);
        return new PriceSummary(dates.size(), total, unitPrice, minAvailable);
    }

    @Transactional
    public void reserveInventory(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        reserveInventory(roomTypeId, checkIn, checkOut, 1);
    }

    @Transactional
    public void reserveInventory(
            Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int roomCount) {
        int quantity = Math.max(roomCount, 1);
        for (LocalDate date : getStayDates(checkIn, checkOut)) {
            int updated = inventoryMapper.decrementStock(roomTypeId, date.toString(), quantity);
            if (updated == 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_INVENTORY);
            }
        }
    }

    @Transactional
    public void releaseInventory(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        releaseInventory(roomTypeId, checkIn, checkOut, 1);
    }

    @Transactional
    public void releaseInventory(
            Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int roomCount) {
        int quantity = Math.max(roomCount, 1);
        for (LocalDate date : getStayDates(checkIn, checkOut)) {
            inventoryMapper.incrementStock(roomTypeId, date.toString(), quantity);
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

    private RoomType getRoomType(Long hotelId, Long roomTypeId) {
        RoomType roomType =
                roomTypeMapper.selectOne(
                        new LambdaQueryWrapper<RoomType>()
                                .eq(RoomType::getId, roomTypeId)
                                .eq(RoomType::getHotelId, hotelId));
        if (roomType == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return roomType;
    }

    private InventoryItemVO defaultInventoryItem(LocalDate date, BigDecimal basePrice) {
        InventoryItemVO vo = new InventoryItemVO();
        vo.setInvDate(date);
        vo.setPrice(basePrice);
        vo.setAvailableRooms(DEFAULT_AVAILABLE_ROOMS);
        return vo;
    }

    private InventoryItemVO toVO(InventoryCalendar item) {
        InventoryItemVO vo = new InventoryItemVO();
        vo.setInvDate(item.getInvDate());
        vo.setPrice(item.getPrice());
        vo.setAvailableRooms(item.getAvailableRooms());
        return vo;
    }

    public record PriceSummary(
            int nights, BigDecimal totalPrice, BigDecimal unitPrice, int availableRooms) {
        public PriceSummary(int nights, BigDecimal totalPrice, BigDecimal unitPrice) {
            this(nights, totalPrice, unitPrice, 0);
        }
    }
}
