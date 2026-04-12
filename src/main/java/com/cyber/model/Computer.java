package com.cyber.model;

import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;
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

        System.out.println(isEdit ? "Chọn khu vực mới (Cũ: " + (this.zone != null ? this.zone.name() : "N/A") + "):" : "Chọn khu vực:");
        System.out.println("1. VIP | 2. STANDARD | 3. ESPORT | 4. STREAMING | 5. COUPLE");
        
        int zoneChoice;
        if (isEdit) {
            String zInput = InputUtils.inputStringUpdate("Lựa chọn mới (1-5) [Enter để giữ nguyên]: ", "").trim();
            if (!zInput.isEmpty()) {
                try {
                    zoneChoice = Integer.parseInt(zInput);
                    setZoneByChoice(zoneChoice);
                } catch (Exception e) {
                    PrintUtils.printError("Giá trị nhập không hợp lệ, giữ nguyên khu vực cũ.");
                }
            }
        } else {
            zoneChoice = InputUtils.inputInt("Lựa chọn (1-5): ", 1, 5);
            setZoneByChoice(zoneChoice);
        }

        if (isEdit) {
            this.hardwareConfig = InputUtils.inputStringUpdate("Nhập cấu hình mới (Cũ: " + this.hardwareConfig + ") [Enter để giữ nguyên]: ", this.hardwareConfig);
        } else {
            this.hardwareConfig = InputUtils.inputString("Nhập cấu hình máy: ");
        }

        if (isEdit) {
            System.out.println("Chọn trạng thái (Cũ: " + FormatUtils.formatComputerStatus(this.status) + "):");
            System.out.println("1. AVAILABLE  |  2. MAINTENANCE");
            String stInput = InputUtils.inputStringUpdate("Lựa chọn mới (1-2) [Enter để giữ nguyên]: ", "").trim();
            if (!stInput.isEmpty()) {
                try {
                    int stChoice = Integer.parseInt(stInput);
                    if (stChoice == 1) this.status = ComputerStatus.AVAILABLE;
                    else if (stChoice == 2) this.status = ComputerStatus.MAINTENANCE;
                    else PrintUtils.printError("Chỉ chọn 1 hoặc 2, giữ nguyên trạng thái cũ.");
                } catch (Exception e) {
                    PrintUtils.printError("Định dạng không hợp lệ, giữ nguyên trạng thái cũ.");
                }
            }
        } else {
            this.status = ComputerStatus.AVAILABLE;
        }

        if (isEdit) {
            this.pricePerHour = InputUtils.inputBigDecimalUpdate("Nhập giá/giờ (Cũ: " + FormatUtils.formatVND(this.pricePerHour) + ") [Enter để giữ nguyên]: ", this.pricePerHour, BigDecimal.ZERO);
        } else {
            this.pricePerHour = InputUtils.inputBigDecimal("Nhập giá/giờ (đ): ", BigDecimal.ZERO);
        }
    }

    private void setZoneByChoice(int choice) {
        switch (choice) {
            case 1 -> this.zone = ComputerZone.VIP;
            case 3 -> this.zone = ComputerZone.ESPORT;
            case 4 -> this.zone = ComputerZone.STREAMING;
            case 5 -> this.zone = ComputerZone.COUPLE;
            default -> this.zone = ComputerZone.STANDARD;
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