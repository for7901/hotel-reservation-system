-- 预订间数；入住人手机改为可选（订单级联系电话复用 guest_phone）
ALTER TABLE hotel_order
    ADD COLUMN room_count INT NOT NULL DEFAULT 1 COMMENT '预订间数' AFTER guest_count;

ALTER TABLE order_guest
    MODIFY COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '可选；订单联系电话存 hotel_order.guest_phone';
