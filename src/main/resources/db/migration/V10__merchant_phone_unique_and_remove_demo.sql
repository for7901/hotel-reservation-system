-- 商家手机号唯一：仅对未删除的 MERCHANT 账号生效（MySQL 8.0.13+ 函数索引）
-- 注：sys_user.phone 已有全局 uk_phone；此索引显式约束商家账号不可重复
CREATE UNIQUE INDEX uk_merchant_phone_active
    ON sys_user ((IF(role = 'MERCHANT' AND deleted = 0, phone, NULL)));

-- 删除演示商家 13800000001 及其名下酒店相关数据
SET @demo_merchant_id := (
    SELECT id FROM sys_user
    WHERE phone = '13800000001' AND role = 'MERCHANT'
    LIMIT 1
);

DELETE ic FROM inventory_calendar ic
    INNER JOIN room_type rt ON ic.room_type_id = rt.id
    INNER JOIN hotel h ON rt.hotel_id = h.id
    WHERE @demo_merchant_id IS NOT NULL AND h.merchant_id = @demo_merchant_id;

DELETE og FROM order_guest og
    INNER JOIN hotel_order o ON og.order_id = o.id
    INNER JOIN hotel h ON o.hotel_id = h.id
    WHERE @demo_merchant_id IS NOT NULL AND h.merchant_id = @demo_merchant_id;

DELETE o FROM hotel_order o
    INNER JOIN hotel h ON o.hotel_id = h.id
    WHERE @demo_merchant_id IS NOT NULL AND h.merchant_id = @demo_merchant_id;

DELETE r FROM hotel_review r
    INNER JOIN hotel h ON r.hotel_id = h.id
    WHERE @demo_merchant_id IS NOT NULL AND h.merchant_id = @demo_merchant_id;

DELETE f FROM hotel_facility f
    INNER JOIN hotel h ON f.hotel_id = h.id
    WHERE @demo_merchant_id IS NOT NULL AND h.merchant_id = @demo_merchant_id;

DELETE rt FROM room_type rt
    INNER JOIN hotel h ON rt.hotel_id = h.id
    WHERE @demo_merchant_id IS NOT NULL AND h.merchant_id = @demo_merchant_id;

DELETE FROM hotel
    WHERE @demo_merchant_id IS NOT NULL AND merchant_id = @demo_merchant_id;

DELETE FROM sys_user
    WHERE phone = '13800000001' AND role = 'MERCHANT';
