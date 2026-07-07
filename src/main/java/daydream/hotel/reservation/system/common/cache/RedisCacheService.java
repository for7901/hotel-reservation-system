package daydream.hotel.reservation.system.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Optional<StringRedisTemplate> redisTemplate;

    public RedisCacheService(Optional<StringRedisTemplate> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> T get(String key, TypeReference<T> type, Supplier<T> loader, int ttlSeconds) {
        if (redisTemplate.isEmpty()) {
            return loader.get();
        }
        try {
            String json = redisTemplate.get().opsForValue().get(key);
            if (json != null) {
                return OBJECT_MAPPER.readValue(json, type);
            }
        } catch (Exception e) {
            log.warn("Cache read failed for key {}, fallback to loader", key, e);
            redisTemplate.get().delete(key);
        }

        T value = loader.get();
        if (value != null) {
            try {
                redisTemplate
                        .get()
                        .opsForValue()
                        .set(
                                key,
                                OBJECT_MAPPER.writeValueAsString(value),
                                Duration.ofSeconds(ttlSeconds));
            } catch (Exception e) {
                log.warn("Cache write failed for key {}", key, e);
            }
        }
        return value;
    }

    @Override
    public void evict(String key) {
        redisTemplate.ifPresent(template -> template.delete(key));
    }
}
