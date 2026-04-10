# Dự án: Cyber Cafe Management System (PRJ-CYBER-JAVA-04)

Tài liệu này tổng hợp hiểu biết của tôi về kiến trúc và cách thức vận hành của dự án sau khi đọc toàn bộ mã nguồn.

---

## 1. Tổng quan dự án
Đây là một hệ thống quản lý phòng máy (Cyber Cafe) hoàn chỉnh, được xây dựng trên nền tảng **Java Swing** cho giao diện người dùng và **MySQL** cho cơ sở dữ liệu. Dự án áp dụng nhiều mẫu thiết kế (design patterns) nâng cao để giải quyết các bài toán nghiệp vụ phức tạp về tính tiền và quản lý dịch vụ.

## 2. Kiến trúc hệ thống
Dự án tuân thủ mô hình 3 lớp (3-Tier Architecture):
- **Presentation Layer (`com.cyber.view`)**: Sử dụng Java Swing. Quản lý việc hiển thị và điều hướng thông qua `AppRouter`.
- **Service Layer (`com.cyber.service`)**: Chứa logic nghiệp vụ. Các service chính như `AuthService`, `BookingService`, `FbOrderService` đều được triển khai theo mẫu **Singleton**.
- **Data Access Layer (`com.cyber.dao`)**: Sử dụng JDBC để tương tác với MySQL. Tách biệt Interface và Implementation (`DAOImpl`).

---

## 3. Các thực thể chính và Luồng dữ liệu
### A. Quản lý người dùng & Bảo mật
- **Roles**: Gồm `ADMIN`, `STAFF`, và `CUSTOMER`.
- **User**: Lưu trữ thông tin tài khoản, số dư (`balance`) và trạng thái (`ACTIVE`, `LOCKED`).
- **Security**: Mật khẩu được mã hóa bằng thuật toán **SHA-256**. Hệ thống có cơ chế tự động nâng cấp mật khẩu từ văn bản thuần túy sang mã hóa khi người dùng đăng nhập lần đầu sau khi cập nhật hệ thống.

### B. Quản lý Máy trạm (Computer & Booking)
- **Computer**: Quản lý các thông số như cấu hình phần cứng, giá giờ chơi và trạng thái (`AVAILABLE`, `IN_USE`, `MAINTENANCE`).
- **Booking**: Lưu trữ các phiên chơi. Hệ thống hỗ trợ:
  - **Pre-paid (Trả trước)**: Khách đặt máy với khoảng thời gian cố định.
  - **Pay-As-You-Go (Trả sau/Mở máy tự do)**: Tính tiền dựa trên thời gian sử dụng thực tế.
- **Billing Heartbeat**: Một cơ chế chạy nền (background threads) định kỳ (ví dụ: mỗi 10 giây) để trừ tiền trực tiếp vào số dư người dùng dựa trên giá snapshot tại thời điểm mở máy.

### C. Hệ thống F&B (Food & Beverage) Nâng cao
Đây là phần có độ phức tạp kỹ thuật cao nhất với sự kết hợp của nhiều Design Patterns:
- **Decorator Pattern**: Sử dụng để bọc các món ăn (`SingleItem`) với các tùy chọn như `SizeDecorator`, `ToppingDecorator`. Điều này giúp tính toán giá cuối cùng một cách linh hoạt mà không làm rối mã nguồn.
- **Strategy Pattern**: Áp dụng cho logic giảm giá (`IDiscountStrategy`), bao gồm `PercentageDiscount`, `FixedAmountDiscount` và `HappyHourDiscount`.
- **Stock Management**: Món ăn và Topping đều có quản lý tồn kho (`stock_quantity`). Hệ thống tự ngăn chặn đặt hàng nếu hết hàng.

---

## 4. Các điểm nổi bật về mặt Kỹ thuật
- **Transaction Safety**: Sử dụng `conn.setAutoCommit(false)` và `rollback()` trong Service layer để đảm bảo tính toàn vẹn dữ liệu khi thực hiện các tác vụ tài chính (ví dụ: trừ tiền đồng thời với việc tạo đơn hàng).
- **Snapshot Pricing**: Giá giờ chơi và giá món ăn được "chốt" (snapshot) tại thời điểm giao dịch vào bảng `bookings` hoặc `fb_order_details`. Điều này giúp tránh tranh chấp nếu Admin thay đổi giá sau khi khách đã bắt đầu sử dụng dịch vụ.
- **Log System**: Mọi hành động nhạy cảm (nạp tiền, sửa máy, xóa món) đều được ghi lại vào bảng `system_logs` để truy vết.

## 5. Cấu trúc Thư mục chính
- `com.cyber.connection`: Quản lý kết nối JDBC.
- `com.cyber.dao`: Tương tác trực tiếp với Database.
- `com.cyber.domain.fb`: Chứa logic Domain của F&B (Patterns).
- `com.cyber.exception`: Các ngoại lệ nghiệp vụ (`BusinessException`).
- `com.cyber.model`: Các POJO đại diện cho bảng trong Database.
- `com.cyber.service`: Logic xử lý nghiệp vụ chính.
- `com.cyber.view`: Giao diện Swing.

---

## Kết luận
Dự án được thiết kế rất bài bản, có sự đầu tư về mặt kiến trúc và xử lý các case nghiệp vụ thực tế một cách chuyên nghiệp (như cơ chế Heartbeat tính tiền và thiết kế F&B linh hoạt). Hệ thống đã sẵn sàng cho quy mô vận hành thực tế của một phòng máy cao cấp.
