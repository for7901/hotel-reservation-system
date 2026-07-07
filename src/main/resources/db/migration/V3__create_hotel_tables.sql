CREATE TABLE IF NOT EXISTS city (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50)  NOT NULL,
    code       VARCHAR(20)  DEFAULT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_city_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hotel (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    merchant_id  BIGINT        NOT NULL,
    city_id      BIGINT        NOT NULL,
    name         VARCHAR(100)  NOT NULL,
    address      VARCHAR(255)  NOT NULL,
    star_rating  TINYINT       NOT NULL DEFAULT 3,
    cover_image  VARCHAR(500)  DEFAULT NULL,
    description  TEXT,
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(255) DEFAULT NULL,
    min_price    DECIMAL(10,2) DEFAULT NULL,
    score        DECIMAL(3,1)  DEFAULT 4.5,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_hotel_city (city_id),
    KEY idx_hotel_merchant (merchant_id),
    KEY idx_hotel_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS room_type (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    hotel_id    BIGINT        NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    bed_type    VARCHAR(50)   NOT NULL,
    area        INT           DEFAULT NULL,
    max_guests  TINYINT       NOT NULL DEFAULT 2,
    base_price  DECIMAL(10,2) NOT NULL,
    cover_image VARCHAR(500)  DEFAULT NULL,
    description VARCHAR(500)  DEFAULT NULL,
    breakfast   TINYINT       NOT NULL DEFAULT 0 COMMENT '0无早 1含早',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_room_hotel (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hotel_facility (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    hotel_id   BIGINT      NOT NULL,
    name       VARCHAR(50) NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_facility_hotel (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
