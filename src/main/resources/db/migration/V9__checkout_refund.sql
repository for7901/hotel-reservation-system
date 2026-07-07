ALTER TABLE hotel_order
    ADD COLUMN checkout_apply_at DATETIME DEFAULT NULL AFTER cancelled_at,
    ADD COLUMN refund_amount DECIMAL(10,2) DEFAULT NULL AFTER checkout_apply_at,
    ADD COLUMN refund_policy VARCHAR(255) DEFAULT NULL AFTER refund_amount;
