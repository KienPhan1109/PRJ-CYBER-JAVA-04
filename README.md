# 🎮 Cyber Gaming & F&B Management System
Hệ thống Phần mềm Quản trị tích hợp Phòng máy tính (Cyber Gaming) và Lĩnh vực dịch vụ ăn uống (F&B) được lập trình bằng ngôn ngữ **Java thuần (JDBC Console App)** cùng kiến trúc chặt chẽ.

---

## 1. 🏗️ Kiến Trúc Tổng Thể (Architecture)
Dự án được triển khai bằng thiết kế **Layered Architecture (Kiến trúc phân tầng)** với 3 tầng độc lập, vận hành theo tuần tự **View** -> **Service** -> **DAO**, bổ sung thiết kế **Singleton Pattern** để quản lý bộ nhớ thông minh.

* **View (Tầng Giao Diện Console):** Trực tiếp tương tác với người dùng ở Terminal. Thu thập dữ liệu input, map vào đối tượng Object. Không chứa logic Insert/Update vào Database.
* **Service (Tầng Nghiệp vụ Chuyên Sâu):** Nắm vai trò quản trị Logic. Xử lý các nghiệp vụ bắt lỗi đặc thù chưa tới phiên database như: Trùng tên người dùng, Không cho xoá máy đang có khách, Tự động Set Enum. Nếu lỗi xảy ra, ném ra một cảnh báo `BusinessException`.
* **DAO (Data Access Object):** Tầng duy nhất tương tác và thao tác trực tiếp với **MySQL Database** qua JDBC chuẩn. Ngăn chặn SQL Injection thông qua `PreparedStatement`. Tự động parse/get dữ liệu thành các Models. Xử lý tự bung Khóa chính (Auto_Increment) và Join Query.

---

## 2. 📂 Cấu Trúc Thư Mục Hệ Thống
```text
PRJ-CYBER-JAVA-04
├── src
│   ├── main
│   │   ├── java/com/cyber
│   │   │   ├── Main.java               <- Điểm khởi đầu (Entry Point), bộ định tuyến Menu chính.
│   │   │   ├── connection
│   │   │   │   └── DatabaseConnection.java <- Lớp cấp phát và đóng kết nối MySQL.
│   │   │   ├── dao                     <- Chứa các Inteface (I...DAO) khai báo quy chuẩn hoạt động.
│   │   │   │   └── impl                <- Code triển khai JDBC xử lý SQL (UserDAOImpl, ComputerDAO...).
│   │   │   ├── exception
│   │   │   │   └── BusinessException.java  <- Exception tuỳ chỉnh (Kèm Validation mã lỗi, tin nhắn nhẹ nhàng).
│   │   │   ├── model                   <- Các Entities đối ứng 1-1 với DB (User, Computer, Booking...).
│   │   │   │   └── enums               <- Định nghĩa cứng các cấu thái (ComputerStatus, ComputerZone, ServiceItemStatus).
│   │   │   ├── service                 <- Lớp xử lý nghiệp vụ chung cho Models (AuthService, ComputerService...).
│   │   │   ├── util                    <- Tiện ích hỗ trợ tĩnh (Static Helpers).
│   │   │   │   ├── FormatUtils.java        <- Khung Format hiển thị (VND, Tiếng Việt trạng thái màu mè).
│   │   │   │   └── InputUtils.java         <- Máy quét thần thánh, bắt lỗi trống, chữ vi phạm ký tự...
│   │   │   └── view                    <- Giao diện điều hướng màn hình.
│   │   │       ├── AuthView.java           <- Giao diện Login/Register.
│   │   │       ├── AdminMainView.java      <- Menu Bảng Điều Khiển Admin.
│   │   │       ├── ComputerManagementView.java <- Menu CRUD Quản lý Máy Trạm.
│   │   │       └── ServiceManagementView.java  <- Menu CRUD Quản lý Kho Đồ Ăn/Uống (F&B).
│   │   └── resources
│   │       └── db_schema.sql           <- Lược đồ CSDL & Dữ liệu Seed khởi chạy gốc.
└── README.md
```

---

## 3. ⚙️ Tính Năng Nổi Bật Vừa Mới Hoàn Thiện
### Authentication (Xác thực hệ thống)
* Phân luồng quyền lực sắc bén bằng table `roles`. Login sai mật khẩu thì retry, nhưng phân ra Role `ADMIN`, `STAFF`, `CUSTOMER` và rẽ nhánh chạy sau khi login xong.
* Kiểm định dữ liệu Register đầu vào (Độ dài chữ, Mật khẩu Confirmation khớp).

### Admin Module (Phân hệ Quản Trị Viên)
* **Quản Lý Máy Tính `ComputerManagementView`**: 
  - Xem bằng View Table đẹp mắt với ANSI Colors tự format cho `ComputerStatus`.
  - Validate Xóa Mềm: Bọc kết nối sang Database check `bookings`, nếu máy đó nằm trong log lịch sử/đang hoạt động thì ngăn Xóa mà không đánh sập App.
  - Auto-Fill trạng thái lúc khai sinh (Khi Add mặc định là `[SẴN SÀNG]`).
* **Quản Lý Dịch Vụ F&B `ServiceManagementView`**:
  - Giao diện console Table chuyên nghiệp để Admin theo dõi Kho (Stock) cùng Giá trị tiền tệ (VND).
  - Tương tự như Computer, chặn Drop F&B nếu đang có User chốt Bill trong bảng `order_details`.
  
---

## 4. 🗄️ Cấu Trúc Database & Mối Quan Hệ (ER Diagram Concept)
Dự án có **7 Bảng** Relational Database chính:
1. `roles`: {ADMIN, STAFF, CUSTOMER}.
2. `users` *(fk: role_id)*: Tên miền của các người dùng vào App (Chứa cả Balance/Tiền ví nếu là Khách).
3. `computers`: Tập hợp danh sách các CPU/PC (Kèm Zone / Status dưới dạng Enum).
4. `service_items`: Mặt hàng tiêu dùng tại quầy.
5. `bookings` *(fk: user_id, computer_id)*: Lịch sử Book máy trạm (Hành trang nạp - rút của Game Thủ).
6. `fb_orders` *(fk: booking_id)*: Tổng hợp đơn hàng mua thức ăn (Thuộc về một phiên chơi).
7. `order_details` *(fk: order_id, item_id)*: Tách dòng các Items Món ăn trong cái `fb_orders` to đó (Quan hệ n-n).

---

## 5. 🚀 Roadmap - Tính Năng Đang Phát Triển (Sắp Tới)
*(Các tính năng thuộc quyền của Khách Hàng hoặc Thu Ngân chưa được Build Views)*
- [ ] Tính năng nạp tiền vào tài khoản (Topup User Balance).
- [ ] Khách Hàng: Đặt máy từ xa, mở khoá phiên Booking tại tiệm, Order gọi món tới tận bàn.
- [ ] Nhân Viên (STAFF): Màn hình xác nhận order (Nấu Mì...), thanh toán bill.
