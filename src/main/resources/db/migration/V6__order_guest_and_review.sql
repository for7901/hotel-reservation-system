ALTER TABLE hotel_order
    ADD COLUMN guest_count   INT          NOT NULL DEFAULT 1 COMMENT '入住人数' AFTER nights,
    ADD COLUMN reject_reason VARCHAR(255) DEFAULT NULL COMMENT '入住审核拒绝原因' AFTER status;

CREATE TABLE IF NOT EXISTS order_guest (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    order_id   BIGINT       NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    id_card    VARCHAR(18)  DEFAULT NULL COMMENT '身份证号',
    sort_order TINYINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_guest_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
