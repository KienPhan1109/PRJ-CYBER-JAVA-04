DROP DATABASE IF EXISTS cyber_gaming_db;
CREATE DATABASE IF NOT EXISTS cyber_gaming_db;
USE cyber_gaming_db;

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
    price_per_hour DECIMAL(10, 2) NOT NULL
);

-- 4. Table Service Item (F&B)
CREATE TABLE service_items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    status ENUM('ACTIVE', 'OUT_OF_STOCK') DEFAULT 'ACTIVE',
    CHECK (price >= 0),
    CHECK (stock_quantity >= 0)
);

-- 5. Table Booking (PC Reservation)
CREATE TABLE bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    computer_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    total_fee DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    FOREIGN KEY (computer_id) REFERENCES computers(computer_id) ON DELETE RESTRICT
);

-- 6. Table F&B Order
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

-- 7. Table Order Detail
CREATE TABLE order_details (
    order_detail_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    item_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES fb_orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES service_items(item_id) ON DELETE RESTRICT,
    CHECK (quantity > 0)
);

-- ==============================================
-- DỮ LIỆU MẪU (SEED DATA)
-- ==============================================

INSERT INTO roles (role_name) VALUES ('ADMIN'), ('STAFF'), ('CUSTOMER');

-- Thêm Users
INSERT INTO users (username, password_hash, role_id, balance, full_name, phone, status) VALUES
('admin', 'hash123', 1, 0.00, 'Quản trị viên', '0901234567', 'ACTIVE'),
('staff1', 'hash123', 2, 0.00, 'Nhân viên 1', '0901234568', 'ACTIVE'),
('customer1', 'hash123', 3, 500000.00, 'Khách hàng VIP 1', '0901234569', 'ACTIVE'),
('customer2', 'hash123', 3, 20000.00, 'Khách hàng Thường 1', '0901234570', 'ACTIVE');

-- Thêm Máy tính
INSERT INTO computers (name, zone, hardware_config, status, price_per_hour) VALUES
('VIP-01', 'VIP', 'i9-13900K, RTX 4090, 64GB RAM, 240Hz Monitor', 'AVAILABLE', 25000.00),
('VIP-02', 'VIP', 'i9-13900K, RTX 4090, 64GB RAM, 240Hz Monitor', 'AVAILABLE', 25000.00),
('STD-01', 'Standard', 'i5-12400F, RTX 3060, 16GB RAM, 144Hz Monitor', 'AVAILABLE', 10000.00),
('STD-02', 'Standard', 'i5-12400F, RTX 3060, 16GB RAM, 144Hz Monitor', 'AVAILABLE', 10000.00);

-- Thêm Sản phẩm F&B
INSERT INTO service_items (name, description, price, stock_quantity, status) VALUES
('Mì Tôm Trứng Lòng Đào', 'Mì hảo hảo trứng lòng đào siêu ngon', 25000.00, 50, 'ACTIVE'),
('Sting Dâu', 'Nước tăng lực Sting đỏ', 15000.00, 100, 'ACTIVE'),
('Coca Cola', 'Nước ngọt giải khát', 15000.00, 100, 'ACTIVE'),
('Cơm Rang Dưa Bò', 'Cơm rang giòn kèm dưa chua thịt bò', 40000.00, 20, 'ACTIVE'),
('Trà Đá', 'Trà đá miễn phí cho VIP', 5000.00, 200, 'ACTIVE');

-- Thêm một Booking (Phiên đặt máy) đang PENDING (giả định 1 giờ nữa bắt đầu và chơi trong 3 giờ)
INSERT INTO bookings (user_id, computer_id, start_time, end_time, status, total_fee) VALUES
(3, 1, DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR), 'PENDING', 75000.00);

-- Khách hàng này cũng order thêm 1 Mì tôm trứng và 1 Sting Dâu
INSERT INTO fb_orders (user_id, booking_id, status, total_amount) VALUES
(3, 1, 'PENDING', 40000.00);

INSERT INTO order_details (order_id, item_id, quantity, unit_price) VALUES
(1, 1, 1, 25000.00), -- Mì tôm trứng
(1, 2, 1, 15000.00); -- Sting Dâu