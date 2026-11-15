package daydream.hotel.reservation.system.inventory.task;

import daydream.hotel.reservation.system.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class InventoryMaintenanceTask {

    private static final Logger log = LoggerFactory.getLogger(InventoryMaintenanceTask.class);

    private final InventoryService inventoryService;

    public InventoryMaintenanceTask(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** 每天凌晨补全未来 60 天缺失库存，默认每天 10 间。 */
    @Scheduled(cron = "0 10 0 * * ?")
    public void extendDefaultInventory() {
        try {
            int created =
                    inventoryService.ensureDefaultInventoryForAllRoomTypes(
                            InventoryService.DEFAULT_INVENTORY_DAYS,
                            InventoryService.DEFAULT_AVAILABLE_ROOMS);
            if (created > 0) {
                log.info("Extended default inventory with {} new room-day records", created);
            }
        } catch (Exception ex) {
            log.warn("Failed to extend default inventory: {}", ex.getMessage());
        }
    }
}
