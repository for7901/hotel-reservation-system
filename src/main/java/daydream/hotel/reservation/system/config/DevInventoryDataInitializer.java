package daydream.hotel.reservation.system.config;

import daydream.hotel.reservation.system.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(5)
public class DevInventoryDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevInventoryDataInitializer.class);

    private final InventoryService inventoryService;

    public DevInventoryDataInitializer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int created =
                    inventoryService.ensureDefaultInventoryForAllRoomTypes(
                            InventoryService.DEFAULT_INVENTORY_DAYS,
                            InventoryService.DEFAULT_AVAILABLE_ROOMS);
            if (created > 0) {
                log.info(
                        "Initialized {} default inventory records ({} rooms/day)",
                        created,
                        InventoryService.DEFAULT_AVAILABLE_ROOMS);
            }
        } catch (Exception ex) {
            log.warn("Skip inventory seed: {}", ex.getMessage());
        }
    }
}
