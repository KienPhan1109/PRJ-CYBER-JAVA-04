package com.cyber.model;

import com.cyber.util.PrintUtils;

import com.cyber.model.enums.ComputerStatus;
import com.cyber.model.enums.ComputerZone;

import java.math.BigDecimal;

public class Computer {
    private int computerId;
    private String name;
    private ComputerZone zone;
    private String hardwareConfig;
    private ComputerStatus status;
    private BigDecimal pricePerHour;
    private boolean isDeleted;

    public Computer() {}

    public void inputData(boolean isEdit, String preValidatedName) {
        this.name = preValidatedName;

        // ZONE
        System.out.println(isEdit ? "Chọn khu vực mới (Cũ: " + (this.zone != null ? this.zone.name() : "N/A") + "):" : "Chọn khu vực:");
        System.out.println("1. VIP | 2. STANDARD | 3. ESPORT | 4. STREAMING | 5. COUPLE");
        
        int zoneChoice;
        if (isEdit) {
            String zInput = com.cyber.util.InputUtils.inputStringUpdate("Lựa chọn mới (1-5) [Enter để giữ nguyên]: ", "").trim();
            if (!zInput.isEmpty()) {
                try {
                    zoneChoice = Integer.parseInt(zInput);
                    setZoneByChoice(zoneChoice);
                } catch (Exception e) {
                    PrintUtils.printError("Giá trị nhập không hợp lệ, giữ nguyên khu vực cũ.");
                }
            }
        } else {
            zoneChoice = com.cyber.util.InputUtils.inputInt("Lựa chọn (1-5): ", 1, 5);
            setZoneByChoice(zoneChoice);
        }

        // HARDWARE CONFIG
        if (isEdit) {
            this.hardwareConfig = com.cyber.util.InputUtils.inputStringUpdate("Nhập cấu hình mới (Cũ: " + this.hardwareConfig + ") [Enter để giữ nguyên]: ", this.hardwareConfig);
        } else {
            this.hardwareConfig = com.cyber.util.InputUtils.inputString("Nhập cấu hình máy: ");
        }

        // STATUS (only allow manual status for edit, add defaults to AVAILABLE)
        if (isEdit) {
            System.out.println("Chọn trạng thái (Cũ: " + com.cyber.util.FormatUtils.formatComputerStatus(this.status) + "):");
            System.out.println("1. AVAILABLE  |  2. IN_USE  |  3. MAINTENANCE");
            String stInput = com.cyber.util.InputUtils.inputStringUpdate("Lựa chọn mới (1-3) [Enter để giữ nguyên]: ", "").trim();
            if (!stInput.isEmpty()) {
                try {
                    int stChoice = Integer.parseInt(stInput);
                    if (stChoice == 1) this.status = com.cyber.model.enums.ComputerStatus.AVAILABLE;
                    else if (stChoice == 2) this.status = com.cyber.model.enums.ComputerStatus.IN_USE;
                    else if (stChoice == 3) this.status = com.cyber.model.enums.ComputerStatus.MAINTENANCE;
                } catch (Exception e) {
                    PrintUtils.printError("Định dạng không hợp lệ, giữ nguyên trạng thái cũ.");
                }
            }
        } else {
            this.status = com.cyber.model.enums.ComputerStatus.AVAILABLE;
        }

        // PRICE
        if (isEdit) {
            this.pricePerHour = com.cyber.util.InputUtils.inputBigDecimalUpdate("Nhập giá/giờ (Cũ: " + com.cyber.util.FormatUtils.formatVND(this.pricePerHour) + ") [Enter để giữ nguyên]: ", this.pricePerHour, java.math.BigDecimal.ZERO);
        } else {
            this.pricePerHour = com.cyber.util.InputUtils.inputBigDecimal("Nhập giá/giờ (đ): ", java.math.BigDecimal.ZERO);
        }
    }

    private void setZoneByChoice(int choice) {
        switch (choice) {
            case 1: this.zone = com.cyber.model.enums.ComputerZone.VIP; break;
            case 2: this.zone = com.cyber.model.enums.ComputerZone.STANDARD; break;
            case 3: this.zone = com.cyber.model.enums.ComputerZone.ESPORT; break;
            case 4: this.zone = com.cyber.model.enums.ComputerZone.STREAMING; break;
            case 5: this.zone = com.cyber.model.enums.ComputerZone.COUPLE; break;
            default: this.zone = com.cyber.model.enums.ComputerZone.STANDARD;
        }
    }

    public int getComputerId() {
        return computerId;
    }

    public void setComputerId(int computerId) {
        this.computerId = computerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ComputerZone getZone() {
        return zone;
    }

    public void setZone(ComputerZone zone) {
        this.zone = zone;
    }

    public String getHardwareConfig() {
        return hardwareConfig;
    }

    public void setHardwareConfig(String hardwareConfig) {
        this.hardwareConfig = hardwareConfig;
    }

    public ComputerStatus getStatus() {
        return status;
    }

    public void setStatus(ComputerStatus status) {
        this.status = status;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
}