package com.cyber.model;

import com.cyber.util.PrintUtils;

import com.cyber.model.enums.ServiceItemStatus;

import java.math.BigDecimal;

public class ServiceItem {
    private int itemId;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private ServiceItemStatus status;

    public ServiceItem() {}

    public void inputData(boolean isEdit) {
        if (isEdit) {
            this.name = com.cyber.util.InputUtils.inputStringUpdate("Tên mới (Cũ: " + this.name + ") [Enter để giữ nguyên]: ", this.name);
            this.description = com.cyber.util.InputUtils.inputStringUpdate("Mô tả mới (Cũ: " + this.description + ") [Enter để giữ nguyên]: ", this.description);
            this.price = com.cyber.util.InputUtils.inputBigDecimalUpdate("Giá tiền mới (Cũ: " + com.cyber.util.FormatUtils.formatVND(this.price) + ") [Enter để giữ nguyên]: ", this.price, java.math.BigDecimal.ZERO);
            this.stockQuantity = com.cyber.util.InputUtils.inputIntUpdate("Tồn kho mới (Cũ: " + this.stockQuantity + ") [Enter để giữ nguyên]: ", this.stockQuantity, 0, 10000);
            
            System.out.println("Trạng thái (Cũ: " + com.cyber.util.FormatUtils.formatServiceItemStatus(this.status) + "): 1. ACTIVE  |  2. OUT_OF_STOCK");
            String stInput = com.cyber.util.InputUtils.inputStringUpdate("Lựa chọn (1-2) [Enter để giữ nguyên]: ", "").trim();
            if (!stInput.isEmpty()) {
                try {
                    int stChoice = Integer.parseInt(stInput);
                    if (stChoice == 1) this.status = com.cyber.model.enums.ServiceItemStatus.ACTIVE;
                    else if (stChoice == 2) this.status = com.cyber.model.enums.ServiceItemStatus.OUT_OF_STOCK;
                } catch (Exception e) {
                    PrintUtils.printError("Định dạng không hợp lệ, giữ nguyên trạng thái cũ.");
                }
            }
        } else {
            this.name = com.cyber.util.InputUtils.inputString("Nhập tên món: ");
            this.description = com.cyber.util.InputUtils.inputString("Nhập mô tả món: ");
            this.price = com.cyber.util.InputUtils.inputBigDecimal("Nhập giá tiền (đ): ", java.math.BigDecimal.ZERO);
            this.stockQuantity = com.cyber.util.InputUtils.inputInt("Nhập số lượng tồn kho ban đầu: ", 0, 10000);
            this.status = com.cyber.model.enums.ServiceItemStatus.ACTIVE;
        }
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public ServiceItemStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceItemStatus status) {
        this.status = status;
    }
}