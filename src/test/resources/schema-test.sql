CREATE TABLE IF NOT EXISTS sys_user (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    phone      VARCHAR(20)  NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    nickname   VARCHAR(50)  DEFAULT NULL,
    avatar     VARCHAR(255) DEFAULT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status     TINYINT      NOT NULL DEFAULT 1,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    TINYINT      NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS province (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS city (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    province_id BIGINT       DEFAULT NULL,
    name        VARCHAR(50)  NOT NULL,
    code        VARCHAR(20)  DEFAULT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (province_id, name)
);

CREATE TABLE IF NOT EXISTS hotel (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id   BIGINT        NOT NULL,
    city_id       BIGINT        NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    address       VARCHAR(255)  NOT NULL,
    star_rating   TINYINT       NOT NULL DEFAULT 3,
    cover_image   VARCHAR(500)  DEFAULT NULL,
    description   CLOB,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(255)  DEFAULT NULL,
    min_price     DECIMAL(10,2) DEFAULT NULL,
    score         DECIMAL(3,1)  DEFAULT 4.5,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS room_type (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hotel_id    BIGINT        NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    bed_type    VARCHAR(50)   NOT NULL,
    area        INT           DEFAULT NULL,
    max_guests  TINYINT       NOT NULL DEFAULT 2,
    base_price  DECIMAL(10,2) NOT NULL,
    cover_image VARCHAR(500)  DEFAULT NULL,
    description VARCHAR(500)  DEFAULT NULL,
    breakfast   TINYINT       NOT NULL DEFAULT 0,
    status      TINYINT       NOT NULL DEFAULT 1,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS hotel_facility (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hotel_id   BIGINT      NOT NULL,
    name       VARCHAR(50) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory_calendar (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    room_type_id    BIGINT        NOT NULL,
    inv_date        DATE          NOT NULL,
    price           DECIMAL(10,2) NOT NULL,
    available_rooms INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hotel_order (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(32)   NOT NULL,
    user_id         BIGINT        NOT NULL,
    hotel_id        BIGINT        NOT NULL,
    room_type_id    BIGINT        NOT NULL,
    hotel_name      VARCHAR(100)  NOT NULL,
    room_type_name  VARCHAR(100)  NOT NULL,
    check_in_date   DATE          NOT NULL,
    check_out_date  DATE          NOT NULL,
    nights          INT           NOT NULL,
    guest_count     INT           NOT NULL DEFAULT 1,
    guest_name      VARCHAR(50)   NOT NULL,
    guest_phone     VARCHAR(20)   NOT NULL,
    unit_price      DECIMAL(10,2) NOT NULL,
    total_amount    DECIMAL(10,2) NOT NULL,
    coupon_id       BIGINT        DEFAULT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING_PAYMENT',
    reject_reason   VARCHAR(255)  DEFAULT NULL,
    paid_at         TIMESTAMP     DEFAULT NULL,
    cancelled_at    TIMESTAMP     DEFAULT NULL,
    checkout_apply_at TIMESTAMP   DEFAULT NULL,
    refund_amount   DECIMAL(10,2) DEFAULT NULL,
    refund_policy   VARCHAR(255)  DEFAULT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_guest (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT       NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    id_card    VARCHAR(18)  DEFAULT NULL,
    sort_order TINYINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hotel_review (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    hotel_id        BIGINT       NOT NULL,
    order_id        BIGINT       NOT NULL,
    user_nickname   VARCHAR(50)  DEFAULT NULL,
    rating          TINYINT      NOT NULL,
    content         VARCHAR(500) DEFAULT NULL,
    merchant_reply  VARCHAR(500) DEFAULT NULL,
    reply_at        TIMESTAMP    DEFAULT NULL,
    status          TINYINT      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    operator_id     BIGINT       NOT NULL,
    operator_name   VARCHAR(50)  DEFAULT NULL,
    operator_role   VARCHAR(20)  NOT NULL,
    action          VARCHAR(50)  NOT NULL,
    target_type     VARCHAR(50)  DEFAULT NULL,
    target_id       BIGINT       DEFAULT NULL,
    detail          VARCHAR(500) DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_favorite (
    id              BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT   NOT NULL,
    hotel_id        BIGINT   NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS banner (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(100)  NOT NULL,
    image_url       VARCHAR(500)  NOT NULL,
    link_url        VARCHAR(500)  DEFAULT NULL,
    sort_order      INT           NOT NULL DEFAULT 0,
    status          TINYINT       NOT NULL DEFAULT 1,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coupon (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL,
    coupon_type     VARCHAR(20)   NOT NULL DEFAULT 'FIXED',
    amount          DECIMAL(10,2) NOT NULL,
    min_amount      DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_count     INT           NOT NULL DEFAULT 100,
    claimed_count   INT           NOT NULL DEFAULT 0,
    start_time      TIMESTAMP     DEFAULT NULL,
    end_time        TIMESTAMP     DEFAULT NULL,
    status          TINYINT       NOT NULL DEFAULT 1,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_coupon (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    coupon_id       BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'UNUSED',
    used_at         TIMESTAMP    DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
