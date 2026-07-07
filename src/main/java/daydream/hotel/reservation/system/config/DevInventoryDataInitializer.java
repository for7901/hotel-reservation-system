package daydream.hotel.reservation.system.config;

import daydream.hotel.reservation.system.hotel.entity.RoomType;
import daydream.hotel.reservation.system.hotel.mapper.RoomTypeMapper;
import daydream.hotel.reservation.system.inventory.entity.InventoryCalendar;
import daydream.hotel.reservation.system.inventory.mapper.InventoryCalendarMapper;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(3)
public class DevInventoryDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevInventoryDataInitializer.class);

    private final InventoryCalendarMapper inventoryMapper;
    private final RoomTypeMapper roomTypeMapper;

    public DevInventoryDataInitializer(
            InventoryCalendarMapper inventoryMapper, RoomTypeMapper roomTypeMapper) {
        this.inventoryMapper = inventoryMapper;
        this.roomTypeMapper = roomTypeMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (inventoryMapper.selectCount(null) > 0) {
                return;
            }
            List<RoomType> roomTypes = roomTypeMapper.selectList(null);
            LocalDate today = LocalDate.now();
            for (RoomType roomType : roomTypes) {
                for (int i = 0; i < 30; i++) {
                    InventoryCalendar item = new InventoryCalendar();
                    item.setRoomTypeId(roomType.getId());
                    item.setInvDate(today.plusDays(i));
                    item.setPrice(roomType.getBasePrice());
                    item.setAvailableRooms(10);
                    inventoryMapper.insert(item);
                }
            }
            log.info("Initialized demo inventory for {} room types", roomTypes.size());
        } catch (Exception ex) {
            log.warn("Skip inventory seed: {}", ex.getMessage());
        }
    }
}
