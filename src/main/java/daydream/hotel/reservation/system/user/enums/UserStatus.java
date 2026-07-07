package daydream.hotel.reservation.system.user.enums;

public enum UserStatus {
    DISABLED(0),
    ACTIVE(1);

    private final int value;

    UserStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
