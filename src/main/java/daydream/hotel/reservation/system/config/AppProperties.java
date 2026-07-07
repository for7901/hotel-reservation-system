package daydream.hotel.reservation.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cache cache = new Cache();
    private Order order = new Order();
    private RateLimit rateLimit = new RateLimit();

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class Cache {
        private int bannerTtlSeconds = 600;
        private int cityTtlSeconds = 1800;

        public int getBannerTtlSeconds() {
            return bannerTtlSeconds;
        }

        public void setBannerTtlSeconds(int bannerTtlSeconds) {
            this.bannerTtlSeconds = bannerTtlSeconds;
        }

        public int getCityTtlSeconds() {
            return cityTtlSeconds;
        }

        public void setCityTtlSeconds(int cityTtlSeconds) {
            this.cityTtlSeconds = cityTtlSeconds;
        }
    }

    public static class Order {
        private int paymentTimeoutMinutes = 30;
        private long timeoutCheckIntervalMs = 60_000L;

        public int getPaymentTimeoutMinutes() {
            return paymentTimeoutMinutes;
        }

        public void setPaymentTimeoutMinutes(int paymentTimeoutMinutes) {
            this.paymentTimeoutMinutes = paymentTimeoutMinutes;
        }

        public long getTimeoutCheckIntervalMs() {
            return timeoutCheckIntervalMs;
        }

        public void setTimeoutCheckIntervalMs(long timeoutCheckIntervalMs) {
            this.timeoutCheckIntervalMs = timeoutCheckIntervalMs;
        }
    }

    public static class RateLimit {
        private int loginMaxAttempts = 5;
        private int loginWindowSeconds = 60;

        public int getLoginMaxAttempts() {
            return loginMaxAttempts;
        }

        public void setLoginMaxAttempts(int loginMaxAttempts) {
            this.loginMaxAttempts = loginMaxAttempts;
        }

        public int getLoginWindowSeconds() {
            return loginWindowSeconds;
        }

        public void setLoginWindowSeconds(int loginWindowSeconds) {
            this.loginWindowSeconds = loginWindowSeconds;
        }
    }
}
