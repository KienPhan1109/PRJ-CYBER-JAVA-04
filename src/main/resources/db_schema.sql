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
    is_deleted TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE RESTRICT
);

-- 3. Table Computer
CREATE TABLE computers (
    computer_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    zone VARCHAR(50) NOT NULL, -- VIP, STANDARD, ESPORT, STREAMING, COUPLE
    hardware_config TEXT,
    status ENUM('AVAILABLE', 'IN_USE', 'MAINTENANCE') DEFAULT 'AVAILABLE',
    price_per_hour DECIMAL(10, 2) NOT NULL,
    is_deleted TINYINT(1) DEFAULT 0
);

-- 4. Table Booking (PC Reservation)
CREATE TABLE bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    computer_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NULL,
    status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'RESERVED') DEFAULT 'PENDING',
    total_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    hourly_rate_snapshot DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    FOREIGN KEY (computer_id) REFERENCES computers(computer_id) ON DELETE RESTRICT
);

-- ==============================================
-- F&B SCHEMA
-- ==============================================

-- 5. F&B Categories
CREATE TABLE fb_categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. F&B Menu Items
CREATE TABLE fb_menu_items (
    menu_item_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    base_price DECIMAL(12, 2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    prep_time_minutes INT DEFAULT 5,
    item_tags VARCHAR(255),
    availability VARCHAR(50) DEFAULT 'ALL',
    temperature_level ENUM('HOT','COLD','ICED','NONE') DEFAULT 'NONE',
    status ENUM('ACTIVE','OUT_OF_STOCK','HIDDEN') DEFAULT 'ACTIVE',
    is_deleted TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES fb_categories(category_id) ON DELETE RESTRICT
);

-- 7. F&B Item Options
CREATE TABLE fb_item_options (
    option_id INT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id INT NOT NULL,
    option_type ENUM('SIZE','WEIGHT','SUGAR_LEVEL','ICE_LEVEL','OTHER') NOT NULL,
    option_label VARCHAR(50) NOT NULL,
    extra_price DECIMAL(10, 2) DEFAULT 0.00,
    FOREIGN KEY (menu_item_id) REFERENCES fb_menu_items(menu_item_id) ON DELETE CASCADE
);

-- 8. F&B Toppings
CREATE TABLE fb_toppings (
    topping_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    extra_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    stock_quantity INT DEFAULT 99,
    status ENUM('ACTIVE','OUT_OF_STOCK','HIDDEN') DEFAULT 'ACTIVE'
);

-- 9. F&B Orders
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

-- 10. Order Details
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

-- 11. System Logs
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

-- ==============================================
-- ENRICHED SEED DATA
-- ==============================================

INSERT INTO roles (role_name) VALUES ('ADMIN'), ('STAFF'), ('CUSTOMER');

-- Users (password = hash123)
INSERT INTO users (username, password_hash, role_id, balance, full_name, phone, status) VALUES
('admin', 'hash123', 1, 0.00, 'Quản trị viên Hệ thống', '0901234567', 'ACTIVE'),
('staff1', 'hash123', 2, 0.00, 'Nguyễn Văn Nhân', '0901234568', 'ACTIVE'),
('staff2', 'hash123', 2, 0.00, 'Trần Thị Viên', '0901234560', 'ACTIVE'),
('customer1', 'hash123', 3, 500000.00, 'Anh Khách VIP 1', '0901234569', 'ACTIVE'),
('customer2', 'hash123', 3, 150000.00, 'Bạn Khách Thường 1', '0901234570', 'ACTIVE'),
('customer3', 'hash123', 3, 1000000.00, 'Khách Quen Esport', '0901234571', 'ACTIVE'),
('customer4', 'hash123', 3, 20000.00, 'Khách Vãng Lai', '0901234572', 'ACTIVE');

-- Computers (More Zones)
INSERT INTO computers (name, zone, hardware_config, status, price_per_hour) VALUES
('VIP-01', 'VIP', 'i9-13900K, RTX 4090, 64GB RAM, 240Hz Monitor', 'AVAILABLE', 25000.00),
('VIP-02', 'VIP', 'i9-13900K, RTX 4090, 64GB RAM, 240Hz Monitor', 'AVAILABLE', 25000.00),
('STD-01', 'STANDARD', 'i5-12400F, RTX 3060, 16GB RAM, 144Hz Monitor', 'AVAILABLE', 10000.00),
('STD-02', 'STANDARD', 'i5-12400F, RTX 3060, 16GB RAM, 144Hz Monitor', 'AVAILABLE', 10000.00),
('ESP-01', 'ESPORT', 'i7-13700K, RTX 4070Ti, 32GB RAM, 360Hz Monitor', 'AVAILABLE', 18000.00),
('ESP-02', 'ESPORT', 'i7-13700K, RTX 4070Ti, 32GB RAM, 360Hz Monitor', 'AVAILABLE', 18000.00),
('STR-01', 'STREAMING', 'i9-13900K, RTX 4080, Dual Monitor, GoXLR, Shure SM7B', 'AVAILABLE', 35000.00),
('CP-01', 'COUPLE', '2x i5-13400, RTX 3060Ti, Sofa đôi, Privacy Partition', 'AVAILABLE', 20000.00);

-- F&B Categories
INSERT INTO fb_categories (category_name, description) VALUES
('FOOD',  'Cơm, Mì, Bún thơm ngon'),
('DRINK', 'Trà sữa, Cafe, Nước ngọt mát lạnh'),
('SNACK', 'Đồ ăn vặt, hướng dương, khoai tây'),
('COMBO', 'Các gói kết hợp tiết kiệm');

-- F&B Menu Items
INSERT INTO fb_menu_items (category_id, name, description, base_price, stock_quantity, prep_time_minutes, item_tags, availability, temperature_level, status) VALUES
(1, 'Mì Xào Bò Đặc Biệt',      'Mì xào giòn kèm 100g thịt bò và rau cải', 45000.00, 30, 10, 'BestSeller,Hot', 'ALL', 'NONE', 'ACTIVE'),
(1, 'Cơm Rang Dưa Bò',        'Cơm rang giòn kèm dưa chua và bò xào', 40000.00, 20, 12, 'Popular', 'ALL', 'NONE', 'ACTIVE'),
(1, 'Mì Cay Seoul (Cấp 3)',   'Mì cay hải sản style Hàn Quốc', 55000.00, 15, 15, 'Spicy,Hot', 'ALL', 'NONE', 'ACTIVE'),
(3, 'Khoai Tây Chiên Lắc Phô Mai', 'Khoai tây bổ múi cau, bột phô mai Mỹ', 25000.00, 50, 5, 'Snack', 'ALL', 'NONE', 'ACTIVE'),
(2, 'Trà Sữa Truyền Thống',   'Trà đen ủ lạnh pha sữa tươi', 30000.00, 99, 5, 'BestSeller', 'ALL', 'COLD', 'ACTIVE'),
(2, 'Trà Đào Cam Sả',         'Trà đào miếng to, cam tươi, sả thơm', 35000.00, 40, 5, 'Refreshing', 'ALL', 'COLD', 'ACTIVE'),
(2, 'Nước Ngọt Pepsi',        'Pepsi lon 330ml mát lạnh', 15000.00, 100, 1, 'IceCold', 'ALL', 'COLD', 'ACTIVE'),
(2, 'Cafe Sữa Đá',           'Cafe Ranger Robusta, sữa đặc Ngôi sao', 22000.00, 60, 7, 'Morning', 'ALL', 'COLD', 'ACTIVE'),
(4, 'Combo Cày Đêm',          '1 Mì xào bò + 1 Pepsi + 1 Khăn lạnh', 55000.00, 99, 10, 'Value', 'ALL', 'NONE', 'ACTIVE');

-- Toppings
INSERT INTO fb_toppings (name, extra_price, stock_quantity, status) VALUES
('Trân châu đen',   7000.00, 99, 'ACTIVE'),
('Thạch nha đam',   6000.00, 99, 'ACTIVE'),
('Kem cheese mặn',  10000.00, 50, 'ACTIVE'),
('Trứng ốp la',     5000.00, 100, 'ACTIVE'),
('Thêm bò',         15000.00, 30, 'ACTIVE');

-- Options (Size/Sugar)
INSERT INTO fb_item_options (menu_item_id, option_type, option_label, extra_price) VALUES
(5, 'SIZE', 'Size M', 0.00),
(5, 'SIZE', 'Size L', 10000.00),
(5, 'SUGAR_LEVEL', '100% Đường', 0.00),
(5, 'SUGAR_LEVEL', '50% Đường', 0.00),
(5, 'ICE_LEVEL', 'Đá bình thường', 0.00),
(5, 'ICE_LEVEL', 'Không đá', 0.00),
(6, 'SIZE', 'Ly thường', 0.00),
(6, 'SIZE', 'Ly khổng lồ (+Top)', 15000.00);
