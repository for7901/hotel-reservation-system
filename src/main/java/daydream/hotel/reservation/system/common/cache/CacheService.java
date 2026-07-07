package daydream.hotel.reservation.system.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.function.Supplier;

public interface CacheService {

    <T> T get(String key, TypeReference<T> type, Supplier<T> loader, int ttlSeconds);

    void evict(String key);
}
