package daydream.hotel.reservation.system.common.util;

public final class SensitiveUtils {

    private SensitiveUtils() {}

    /** 11 位手机号中间四位脱敏，如 13812345678 → 138****5678 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
