DROP DATABASE IF EXISTS cyber_gaming_db;
CREATE DATABASE IF NOT EXISTS cyber_gaming_db;
USE cyber_gaming_db;

-- ==============================================
-- CORE TABLES
-- ==============================================

-- 1. Table Role
CREATE TABLE roles (
role_id INT AUTO_INCREMENT PRIMARY KEY,
role_name VARCHAR(50) UNIQUE NOT NULL
);

-- 2. Table User
CREATE TABLE users (
user_id INT AUTO_INCREMENT PRIMARY KEY,
username VARCHAR(50) UNIQUE NOT NULL,
password_hash VARCHAR(255) NOT NULL,
role_id INT NOT NULL,
balance DECIMAL(12, 2) DEFAULT 0.00,
full_name VARCHAR(100) NOT NULL,
phone VARCHAR(15),
status ENUM('ACTIVE', 'LOCKED') DEFAULT 'ACTIVE',
is_deleted TINYINT(1) DEFAULT 0               COMMENT '0: Hoạt động, 1: Đã xóa mềm',
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE RESTRICT
);

-- 3. Table Computer
CREATE TABLE computers (
computer_id INT AUTO_INCREMENT PRIMARY KEY,
name VARCHAR(50) UNIQUE NOT NULL,
zone VARCHAR(50) NOT NULL,
hardware_config TEXT,
status ENUM('AVAILABLE', 'IN_USE', 'MAINTENANCE') DEFAULT 'AVAILABLE',
price_per_hour DECIMAL(10, 2) NOT NULL,
is_deleted TINYINT(1) DEFAULT 0                   COMMENT '1: Đã thanh lý'
);

-- 4. Table Booking (PC Reservation)
--    end_time NULL cho phép chế độ Pay-As-You-Go (mở máy không cần biết trước giờ kết thúc)
CREATE TABLE bookings (
booking_id INT AUTO_INCREMENT PRIMARY KEY,
user_id INT NOT NULL,
computer_id INT NOT NULL,
start_time DATETIME NOT NULL,
end_time DATETIME NULL                              COMMENT 'NULL khi đang Pay-As-You-Go',
status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
total_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
hourly_rate_snapshot DECIMAL(10, 2)                 COMMENT 'Chốt giá 1 giờ chơi thời điểm mở máy',
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
FOREIGN KEY (computer_id) REFERENCES computers(computer_id) ON DELETE RESTRICT
);

-- ==============================================
-- F&B ADVANCED SCHEMA
-- ==============================================

-- 5. Bảng phân loại danh mục F&B
CREATE TABLE fb_categories (
category_id   INT AUTO_INCREMENT PRIMARY KEY,
category_name VARCHAR(50)  NOT NULL,                          -- VD: FOOD, DRINK, COMBO
description   TEXT,
is_active     BOOLEAN DEFAULT TRUE,
created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Bảng Món chính của Menu (nâng cao)
CREATE TABLE fb_menu_items (
menu_item_id      INT AUTO_INCREMENT PRIMARY KEY,
category_id       INT          NOT NULL,
name              VARCHAR(150) NOT NULL,
description       TEXT                                        COMMENT 'Mô tả món, cho phép NULL',
base_price        DECIMAL(12, 2) NOT NULL,
stock_quantity    INT DEFAULT 0,
prep_time_minutes INT DEFAULT 5                    COMMENT 'Thời gian chuẩn bị (phút)',
item_tags         VARCHAR(255)                     COMMENT 'Nhãn: Spicy, Vegan, BestSeller — cho phép NULL',
availability      VARCHAR(50)  DEFAULT 'ALL'       COMMENT 'Khung giờ: ALL hoặc HH:MM-HH:MM',
temperature_level ENUM('HOT','COLD','ICED','NONE') DEFAULT 'NONE' COMMENT 'Áp dụng cho đồ uống',
status            ENUM('ACTIVE','OUT_OF_STOCK','HIDDEN') DEFAULT 'ACTIVE',
is_deleted        TINYINT(1) DEFAULT 0             COMMENT '0: Bình thường, 1: Đã xóa mềm',
created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (category_id) REFERENCES fb_categories(category_id) ON DELETE RESTRICT,
CHECK (base_price >= 0),
CHECK (stock_quantity >= 0)
);

-- 7. Bảng lựa chọn đi kèm (Size, Gram, Đường, Đá)
CREATE TABLE fb_item_options (
option_id      INT AUTO_INCREMENT PRIMARY KEY,
menu_item_id   INT          NOT NULL,
option_type    ENUM('SIZE','WEIGHT','SUGAR_LEVEL','ICE_LEVEL','OTHER') NOT NULL COMMENT 'Loại tuỳ chọn',
option_label   VARCHAR(50)  NOT NULL                COMMENT 'VD: Size L, 200g, 50% đường',
extra_price    DECIMAL(10, 2) DEFAULT 0.00          COMMENT 'Phụ phí thêm',
FOREIGN KEY (menu_item_id) REFERENCES fb_menu_items(menu_item_id) ON DELETE CASCADE
);

-- 8. Bảng Topping (nâng cấp: thêm stock_quantity + status)
CREATE TABLE fb_toppings (
topping_id     INT AUTO_INCREMENT PRIMARY KEY,
name           VARCHAR(100) NOT NULL,
extra_price    DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
stock_quantity INT DEFAULT 99                            COMMENT 'Tồn kho topping',
status         ENUM('ACTIVE','OUT_OF_STOCK','HIDDEN') DEFAULT 'ACTIVE'
COMMENT 'ACTIVE: dùng được, OUT_OF_STOCK: hết hàng, HIDDEN: đã khóa',
is_active      BOOLEAN DEFAULT TRUE                     COMMENT 'Legacy flag - dùng status thay thế'
);

-- 9. Table F&B Order
CREATE TABLE fb_orders (
order_id INT AUTO_INCREMENT PRIMARY KEY,
user_id INT NOT NULL,
booking_id INT,
status ENUM('PENDING', 'PREPARING', 'DELIVERED', 'CANCELLED') DEFAULT 'PENDING',
total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

-- 10. Bảng Order Detail nâng cao (hỗ trợ lưu cấu hình Decorator)
CREATE TABLE fb_order_details (
detail_id        INT AUTO_INCREMENT PRIMARY KEY,
order_id         INT NOT NULL,
menu_item_id     INT NOT NULL                        COMMENT 'Trỏ vào fb_menu_items',
quantity         INT NOT NULL DEFAULT 1,
unit_price       DECIMAL(12, 2) NULL                 COMMENT 'Giá đã tính sau Decorator+Strategy',
item_name_snapshot VARCHAR(255)                      COMMENT 'Chốt tên tại thời điểm đặt',
item_description TEXT                                COMMENT 'Mô tả đầy đủ (vd: Trà sữa + Size L + Trân châu)',
item_config_json TEXT                                COMMENT 'JSON lưu toàn bộ cấu hình Decorator để load lại',
unit_price_snapshot DECIMAL(12, 2)                   COMMENT 'Chốt giá tại thời điểm đặt',
discount_applied DECIMAL(12, 2) DEFAULT 0.00         COMMENT 'Số tiền đã giảm (Strategy)',
discount_strategy_name VARCHAR(100)                  COMMENT 'Tên strategy đã dùng',
created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (order_id)     REFERENCES fb_orders(order_id) ON DELETE CASCADE,
FOREIGN KEY (menu_item_id) REFERENCES fb_menu_items(menu_item_id) ON DELETE RESTRICT,
CHECK (quantity > 0)
);

-- ==============================================
-- AUDIT LOG SCHEMA
-- ==============================================

-- 11. Bảng ghi lịch sử mọi hành động trong hệ thống
CREATE TABLE system_logs (
id         INT AUTO_INCREMENT PRIMARY KEY,
log_type   ENUM('USER', 'COMPUTER', 'FB') NOT NULL    COMMENT 'Nhóm log: USER, COMPUTER, FB',
actor_id   INT          NOT NULL                       COMMENT 'ID tài khoản thực hiện (Admin/Staff đang login)',
action     VARCHAR(255) NOT NULL                       COMMENT 'Mô tả ngắn, VD: Nạp 50,000 VND cho KH#5',
target_id  INT          NULL                           COMMENT 'ID đối tượng bị tác động (nullable)',
created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
INDEX idx_log_type        (log_type),
INDEX idx_actor_id        (actor_id),
INDEX idx_created_at      (created_at)
);

-- ==============================================
-- SEED DATA
-- ==============================================

INSERT INTO roles (role_name) VALUES ('ADMIN'), ('STAFF'), ('CUSTOMER');

-- Users (password = hash123)
INSERT INTO users (username, password_hash, role_id, balance, full_name, phone, status) VALUES
('admin', 'hash123', 1, 0.00, 'Quản trị viên', '0901234567', 'ACTIVE'),
('staff1', 'hash123', 2, 0.00, 'Nhân viên 1', '0901234568', 'ACTIVE'),
('customer1', 'hash123', 3, 500000.00, 'Khách hàng VIP 1', '0901234569', 'ACTIVE'),
('customer2', 'hash123', 3, 20000.00, 'Khách hàng Thường 1', '0901234570', 'ACTIVE');

-- Computers
INSERT INTO computers (name, zone, hardware_config, status, price_per_hour) VALUES
('VIP-01', 'VIP', 'i9-13900K, RTX 4090, 64GB RAM, 240Hz Monitor', 'AVAILABLE', 25000.00),
('VIP-02', 'VIP', 'i9-13900K, RTX 4090, 64GB RAM, 240Hz Monitor', 'AVAILABLE', 25000.00),
('STD-01', 'STANDARD', 'i5-12400F, RTX 3060, 16GB RAM, 144Hz Monitor', 'AVAILABLE', 10000.00),
('STD-02', 'STANDARD', 'i5-12400F, RTX 3060, 16GB RAM, 144Hz Monitor', 'AVAILABLE', 10000.00);

-- F&B Categories
INSERT INTO fb_categories (category_name, description) VALUES
('FOOD',  'Các món ăn: cơm, mì'),
('DRINK', 'Đồ uống: trà sữa, nước ngọt, cafe'),
('SNACK', 'Ăn vặt: khoai tây chiên, dưa hũ');

-- F&B Menu Items
INSERT INTO fb_menu_items (category_id, name, description, base_price, stock_quantity, prep_time_minutes, item_tags, availability, temperature_level, status) VALUES
(1, 'Mì Xào Bò',      'Mì xào giòn kèm thịt bò xào tỏi',          45000.00, 30, 10, 'BestSeller,Hot',    'ALL',         'NONE', 'ACTIVE'),
(1, 'Cơm Rang Dưa Bò','Cơm rang giòn kèm dưa chua và thịt bò',     40000.00, 20, 12, 'Spicy',             'ALL',         'NONE', 'ACTIVE'),
(3, 'Snack Lays',      'Khoai tây chiên Lays các vị',               20000.00, 50,  1, 'Vegan',             'ALL',         'NONE', 'ACTIVE'),
(2, 'Trà Sữa Truyền Thống', 'Trà sữa truyền thống pha tươi',       35000.00, 99,  5, 'BestSeller',        'ALL',         'COLD', 'ACTIVE'),
(2, 'Nước Ngọt Pepsi', 'Nước ngọt Pepsi lon 330ml',                 15000.00,100,  1, NULL,                'ALL',         'COLD', 'ACTIVE'),
(2, 'Cafe Đen',        'Cafe đen pha phin truyền thống',            20000.00, 60,  7, 'Hot',               '06:00-14:00', 'HOT',  'ACTIVE');

-- Toppings (có stock_quantity + status)
INSERT INTO fb_toppings (name, extra_price, stock_quantity, status) VALUES
('Trân châu đen',   7000.00,  99, 'ACTIVE'),
('Thạch cà phê',    6000.00,  99, 'ACTIVE'),
('Kem cheese',     10000.00,  50, 'ACTIVE'),
('Trứng pudding',   8000.00,  80, 'ACTIVE'),
('Thêm trứng',      5000.00, 100, 'ACTIVE');

-- Options (Size) cho Trà Sữa (menu_item_id = 4)
INSERT INTO fb_item_options (menu_item_id, option_type, option_label, extra_price) VALUES
(4, 'SIZE',        'Size M',        0.00),
(4, 'SIZE',        'Size L',       10000.00),
(4, 'SUGAR_LEVEL', '100% đường',    0.00),
(4, 'SUGAR_LEVEL', '50% đường',     0.00),
(4, 'ICE_LEVEL',   'Đá bình thường',0.00),
(4, 'ICE_LEVEL',   'Ít đá',         0.00),
(4, 'ICE_LEVEL',   'Không đá',      0.00);

-- Options (Size) cho Nước Ngọt (menu_item_id = 5)
INSERT INTO fb_item_options (menu_item_id, option_type, option_label, extra_price) VALUES
(5, 'SIZE', 'Lon 330ml', 0.00),
(5, 'SIZE', 'Chai 500ml', 8000.00);
