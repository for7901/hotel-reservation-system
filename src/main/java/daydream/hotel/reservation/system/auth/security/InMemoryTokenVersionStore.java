package daydream.hotel.reservation.system.auth.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 内存实现，仅用于 {@code test} profile：让吊销流程在无 Redis 的环境下也能被测试覆盖。 */
@Component
@Profile("test")
public class InMemoryTokenVersionStore implements TokenVersionStore {

    private final Map<Long, AtomicLong> versions = new ConcurrentHashMap<>();

    @Override
    public Long currentVersion(Long userId) {
        if (userId == null) {
            return null;
        }
        AtomicLong version = versions.get(userId);
        return version == null ? 0L : version.get();
    }

    @Override
    public void revokeAll(Long userId) {
        if (userId == null) {
            return;
        }
        versions.computeIfAbsent(userId, key -> new AtomicLong()).incrementAndGet();
    }
}
