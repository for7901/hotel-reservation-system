package daydream.hotel.reservation.system.auth.ratelimit;

import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.config.AppProperties;
import java.time.Duration;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RedisLoginRateLimiter implements LoginRateLimiter {

    private static final String KEY_PREFIX = "rate:login:";

    private final Optional<StringRedisTemplate> redisTemplate;
    private final AppProperties appProperties;

    public RedisLoginRateLimiter(
            Optional<StringRedisTemplate> redisTemplate, AppProperties appProperties) {
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public void check(String key) {
        if (redisTemplate.isEmpty()) {
            return;
        }
        String redisKey = KEY_PREFIX + key;
        Long count = redisTemplate.get().opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate
                    .get()
                    .expire(
                            redisKey,
                            Duration.ofSeconds(
                                    appProperties.getRateLimit().getLoginWindowSeconds()));
        }
        if (count != null && count > appProperties.getRateLimit().getLoginMaxAttempts()) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
