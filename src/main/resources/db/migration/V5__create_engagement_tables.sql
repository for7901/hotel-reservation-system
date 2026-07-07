CREATE TABLE IF NOT EXISTS hotel_review (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    hotel_id        BIGINT       NOT NULL,
    order_id        BIGINT       NOT NULL,
    user_nickname   VARCHAR(50)  DEFAULT NULL,
    rating          TINYINT      NOT NULL,
    content         VARCHAR(500) DEFAULT NULL,
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1展示 0隐藏',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_order (order_id),
    KEY idx_review_hotel (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    operator_id     BIGINT       NOT NULL,
    operator_name   VARCHAR(50)  DEFAULT NULL,
    operator_role   VARCHAR(20)  NOT NULL,
    action          VARCHAR(50)  NOT NULL,
    target_type     VARCHAR(50)  DEFAULT NULL,
    target_id       BIGINT       DEFAULT NULL,
    detail          VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_favorite (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    user_id         BIGINT   NOT NULL,
    hotel_id        BIGINT   NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_hotel (user_id, hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS banner (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    title           VARCHAR(100)  NOT NULL,
    image_url       VARCHAR(500)  NOT NULL,
    link_url        VARCHAR(500)  DEFAULT NULL,
    sort_order      INT           NOT NULL DEFAULT 0,
    status          TINYINT       NOT NULL DEFAULT 1,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS coupon (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)  NOT NULL,
    coupon_type     VARCHAR(20)   NOT NULL DEFAULT 'FIXED',
    amount          DECIMAL(10,2) NOT NULL,
    min_amount      DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_count     INT           NOT NULL DEFAULT 100,
    claimed_count   INT           NOT NULL DEFAULT 0,
    start_time      DATETIME      DEFAULT NULL,
    end_time        DATETIME      DEFAULT NULL,
    status          TINYINT       NOT NULL DEFAULT 1,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_coupon (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    coupon_id       BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'UNUSED',
    used_at         DATETIME     DEFAULT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_coupon (user_id, coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE hotel_order
    ADD COLUMN coupon_id BIGINT DEFAULT NULL,
    ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0;
