# Phân tích hệ thống và Kế hoạch thay đổi

Tài liệu này phân tích các yêu cầu thay đổi đối với hệ thống Cyber Cafe & F&B Management để đơn giản hóa nghiệp vụ và cải thiện trải nghiệm quản trị.

---

## 1. Yêu cầu 1: Loại bỏ Topping và Item Options (Decorator Pattern)

### Thực trạng hiện tại:
Hệ thống đang sử dụng mẫu thiết kế **Decorator** để xử lý các món ăn có nhiều tùy chọn (Size, Đường, Đá, Toppings). Cấu trúc này bao gồm:
- Các lớp Decorator: `ToppingDecorator`, `SizeDecorator`, `WeightDecorator`, `ItemDecorator`.
- Các Interface/Model: `IBillable`, `SingleItem`.
- Hai bảng cơ sở dữ liệu: `fb_toppings` và `fb_item_options`.
- Logic ordering phức tạp (JSON config) trong `FbOrderService` và `CustomerMainView`.

### Giải pháp đề xuất:
- **Xóa bỏ hoàn toàn** các file liên quan đến Decorator pattern (`com.cyber.domain.fb` và các Decorator classes).
- **Xóa bỏ logic Item Options**: Gỡ bỏ bảng `fb_item_options` và các tính năng liên quan (chọn Size, Đường, Đá).
- **Đơn giản hóa Topping**: 
    - Gỡ bỏ việc tích hợp Topping vào món ăn chính theo dạng Decorator (khách không chọn topping khi đặt nước nữa).
    - Nếu cần bán Topping, Admin có thể thêm chúng như một món riêng biệt trong Menu hoặc quản lý danh sách Topping đơn giản.
    - Cập nhật `FbOrderService` để loại bỏ logic `ItemConfigJson`.

---

## 2. Yêu cầu 2: Thay đổi logic xóa Máy trạm (Soft Delete / Toggle)

### Thực trạng hiện tại:
- Khi xóa máy trạm, hệ thống gọi `deleteComputer` thực hiện xóa (hoặc đánh dấu `isDeleted`).
- Người dùng muốn máy trạm có thể "Ẩn/Hiện" tương tự như món ăn F&B (chuyển sang trạng thái `HIDDEN` thay vì xóa vĩnh viễn).

### Giải pháp đề xuất:
- Thêm trạng thái `HIDDEN` vào enum `ComputerStatus` (nếu cần) hoặc sử dụng cờ `isDeleted` để Filter.
- Tuy nhiên, để giống F&B nhất, ta nên thêm trạng thái `HIDDEN` để Admin có thể Toggle (Bật/Tắt) máy trạm.
- Cập nhật `ComputerManagementView` từ "Xóa" thành "Ẩn/Hiện món".

---

## 3. Yêu cầu 3: Sắp xếp danh sách F&B theo ID

### Thực trạng hiện tại:
- Danh sách F&B trong DAO đang được sắp xếp theo `category_name` và `name`.

### Giải pháp đề xuất:
- Cập nhật SQL trong `FbMenuItemDAOImpl.java` để `ORDER BY menu_item_id`.

---

## 4. Yêu cầu 4: Logic Tồn kho & Trạng thái Topping

### Yêu cầu:
- Trong phần quản lý Topping (đã được đơn giản hóa), nếu `stockQuantity = 0` thì trạng thái phải chuyển thành `OUT_OF_STOCK`.

### Giải pháp thực hiện:
- Cập nhật logic trong `FbOptionDAOImpl` (hoặc Service quản lý Topping mới) để tự động hóa việc này khi Admin cập nhật tồn kho hoặc khi khách đặt hàng làm giảm tồn kho về 0.

---

## Danh sách các file sẽ bị ảnh hưởng (Dự kiến)

### [DELETE]
- `src/main/java/com/cyber/domain/fb/IBillable.java`
- `src/main/java/com/cyber/domain/fb/SingleItem.java`
- `src/main/java/com/cyber/domain/fb/ItemDecorator.java`
- `src/main/java/com/cyber/domain/fb/ToppingDecorator.java`
- `src/main/java/com/cyber/domain/fb/SizeDecorator.java`
- `src/main/java/com/cyber/domain/fb/WeightDecorator.java`

### [MODIFY]
- `src/main/java/com/cyber/view/FbMenuManagementView.java`: Gỡ bỏ option 5, 6 (hoặc đơn giản hóa option 5).
- `src/main/java/com/cyber/view/CustomerMainView.java`: Gỡ bỏ giao diện chọn option/topping.
- `src/main/java/com/cyber/view/ComputerManagementView.java`: Thay đổi logic Xóa thành Ẩn/Hiện.
- `src/main/java/com/cyber/service/FbOrderService.java`: Gỡ bỏ logic `FbAdvancedCartItem`.
- `src/main/java/com/cyber/dao/impl/FbMenuItemDAOImpl.java`: Đổi Sort Order.
- `src/main/java/com/cyber/dao/impl/FbOptionDAOImpl.java`: Cập nhật logic `OUT_OF_STOCK`.
- `src/main/java/com/cyber/model/enums/ComputerStatus.java`: Thêm `HIDDEN`.
