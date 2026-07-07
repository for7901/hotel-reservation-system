package daydream.hotel.reservation.system.user.enums;

public enum UserRole {
    USER,
    MERCHANT,
    ADMIN;

    public static boolean isAdmin(String role) {
        return ADMIN.name().equals(role);
    }

    public static boolean isMerchant(String role) {
        return MERCHANT.name().equals(role);
    }
}
