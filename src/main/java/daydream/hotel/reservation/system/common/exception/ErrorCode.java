package daydream.hotel.reservation.system.common.exception;

public enum ErrorCode {
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    DUPLICATE_PHONE(1001, "手机号已注册"),
    INVALID_CREDENTIALS(1002, "手机号或密码错误"),
    USER_DISABLED(1003, "账号已被禁用"),
    HOTEL_NOT_FOUND(2001, "酒店不存在或已下架"),
    INSUFFICIENT_INVENTORY(2002, "库存不足"),
    INVALID_DATE_RANGE(2003, "日期范围无效"),
    ORDER_NOT_FOUND(2004, "订单不存在"),
    ORDER_STATUS_INVALID(2005, "订单状态不允许此操作"),
    TOO_MANY_REQUESTS(4290, "请求过于频繁，请稍后再试"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
