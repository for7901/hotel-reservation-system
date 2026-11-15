package daydream.hotel.reservation.system.auth.security;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 实现：{@code auth:token:version:<userId>} → 自增计数。
 *
 * <p>测试环境由 {@link InMemoryTokenVersionStore} 顶替（{@code test} profile 不加载 Redis）。
 */
@Component
@Profile("!test")
public class RedisTokenVersionStore implements TokenVersionStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenVersionStore.class);

    private static final String KEY_PREFIX = "auth:token:version:";

    private final Optional<StringRedisTemplate> redisTemplate;

    public RedisTokenVersionStore(Optional<StringRedisTemplate> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Long currentVersion(Long userId) {
        if (redisTemplate.isEmpty() || userId == null) {
            return null;
        }
        try {
            String value = redisTemplate.get().opsForValue().get(key(userId));
            return value == null ? 0L : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            // 值被人工改坏：当成未吊销，不要因此把用户全部踢下线
            log.warn("token 版本号格式异常，按 0 处理: userId={}", userId, ex);
            return 0L;
        } catch (Exception ex) {
            // Redis 故障：返回 null 让调用方跳过校验（fail-open），避免缓存故障升级成全局 401
            log.warn("读取 token 版本号失败，本次跳过吊销校验: userId={}", userId, ex);
            return null;
        }
    }

    @Override
    public void revokeAll(Long userId) {
        if (redisTemplate.isEmpty() || userId == null) {
            return;
        }
        try {
            // 故意不设 TTL：版本号一旦过期消失就会退回 0，已吊销的旧 Token 会"复活"
            redisTemplate.get().opsForValue().increment(key(userId));
            log.info("已吊销用户 {} 的全部 token", userId);
        } catch (Exception ex) {
            log.error("吊销 token 失败，旧 token 仍有效至过期: userId={}", userId, ex);
        }
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
