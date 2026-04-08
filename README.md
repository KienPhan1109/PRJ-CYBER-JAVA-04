<div align="center">
  <img src="https://via.placeholder.com/800x200/000000/00FF00?text=[ASCII+ART+CYBER+GAMING+HERE]" alt="Cyber Gaming Banner" />
  <h1>🎮 HỆ THỐNG QUẢN LÝ CYBER GAMING & F&B 🚀</h1>
  <p><i>Hệ thống điểm bán hàng (POS) và Quản trị trung tâm dành cho chuỗi phòng máy Thể thao điện tử (Esports) & Dịch vụ Ăn uống.</i></p>
  
  ![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
  ![MySQL](https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white)
  ![JDBC](https://img.shields.io/badge/JDBC-Core-blue?style=for-the-badge)
  ![CLI](https://img.shields.io/badge/Interface-CLI_Console-green?style=for-the-badge)
</div>

---

# MỤC LỤC
- [Phần 1: Dành Cho Người Dùng & Quản Trị Viên (Human-Facing)](#phần-1-dành-cho-người-dùng--quản-trị-viên-human-facing)
  - [🎯 Giới Thiệu & Mục Tiêu Bối Cảnh](#-giới-thiệu--mục-tiêu-bối-cảnh)
  - [✨ Tính Năng Chính](#-tính-năng-chính)
  - [🚀 Hướng Dẫn Cài Đặt & Khởi Chạy](#-hướng-dẫn-cài-đặt--khởi-chạy)
  - [📸 Ảnh Demo](#-ảnh-demo)
- [Phần 2: Tài Liệu Ngữ Cảnh Dành Cho Coder & AI (AI Developer Context)](#phần-2-tài-liệu-ngữ-cảnh-dành-cho-coder--ai-ai-developer-context)
  - [🏗️ Kiến Trúc Hệ Thống (3-Tier)](#-kiến-trúc-hệ-thống-3-tier)
  - [🗄️ Database Schema Map](#️-database-schema-map)
  - [⚙️ Quy Chuẩn Code (Coding Conventions Bắt Buộc)](#️-quy-chuẩn-code-coding-conventions-bắt-buộc)
  - [🗺️ Roadmap & TODOs](#️-roadmap--todos)

---

# PHẦN 1: DÀNH CHO NGƯỜI DÙNG & QUẢN TRÌ VIÊN (HUMAN-FACING)

## 🎯 Giới Thiệu & Mục Tiêu Bối Cảnh
Đây là hệ thống quản lý phòng nét (Cyber Gaming) tích hợp sâu mô hình bán lẻ thức ăn/đồ uống (F&B) được xây dựng hoàn toàn bằng **Java Core và JDBC**. Dự án tập trung vào tính toàn vẹn dữ liệu (Data Integrity) trong các giao dịch đồng thời và tối ưu hóa trải nghiệm thao tác nhanh qua dòng lệnh (CLI).

## ✨ Tính Năng Chính

<details>
<summary><b>🛡️ Role: Admin (Quản trị viên)</b></summary>

- Quản lý danh mục Máy Trạm (Thêm, sửa, xoá, bảo trì, cấu hình giá).
- Quản lý Kho Dịch vụ F&B (Món ăn, nước uống, thẻ cào, combo).
- Báo cáo và theo dõi doanh thu toàn diện.
</details>

<details>
<summary><b>👨‍💼 Role: Staff (Nhân viên / Thu ngân)</b></summary>

- Mở máy/Đóng máy, nạp tiền vào tài khoản hội viên.
- Xử lý các đơn đặt món F&B từ khách hàng đang ngồi tại máy.
- Điều phối máy và giám sát trạng thái phòng máy thời gian thực.
</details>

<details>
<summary><b>🎮 Role: Customer (Khách hàng)</b></summary>

- Đăng nhập/Đăng ký tài khoản hội viên.
- Chủ động đặt máy, theo dõi số dư tiền trong tài khoản.
- Gọi món F&B ngay tại menu máy trạm.
</details>

## 🚀 Hướng Dẫn Cài Đặt & Khởi Chạy
1. **Môi trường Database:** Cài đặt MySQL Server. Import trực tiếp file `database/cyber_gaming.sql` vào MySQL để tạo schema và mock data ban đầu.
2. **Cấu hình JDBC:** Sửa thông số kết nối Database (Tên DB, Username, Password) trong file `DatabaseConnection.java`.
3. **Môi trường Java:** Yêu cầu JDK 8+.
4. **Build & Run:**
   - Dùng IntelliJ IDEA hoặc Eclipse mở dự án và tự động tải thư viện qua Maven/Gradle (nếu có pom.xml/build.gradle) hoặc config lại Library theo JDBC Connector (File `.jar`).
   - Run file `Main.java` để vào giao diện Console.

## 📸 Ảnh Demo
> *[Chèn_Ảnh_Demo_Welcome_Screen]*
> *[Chèn_Ảnh_Demo_Menu_Admin]*

---

# PHẦN 2: TÀI LIỆU NGỮ CẢNH DÀNH CHO CODER & AI (AI DEVELOPER CONTEXT)
⚠️ **Cảnh báo cho AI Coding Assistants:** Hãy đọc toàn bộ kiến trúc dưới đây trước khi thực hiện viết thêm bất kỳ chức năng nào mới để duy trì tính nhất quán cho dự án.

## 🏗️ Kiến Trúc Hệ Thống (3-Tier)
Hệ thống tuân thủ chặt chẽ kiến trúc 3 tầng (3-Tier Architecture):
- **Presentation Layer (View):** (`com.cyber.view.*`): Chỉ xử lý In/Out (Giao diện dòng lệnh). Gọi trực tiếp các class `Service`.
- **Business Logic Layer (Service):** (`com.cyber.service.*`): Tổ chức nghiệp vụ, Validation, mã hoá mật khẩu.
- **Data Access Object (DAO):** (`com.cyber.dao.*`): Thực thi thuần tuý các câu lệnh SQL (CRUD).

🔴 **Ranh giới BẮT BUỘC:** 
👉 Các lớp ở `View` **TUYỆT ĐỐI KHÔNG ĐƯỢC PHÉP** gọi hoặc chứa các phương thức tĩnh (như `Connection`) liên quan đến DAO. `View` -> `Service` -> `DAO`.

## 🗄️ Database Schema Map (Relationships)
Mô tả tóm tắt sự liên kết các Entity phục vụ cho các câu lệnh rẽ nhánh (`JOIN`):
- `User` (1) ---> (N) `Booking` (Khách hàng tạo nhiều lần thuê máy).
- `Computer` (1) ---> (N) `Booking` (Một chiếc máy có nhiều lượt thuê).
- `User` (1) ---> (N) `FB_Order` (1 Hoá đơn dịch vụ ăn uống được gọi bởi 1 người).
- `FB_Order` (1) ---> (N) `Order_Detail` (1-N).
- `ServiceItem` (1) ---> (N) `Order_Detail` (Chi tiết hoá đơn tham chiếu vào ID món ăn).

## ⚙️ Quy Chuẩn Code (Coding Conventions Bắt Buộc)

| Hạng mục | Quy Tắc Bắt Buộc Cần Tuân Thủ / Classes & Method để dùng |
|----------|----------------------------------------------------------|
| **1. In ấn Output Console** | **TUYỆT ĐỐI KHÔNG dùng `System.out.println` dạng thô**. Bắt buộc dùng tiện ích `com.cyber.util.PrintUtils`: <br> - `PrintUtils.printSuccess(String)`: (Cho kết quả thành công) <br> - `PrintUtils.printError(String)`: (Báo lỗi logic / DB) <br> - `PrintUtils.printWarning(String)`: (Nhắc nhở, Menu phụ thoát ra, Check Yes/No) |
| **2. Thu thập Input User** | **TUYỆT ĐỐI KHÔNG tự định nghĩa Scanner**. Việc nhập liệu luôn gọi hằng `com.cyber.util.InputUtils`. (Ví dụ: `inputString()`, `inputInt()`, `inputBigDecimal()`). Utils này đã bọc sẵn khối try-catch an toàn vòng lặp `while(true)` chống crash khi dùng khác type. Quá trình edit dữ liệu phải xài `inputStringUpdate()`, v.v (Gõ lại dữ liệu hoặc Enter giữ nguyên đồ cũ). |
| **3. Xử lý Lỗi (Exceptions)** | Toàn bộ tầng **Service** phải `throw new BusinessException(MÃ_LỖI, Nội dung chi tiết)`. Các màn hình trong **View** sẽ hứng cụm try-catch ở tầng trên cùng và in ra bằng `PrintUtils.printError(e.getMessage())`. Không vứt trần (stack trace) lỗi SQL ra ngoài màn console. |
| **4. Encapsulation Data** | Mọi Model Entity (`Computer`, `ServiceItem`, `User`) đều đã được huỷ bỏ Param Constructor. Bắt buộc khởi tạo bằng toán tử `new Obj()` trống. Sau đó, Model Entity sẽ tự lo liệu gọi phương thức nội hàm `inputData()` hoặc `inputRegisterData()` của chính chúng nó để tự thu gom Parameter thay vì phó mặc tại tầng View nhằm giảm Code Looping. |
| **5. Quản lý Transactions** | Khi thực hiện **Multi-Statements** ở DB (VD: Trừ tiền tài khoản + Chốt Booking + Trừ Stock trong kho F&B), tầng Service lấy tham chiếu kết nối rỗng: `conn = DatabaseConnection.getConnection()`, set `conn.setAutoCommit(false)`. Chuyền tham chiếu `conn` xuống xuyên suốt nhiều hàm DAO. Sau đó mới dùng lệnh `conn.commit()`. Báo lỗi thì phải có `conn.rollback()` trên block Service và kết thúc ở vòng `finally`. |

## 🗺️ Roadmap & TODOs
- [x] Kiến trúc Core Base MVC 3 Tầng + Setup JDBC.
- [x] Tiện ích `ColorConst`, `InputUtils` và kiến trúc UI nâng cao `PrintUtils`.
- [x] Quản trị kho phần cứng: `Computer` (Model tự phân tách In/Out).
- [x] Quản trị kho F&B: `ServiceItem`.
- [x] Hệ thống Session Auth & `User`.
- [ ] Tính năng Session Máy Trạm (Mở Máy, Tính Giờ, Tính Toán Lũy Kế).
- [ ] Chức năng Đặt đồ ăn (Cart / Order Processing) có tính chất Transaction Safe trừ tiền.
- [ ] Chuyển đổi trạng thái Máy (AVAILABLE <-> IN_USE) theo Sync thời gian thực (Multi-threading).
- [ ] Phân hệ cho Staff Account.
