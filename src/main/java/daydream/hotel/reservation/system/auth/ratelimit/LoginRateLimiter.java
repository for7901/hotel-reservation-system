package daydream.hotel.reservation.system.auth.ratelimit;

public interface LoginRateLimiter {

    void check(String key);
}
