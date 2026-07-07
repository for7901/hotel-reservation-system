CREATE TABLE IF NOT EXISTS inventory_calendar (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    room_type_id    BIGINT        NOT NULL,
    inv_date        DATE          NOT NULL,
    price           DECIMAL(10,2) NOT NULL,
    available_rooms INT           NOT NULL DEFAULT 0,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_date (room_type_id, inv_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hotel_order (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    order_no        VARCHAR(32)   NOT NULL,
    user_id         BIGINT        NOT NULL,
    hotel_id        BIGINT        NOT NULL,
    room_type_id    BIGINT        NOT NULL,
    hotel_name      VARCHAR(100)  NOT NULL,
    room_type_name  VARCHAR(100)  NOT NULL,
    check_in_date   DATE          NOT NULL,
    check_out_date  DATE          NOT NULL,
    nights          INT           NOT NULL,
    guest_name      VARCHAR(50)   NOT NULL,
    guest_phone     VARCHAR(20)   NOT NULL,
    unit_price      DECIMAL(10,2) NOT NULL COMMENT '每晚均价快照',
    total_amount    DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING_PAYMENT',
    paid_at         DATETIME      DEFAULT NULL,
    cancelled_at    DATETIME      DEFAULT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_user (user_id),
    KEY idx_order_hotel (hotel_id),
    KEY idx_order_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
