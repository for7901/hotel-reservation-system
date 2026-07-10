package daydream.hotel.reservation.system.order.task;

import daydream.hotel.reservation.system.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    private final OrderService orderService;

    public OrderTimeoutTask(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${app.order.timeout-check-interval-ms:60000}")
    public void cancelExpiredOrders() {
        int count = orderService.cancelExpiredPendingOrders();
        if (count > 0) {
            log.info("Auto-cancelled {} expired pending-payment orders", count);
        }
    }

    /** 离店日到达后，自动将已支付订单标记为已完成 */
    @Scheduled(fixedDelayString = "${app.order.timeout-check-interval-ms:60000}")
    public void autoCompleteDueOrders() {
        int count = orderService.autoCompleteDueOrders();
        if (count > 0) {
            log.info("Auto-completed {} orders past check-out date", count);
        }
    }
}
