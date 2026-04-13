package com.cyber.domain.fb;
import com.cyber.model.enums.FBStatus;
import com.cyber.model.enums.FbTemperature;
import com.cyber.util.InputUtils;
import com.cyber.util.FormatUtils;

import java.math.BigDecimal;

public class FbMenuItem {
    private int menuItemId;
    private int categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private int stockQuantity;
    private int prepTimeInMinutes;

    private String availability;
    private FbTemperature temperatureLevel;
    private FBStatus status;
    private boolean isDeleted;

    public FbMenuItem() {
    }

    public FbMenuItem(int menuItemId, int categoryId, String name,
                      String description, BigDecimal basePrice,
                      int stockQuantity, int prepTimeInMinutes,
                      String availability,
                      FbTemperature temperatureLevel, FBStatus status, boolean isDeleted) {
        this.menuItemId = menuItemId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.stockQuantity = stockQuantity;
        this.prepTimeInMinutes = prepTimeInMinutes;
        this.availability = availability;
        this.temperatureLevel = temperatureLevel;
        this.status = status;
        this.isDeleted = isDeleted;
    }

    /**
     * Nhập dữ liệu cho FbMenuItem (dùng chung cho cả Thêm và Sửa).
     * Tên món đã được validate trùng lặp từ bên ngoài trước khi gọi vào đây.
     *
     * @param isEdit        true = chế độ sửa (hiển thị giá trị cũ, Enter giữ nguyên)
     * @param validatedName tên món đã qua validate (không null)
     */
    public void inputData(boolean isEdit, String validatedName) {
        this.name = validatedName;

        // Danh mục
        System.out.println(isEdit ? "Danh mục (Cũ: " + this.categoryId + "):" : "Chọn danh mục:");
        System.out.println("1. FOOD | 2. DRINK | 3. SNACK | 4. TOPPING");
        if (isEdit) {
            int choice = InputUtils.inputIntUpdate(
                    "Lựa chọn mới (1-4) [Enter giữ nguyên]: ", this.categoryId, 1, 4);
            this.categoryId = choice;
        } else {
            int choice = InputUtils.inputInt("Lựa chọn (1-4): ", 1, 4);
            this.categoryId = choice;
        }

        // Mô tả (cho phép null)
        if (isEdit) {
            String oldDesc = this.description != null ? this.description : "";
            this.description = InputUtils.inputStringUpdate(
                    "Mô tả mới (Cũ: " + (oldDesc.isEmpty() ? "Không có" : oldDesc) + ") [Enter giữ nguyên]: ", oldDesc);
            if (this.description != null && this.description.isBlank()) this.description = null;
        } else {
            String desc = InputUtils.inputStringOptional("Mô tả (Enter để bỏ qua): ");
            this.description = desc.isEmpty() ? null : desc;
        }

        // Giá gốc
        if (isEdit) {
            this.basePrice = InputUtils.inputBigDecimalUpdate(
                    "Giá gốc mới (Cũ: " + FormatUtils.formatVND(this.basePrice) + ") [Enter giữ nguyên]: ", this.basePrice, BigDecimal.ZERO);
        } else {
            this.basePrice = InputUtils.inputBigDecimal("Giá gốc (VND): ", BigDecimal.ZERO);
        }

        // Tồn kho
        if (isEdit) {
            this.stockQuantity = InputUtils.inputIntUpdate(
                    "Tồn kho mới (Cũ: " + this.stockQuantity + ") [Enter giữ nguyên]: ", this.stockQuantity, 0, 99999);
        } else {
            this.stockQuantity = InputUtils.inputInt("Tồn kho ban đầu: ", 0, 99999);
        }

        // Thời gian chuẩn bị
        if (isEdit) {
            this.prepTimeInMinutes = InputUtils.inputIntUpdate(
                    "Thời gian chuẩn bị mới (Cũ: " + this.prepTimeInMinutes + " phút) [Enter giữ nguyên]: ", this.prepTimeInMinutes, 0, 120);
        } else {
            this.prepTimeInMinutes = InputUtils.inputInt("Thời gian chuẩn bị (phút): ", 0, 120);
        }

        // Khung giờ phục vụ
        if (isEdit) {
            this.availability = InputUtils.inputStringUpdate(
                    "Khung giờ phục vụ mới (Cũ: " + this.availability + ") [Enter giữ nguyên]: ", this.availability != null ? this.availability : "ALL");
        } else {
            this.availability = InputUtils.inputString("Khung giờ phục vụ (ALL hoặc VD: 06:00-22:00): ");
        }

        // Nhiệt độ
        System.out.println(isEdit ? "Nhiệt độ (Cũ: " + FormatUtils.formatFbTemperature(this.temperatureLevel) + "):" : "Chọn nhiệt độ:");
        System.out.println("1. HOT | 2. COLD | 3. ICED | 4. NONE");
        if (isEdit) {
            int oldTempChoice = switch (this.temperatureLevel) {
                case HOT -> 1;
                case COLD -> 2;
                case ICED -> 3;
                default -> 4;
            };
            String tInput = InputUtils.inputStringUpdate("Lựa chọn mới (1-4) [Enter giữ nguyên]: ", "").trim();
            if (!tInput.isEmpty()) {
                try {
                    int tChoice = Integer.parseInt(tInput);
                    this.temperatureLevel = switch (tChoice) {
                        case 1 -> FbTemperature.HOT;
                        case 2 -> FbTemperature.COLD;
                        case 3 -> FbTemperature.ICED;
                        default -> FbTemperature.NONE;
                    };
                } catch (Exception e) {
                    // giữ nguyên
                }
            }
        } else {
            int tempChoice = InputUtils.inputInt("Lựa chọn (1-4): ", 1, 4);
            this.temperatureLevel = switch (tempChoice) {
                case 1 -> FbTemperature.HOT;
                case 2 -> FbTemperature.COLD;
                case 3 -> FbTemperature.ICED;
                default -> FbTemperature.NONE;
            };
        }

        // Status tự động
        this.status = this.stockQuantity == 0 ? FBStatus.OUT_OF_STOCK : FBStatus.ACTIVE;
    }

    public int getMenuItemId() { return menuItemId; }
    public void setMenuItemId(int menuItemId) { this.menuItemId = menuItemId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public int getPrepTimeInMinutes() { return prepTimeInMinutes; }
    public void setPrepTimeInMinutes(int prepTimeInMinutes) { this.prepTimeInMinutes = prepTimeInMinutes; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public FbTemperature getTemperatureLevel() { return temperatureLevel; }
    public void setTemperatureLevel(FbTemperature temperatureLevel) { this.temperatureLevel = temperatureLevel; }

    public FBStatus getStatus() { return status; }
    public void setStatus(FBStatus status) { this.status = status; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}
