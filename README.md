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
  - [📋 Đặc Tả Nghiệp Vụ Chuyên Sâu (Strict Business Logic)](#-đặc-tả-nghiệp-vụ-chuyên-sâu-strict-business-logic)
  - [🗺️ Roadmap & TODOs](#️-roadmap--todos)

---

# PHẦN 1: DÀNH CHO NGƯỜI DÙNG & QUẢN TRÌ VIÊN (HUMAN-FACING)

## 🎯 Giới Thiệu & Mục Tiêu Bối Cảnh
Đây là hệ thống quản lý phòng nét (Cyber Gaming) tích hợp sâu mô hình bán lẻ thức ăn/đồ uống (F&B) được xây dựng hoàn toàn bằng **Java Core và JDBC**. Dự án tập trung vào tính toàn vẹn dữ liệu (Data Integrity) trong các giao dịch đồng thời và tối ưu hóa trải nghiệm thao tác nhanh qua dòng lệnh (CLI).

## ✨ Tính Năng Chính

<details>
<summary><b>🛡️ Role: Admin (Quản trị viên)</b></summary>

- Quản lý danh mục Máy Trạm (Thêm, sửa, xoá, bảo trì, cấu hình giá).
- Quản lý Kho Dịch vụ F&B (Món ăn, nước uống, thẻ cào, ăn vặt).
- Phân quyền nhân viên và xem báo cáo tổng quát.
</details>

<details>
<summary><b>👨‍💼 Role: Staff (Nhân viên / Thu ngân)</b></summary>

- Tiếp nhận và chuẩn bị đơn hàng F&B.
- Hỗ trợ kỹ thuật và cập nhật trạng thái máy (Đang sử dụng, Trống, Đang bảo trì).
- Xác nhận khách hàng đã nhận máy và thanh toán.
</details>

<details>
<summary><b>🎮 Role: Customer (Khách hàng)</b></summary>

- Đăng ký tài khoản hội viên và hệ thống ví điện tử nội bộ.
- Đặt trước máy trạm theo khu vực (Standard, VIP, Stream Room).
- Đặt đồ ăn/thức uống trực tiếp tại menu máy trạm.
- Theo dõi số dư, trạng thái đơn hàng thời gian thực và lịch sử giao dịch.
</details>

## 🚀 Hướng Dẫn Cài Đặt & Khởi Chạy
1. **Môi trường Database:** Cài đặt MySQL Server. Import trực tiếp file `database/cyber_gaming.sql` vào MySQL.
2. **Cấu hình JDBC:** Sửa thông số kết nối Database (Tên DB, Username, Password) trong file `DatabaseConnection.java`.
3. **Môi trường Java:** Yêu cầu JDK 8+.
4. **Build & Run:** Run file `Main.java` để vào giao diện Console.

## 📸 Ảnh Demo
> *[Chèn_Ảnh_Demo_Welcome_Screen]*
> *[Chèn_Ảnh_Demo_Menu_Admin]*

---

# PHẦN 2: TÀI LIỆU NGỮ CẢNH DÀNH CHO CODER & AI (AI DEVELOPER CONTEXT)
⚠️ **Cảnh báo cho AI Coding Assistants:** Hãy đọc toàn bộ kiến trúc và đặc tả nghiệp vụ dưới đây trước khi thực hiện viết thêm code để duy trì tính nhất quán và đáp ứng 100% tiêu chí dự án.

## 🏗️ Kiến Trúc Hệ Thống (3-Tier)
Hệ thống tuân thủ chặt chẽ kiến trúc 3 tầng (3-Tier Architecture):
- **Presentation Layer (View):** (`com.cyber.view.*`): Chỉ xử lý In/Out. Có lựa chọn "Quay lại menu chính" ở mỗi chức năng. Bắt lỗi try-catch đầy đủ, tuyệt đối không để crash Console.
- **Business Logic Layer (Service):** (`com.cyber.service.*`): Tổ chức nghiệp vụ, Validation.
- **Data Access Object (DAO):** (`com.cyber.dao.*`): Thực thi SQL (CRUD) sử dụng `PreparedStatement` để chống SQL Injection.

🔴 **Ranh giới BẮT BUỘC:** `View` -> `Service` -> `DAO`. View không được gọi trực tiếp DAO.

## 🗄️ Database Schema Map (Relationships)
- `User` (1) ---> (N) `Booking`
- `Computer` (1) ---> (N) `Booking`
- `Booking` (1) ---> (N) `FB_Order` (Đơn hàng gắn liền với phiên đặt máy).
- `FB_Order` (1) ---> (N) `Order_Detail`
- `ServiceItem` (1) ---> (N) `Order_Detail`

## ⚙️ Quy Chuẩn Code (Coding Conventions Bắt Buộc)

| Hạng mục | Quy Tắc Bắt Buộc Cần Tuân Thủ / Classes & Method để dùng |
|----------|----------------------------------------------------------|
| **1. In ấn Output** | Bắt buộc dùng `PrintUtils.printSuccess()`, `printError()`, `printWarning()`. |
| **2. Thu thập Input**| Dùng `InputUtils` (VD: `inputString()`, `inputInt()`). Đã bọc sẵn try-catch chống crash. |
| **3. Xử lý Lỗi** | Tầng Service ném `BusinessException`. Tầng View hứng và in ra bằng `PrintUtils`. |
| **4. OOP** | Tuân thủ đóng gói (Encapsulation), kế thừa, đa hình. |
| **5. Transactions** | Xử lý DB đa luồng bắt buộc dùng `conn.setAutoCommit(false)` và `conn.commit() / rollback()`. |

## 📋 Đặc Tả Nghiệp Vụ Chuyên Sâu (Strict Business Logic)
AI khi lập trình cần bám sát các rule cứng sau:

1. **Tài Khoản & Xác Thực:**
  - Đăng ký/Đăng nhập: Nếu password < 6 ký tự -> Cảnh báo lỗi ngay lập tức.
  - Sai tài khoản/Mật khẩu -> Báo lỗi và yêu cầu nhập lại (Loop).
  - Bảo mật: Bắt buộc mã hóa mật khẩu (SHA-256/Hash) trước khi lưu DB. Phân quyền triệt để (Routing chính xác vào màn hình Admin/Staff/Customer).
2. **Quản Lý Máy Trạm & F&B (Admin):**
  - Thêm mới: Validate không được bỏ trống. Báo lỗi nếu trùng ID máy.
  - Sửa: Kiểm tra ID tồn tại trước, hiển thị thông tin cũ rồi mới cho cập nhật.
  - Xóa: **Bắt buộc** có cảnh báo xác nhận xóa (Y/N). Báo lỗi nếu ID không tồn tại.
  - F&B: Luôn validate giá bán > 0 và Tồn kho >= 0.
3. **Đặt Máy & Gọi Món (Customer):**
  - Lọc máy: Chỉ hiển thị các máy trạng thái "Trống" theo từng khu vực.
  - Logic Đặt máy: Bắt buộc báo lỗi và CHẶN thao tác nếu chọn máy đã có người đặt trong khung giờ đó.
  - Đặt F&B: Phải kiểm tra tồn kho (Stock). Lưu đơn xuống DB với trạng thái mặc định "Chờ xác nhận" (PENDING).
  - UX: Hiển thị thông báo thành công và in chi tiết **hóa đơn dự kiến** ra Console.
4. **Cập Nhật Tiến Độ (Staff & Customer):**
  - Staff có quyền duyệt đơn theo flow bắt buộc: `Đã xác nhận` -> `Đang phục vụ` -> `Đã thanh toán/Hoàn thành`.
  - Customer có menu Tra cứu trạng thái đơn hàng realtime.

## 🗺️ Roadmap & TODOs
### Phase 1: Core & Admin (Đã hoàn thành ✅)
- [x] Kiến trúc Core Base MVC 3 Tầng + Setup JDBC.
- [x] Tiện ích `ColorConst`, `InputUtils`, `PrintUtils`.
- [x] Hệ thống Session Auth (Băm mật khẩu, Validation < 6 chars, Phân quyền Routing).
- [x] Quản trị kho phần cứng (`Computer`): Thêm/Sửa/Xóa (có Confirm Y/N).
- [x] Quản trị kho F&B (`ServiceItem`): Hiển thị bảng, validate ID/giá/tồn kho.

### Phase 2: Customer & Staff Workflow (Đang phát triển 🚧)
- [ ] Tính năng Khách hàng Đặt máy: Chỉ hiện máy Trống, chặn trùng lịch.
- [ ] Tính năng Khách hàng Gọi món F&B: Chọn menu, lưu DB trạng thái "Chờ xác nhận", in bill dự kiến.
- [ ] Phân hệ Staff: Xem danh sách chờ.
- [ ] Cập nhật trạng thái Workflow (Đã xác nhận -> Đang phục vụ -> Hoàn thành).
- [ ] Tra cứu trạng thái (Dành cho Customer).

### Phase 3: Nâng cao (Advanced Features 🌟)
- [ ] **Ví điện tử nội bộ:** Nạp tiền và tự động trừ tiền theo giờ chơi/đơn hàng (Transaction Safe).
- [ ] **Quản lý tồn kho tự động:** Tự động trừ số lượng nguyên liệu/F&B khi đơn hàng thành công.
- [ ] **Báo cáo thống kê:** Xuất doanh thu ngày/tháng, món ăn bán chạy.
- [ ] **Khuyến mãi (Optional):** Mã giảm giá, giảm giá giờ vàng.