ALTER TABLE hotel_review
    ADD COLUMN merchant_reply VARCHAR(500) DEFAULT NULL COMMENT '商家回复' AFTER content,
    ADD COLUMN reply_at       DATETIME     DEFAULT NULL COMMENT '回复时间' AFTER merchant_reply;
