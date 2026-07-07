package daydream.hotel.reservation.system.auth.ratelimit;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class NoOpLoginRateLimiter implements LoginRateLimiter {

    @Override
    public void check(String key) {
        // 测试环境跳过 Redis 限流
    }
}
