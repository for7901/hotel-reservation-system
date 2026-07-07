package daydream.hotel.reservation.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import daydream.hotel.reservation.system.hotel.entity.RoomType;
import daydream.hotel.reservation.system.hotel.mapper.RoomTypeMapper;
import daydream.hotel.reservation.system.inventory.entity.InventoryCalendar;
import daydream.hotel.reservation.system.inventory.mapper.InventoryCalendarMapper;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(4)
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
            List<RoomType> roomTypes = roomTypeMapper.selectList(null);
            if (roomTypes.isEmpty()) {
                return;
            }
            Set<Long> seededRoomTypeIds = loadSeededRoomTypeIds();
            LocalDate today = LocalDate.now();
            int created = 0;
            for (RoomType roomType : roomTypes) {
                if (seededRoomTypeIds.contains(roomType.getId())) {
                    continue;
                }
                for (int i = 0; i < 30; i++) {
                    InventoryCalendar item = new InventoryCalendar();
                    item.setRoomTypeId(roomType.getId());
                    item.setInvDate(today.plusDays(i));
                    item.setPrice(roomType.getBasePrice());
                    item.setAvailableRooms(10);
                    inventoryMapper.insert(item);
                    created++;
                }
            }
            if (created > 0) {
                log.info("Initialized demo inventory for {} room-day records", created);
            }
        } catch (Exception ex) {
            log.warn("Skip inventory seed: {}", ex.getMessage());
        }
    }

    private Set<Long> loadSeededRoomTypeIds() {
        List<InventoryCalendar> existing =
                inventoryMapper.selectList(
                        new LambdaQueryWrapper<InventoryCalendar>().select(InventoryCalendar::getRoomTypeId));
        Set<Long> ids = new HashSet<>();
        for (InventoryCalendar item : existing) {
            ids.add(item.getRoomTypeId());
        }
        return ids;
    }
}
