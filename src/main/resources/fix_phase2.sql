USE cyber_gaming_db;

-- 1. Cho phép unit_price có thể Null để fix DB Error [Field 'unit_price' doesn't have a default value]
ALTER TABLE fb_order_details MODIFY COLUMN unit_price DECIMAL(12, 2) NULL;

-- 2. Đặt máy trạm: End_time có thể Null (trước đây là NOT NULL) khi user chưa đóng máy
ALTER TABLE bookings MODIFY COLUMN end_time DATETIME NULL;

-- (Bảng Toppings đã có sẵn trong schema gốc của Phase 1 - Nằm ở bảng fb_toppings).
-- Script này đảm bảo đã được migrate
CREATE TABLE IF NOT EXISTS fb_toppings (
   topping_id   INT AUTO_INCREMENT PRIMARY KEY,
   name         VARCHAR(100) NOT NULL,
   extra_price  DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
   is_active    BOOLEAN DEFAULT TRUE
);
