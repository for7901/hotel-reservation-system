CREATE TABLE IF NOT EXISTS province (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50)  NOT NULL,
    code       VARCHAR(20)  NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_province_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE city
    ADD COLUMN province_id BIGINT NULL AFTER id,
    ADD KEY idx_city_province (province_id);

ALTER TABLE city DROP INDEX uk_city_name;
ALTER TABLE city ADD UNIQUE KEY uk_city_province_name (province_id, name);
