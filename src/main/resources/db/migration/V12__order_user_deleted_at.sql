-- 用户端软删除：删除后不在「我的订单」中显示，商家端仍可见
SET @exist := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'hotel_order'
      AND COLUMN_NAME = 'user_deleted_at'
);
SET @sql := IF(
    @exist = 0,
    'ALTER TABLE hotel_order ADD COLUMN user_deleted_at DATETIME DEFAULT NULL COMMENT ''用户删除时间'' AFTER cancelled_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
