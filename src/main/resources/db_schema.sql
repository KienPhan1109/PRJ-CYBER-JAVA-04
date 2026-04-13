DROP DATABASE IF EXISTS cyber_gaming_db;
CREATE DATABASE IF NOT EXISTS cyber_gaming_db;
USE cyber_gaming_db;

-- 1. Table User
CREATE TABLE users (
                       user_id INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role ENUM('ADMIN', 'STAFF', 'CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
                       balance DECIMAL(12, 2) DEFAULT 0.00,
                       full_name VARCHAR(100) NOT NULL,
                       phone VARCHAR(15),
                       status ENUM('ACTIVE', 'LOCKED') DEFAULT 'ACTIVE',
                       is_deleted TINYINT(1) DEFAULT 0,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Table Computer
CREATE TABLE computers (
                           computer_id INT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(50) UNIQUE NOT NULL,
                           zone VARCHAR(50) NOT NULL, -- VIP, STANDARD, ESPORT, STREAMING, COUPLE
                           hardware_config TEXT,
                           status ENUM('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'HIDDEN') DEFAULT 'AVAILABLE',
                           price_per_hour DECIMAL(10, 2) NOT NULL,
                           is_deleted TINYINT(1) DEFAULT 0
);

-- 3. Table Booking
CREATE TABLE bookings (
                          booking_id INT AUTO_INCREMENT PRIMARY KEY,
                          user_id INT NOT NULL,
                          computer_id INT NOT NULL,
                          start_time DATETIME NOT NULL,
                          end_time DATETIME NULL,
                          status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'RESERVED') DEFAULT 'PENDING',
                          total_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                          hourly_rate_snapshot DECIMAL(10, 2),
                          staff_id INT DEFAULT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
                          FOREIGN KEY (computer_id) REFERENCES computers(computer_id) ON DELETE RESTRICT,
                          FOREIGN KEY (staff_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- 4. F&B Categories
CREATE TABLE fb_categories (
                               category_id INT AUTO_INCREMENT PRIMARY KEY,
                               category_name VARCHAR(50) NOT NULL,
                               description TEXT,
                               is_active BOOLEAN DEFAULT TRUE,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. F&B Menu Items
CREATE TABLE fb_menu_items (
                               menu_item_id INT AUTO_INCREMENT PRIMARY KEY,
                               category_id INT NOT NULL,
                               name VARCHAR(150) NOT NULL,
                               description TEXT,
                               base_price DECIMAL(12, 2) NOT NULL,
                               stock_quantity INT DEFAULT 0,
                               prep_time_minutes INT DEFAULT 5,

                               availability VARCHAR(50) DEFAULT 'ALL',
                               temperature_level ENUM('HOT','COLD','ICED','NONE') DEFAULT 'NONE',
                               status ENUM('ACTIVE','OUT_OF_STOCK','HIDDEN') DEFAULT 'ACTIVE',
                               is_deleted TINYINT(1) DEFAULT 0,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (category_id) REFERENCES fb_categories(category_id) ON DELETE RESTRICT
);


-- 6. F&B Orders
CREATE TABLE fb_orders (
                           order_id INT AUTO_INCREMENT PRIMARY KEY,
                           user_id INT NOT NULL,
                           booking_id INT,
                           status ENUM('PENDING', 'PREPARING', 'DELIVERED', 'CANCELLED') DEFAULT 'PENDING',
                           total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                           staff_id INT DEFAULT NULL,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
                           FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
                           FOREIGN KEY (staff_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- 7. Order Details
CREATE TABLE fb_order_details (
                                  detail_id INT AUTO_INCREMENT PRIMARY KEY,
                                  order_id INT NOT NULL,
                                  menu_item_id INT NOT NULL,
                                  quantity INT NOT NULL DEFAULT 1,
                                  unit_price DECIMAL(12, 2) NULL,
                                  item_name_snapshot VARCHAR(255),
                                  item_description TEXT,
                                  item_config_json TEXT,
                                  unit_price_snapshot DECIMAL(12, 2),
                                  discount_applied DECIMAL(12, 2) DEFAULT 0.00,
                                  discount_strategy_name VARCHAR(100),
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  FOREIGN KEY (order_id) REFERENCES fb_orders(order_id) ON DELETE CASCADE,
                                  FOREIGN KEY (menu_item_id) REFERENCES fb_menu_items(menu_item_id) ON DELETE RESTRICT
);

-- 8. System Logs
CREATE TABLE system_logs (
                             id INT AUTO_INCREMENT PRIMARY KEY,
                             log_type ENUM('USER', 'COMPUTER', 'FB') NOT NULL,
                             actor_id INT NOT NULL,
                             action VARCHAR(255) NOT NULL,
                             target_id INT NULL,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             INDEX idx_log_type (log_type),
                             INDEX idx_actor_id (actor_id),
                             INDEX idx_created_at (created_at)
);

-- 1. Users
INSERT INTO users (username, password_hash, role, balance, full_name, phone, status) VALUES
                                                                                         ('admin', 'hash123', 'ADMIN', 0.00, 'Quản trị viên Hệ thống', '0901234567', 'ACTIVE'),
                                                                                         ('staff1', 'hash123', 'STAFF', 0.00, 'Nguyễn Văn Nhân', '0901234568', 'ACTIVE'),
                                                                                         ('staff2', 'hash123', 'STAFF', 0.00, 'Trần Thị Viên', '0901234560', 'ACTIVE'),
                                                                                         ('customer1', 'hash123', 'CUSTOMER', 500000.00, 'Anh Khách VIP 1', '0901234569', 'ACTIVE'),
                                                                                         ('customer2', 'hash123', 'CUSTOMER', 150000.00, 'Bạn Khách Thường 1', '0901234570', 'ACTIVE'),
                                                                                         ('customer3', 'hash123', 'CUSTOMER', 1000000.00, 'Khách Quen Esport', '0901234571', 'ACTIVE'),
                                                                                         ('customer4', 'hash123', 'CUSTOMER', 20000.00, 'Khách Vãng Lai', '0901234572', 'ACTIVE');

-- 2. Computers
INSERT INTO computers (name, zone, hardware_config, status, price_per_hour) VALUES
-- Khu VIP (10 máy)
('VIP-01', 'VIP', 'i9-14900K, RTX 4090, 64GB RAM, 240Hz OLED', 'AVAILABLE', 25000.00),
('VIP-02', 'VIP', 'i9-14900K, RTX 4090, 64GB RAM, 240Hz OLED', 'AVAILABLE', 25000.00),
('VIP-03', 'VIP', 'i9-14900K, RTX 4090, 64GB RAM, 240Hz OLED', 'AVAILABLE', 25000.00),
-- Khu ESPORT (10 máy)
('ESP-01', 'ESPORT', 'i7-14700K, RTX 4070 Ti Super, 32GB RAM, 360Hz Monitor', 'AVAILABLE', 18000.00),
('ESP-02', 'ESPORT', 'i7-14700K, RTX 4070 Ti Super, 32GB RAM, 360Hz Monitor', 'AVAILABLE', 18000.00),
-- Khu STANDARD (20 máy)
('STD-01', 'STANDARD', 'i5-13400F, RTX 3060, 16GB RAM, 144Hz Monitor', 'AVAILABLE', 10000.00),
('STD-02', 'STANDARD', 'i5-13400F, RTX 3060, 16GB RAM, 144Hz Monitor', 'AVAILABLE', 10000.00),
('STD-03', 'STANDARD', 'i5-13400F, RTX 3060, 16GB RAM, 144Hz Monitor', 'AVAILABLE', 10000.00),
-- Khu STREAMING & COUPLE
('STR-01', 'STREAMING', 'i9-14900K, RTX 4080 Super, Shure SM7B, Dual Monitor', 'AVAILABLE', 35000.00),
('STR-02', 'STREAMING', 'i9-14900K, RTX 4080 Super, Shure SM7B, Dual Monitor', 'AVAILABLE', 35000.00),
('STR-03', 'STREAMING', 'i9-14900K, RTX 4080 Super, Shure SM7B, Dual Monitor', 'AVAILABLE', 35000.00),
('CP-01', 'COUPLE', '2x i5-13400, 2x RTX 3060 Ti, Sofa, Privacy Partition', 'AVAILABLE', 20000.00),
('CP-02', 'COUPLE', '2x i5-13400, 2x RTX 3060 Ti, Sofa, Privacy Partition', 'AVAILABLE', 20000.00);

-- 3. FB Categories
INSERT INTO fb_categories (category_name, description) VALUES
                                                           ('FOOD',  'Cơm, Mì, Bún thơm ngon'),
                                                           ('DRINK', 'Trà sữa, Cafe, Nước ngọt mát lạnh'),
                                                           ('SNACK', 'Đồ ăn vặt, hướng dương, khoai tây'),
                                                           ('TOPPING', 'Các loại topping thêm');

-- 4. FB Menu Items
INSERT INTO fb_menu_items (category_id, name, description, base_price, stock_quantity, prep_time_minutes, temperature_level) VALUES
-- FOOD (Món chính)
(1, 'Mì Xào Bò Đặc Biệt', 'Mì xào kèm 100g thịt bò và rau cải', 45000.00, 50, 10, 'NONE'),
(1, 'Cơm Rang Dưa Bò', 'Cơm rang giòn kèm dưa chua và bò xào', 40000.00, 40, 12, 'NONE'),
(1, 'Mì Cay Seoul (Cấp 3)', 'Mì cay hải sản style Hàn Quốc', 55000.00, 30, 15, 'NONE'),

-- DRINK (Thức uống)
(2, 'Trà Sữa Truyền Thống', 'Trà đen ủ lạnh pha sữa tươi', 30000.00, 100, 5, 'COLD'),
(2, 'Trà Đào Cam Sả', 'Trà đào miếng to, cam tươi, sả thơm', 35000.00, 80, 5, 'COLD'),
(2, 'Nước Ngọt Pepsi', 'Pepsi lon 330ml', 15000.00, 500, 1, 'COLD'),

-- SNACK (Đồ ăn vặt)
(3, 'Khoai Tây Chiên Lắc Phô Mai', 'Khoai tây bổ múi cau, bột phô mai Mỹ', 25000.00, 100, 5, 'NONE'),
(3, 'Hướng Dương Bà Già', 'Gói lớn loại 1', 15000.00, 200, 1, 'NONE'),
(3, 'Bim Bim Oishi', 'Các loại đồng giá', 10000.00, 300, 1, 'NONE'),

-- TOPPING (Món thêm)
(4, 'Trân châu đen', 'Thêm vào trà sữa', 7000.00, 999, 1, 'NONE'),
(4, 'Thạch nha đam', 'Thêm vào trà trái cây', 6000.00, 999, 1, 'NONE'),
(4, 'Kem cheese mặn', 'Kem béo ngậy', 10000.00, 50, 2, 'NONE');