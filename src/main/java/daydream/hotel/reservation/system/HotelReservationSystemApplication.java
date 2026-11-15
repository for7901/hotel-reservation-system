package daydream.hotel.reservation.system;

import daydream.hotel.reservation.system.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 酒店预订系统后端启动入口。
 *
 * <p>技术栈概览：Web/Validation（REST）、Security/JJWT（鉴权）、MyBatis-Plus/JDBC（数据访问）、
 * Redis（缓存/限流）、Flyway（库表迁移）；可通过 profile 切换 OpenGauss。
 */
@SpringBootApplication
@EnableScheduling // 启用定时任务（如订单超时取消、库存维护）
@EnableConfigurationProperties(AppProperties.class) // 绑定 application.yml 中 app.* 配置
public class HotelReservationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelReservationSystemApplication.class, args);
    }
}
