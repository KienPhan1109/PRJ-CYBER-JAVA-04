package com.cyber.view;

import com.cyber.exception.BusinessException;
import com.cyber.model.Computer;
import com.cyber.service.ComputerService;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;

import java.math.BigDecimal;
import java.util.List;

public class ComputerManagementView {

    private final ComputerService computerService;

    public ComputerManagementView() {
        this.computerService = ComputerService.getInstance();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n--- QUẢN LÝ MÁY TRẠM ---");
            System.out.println("1. Xem danh sách máy");
            System.out.println("2. Thêm máy trạm mới");
            System.out.println("3. Sửa thông tin máy");
            System.out.println("4. Xoá máy trạm");
            System.out.println("0. Quay lại");

            int choice = InputUtils.inputInt("Chọn chức năng (0-4): ", 0, 4);

            switch (choice) {
                case 1:
                    displayList();
                    break;
                case 2:
                    handleAdd();
                    break;
                case 3:
                    handleEdit();
                    break;
                case 4:
                    handleDelete();
                    break;
                case 0:
                    return;
            }
        }
    }

    private void displayList() {
        try {
            List<Computer> computers = computerService.getAllComputers();
            System.out.println("\n==========================================================================================================");
            System.out.printf("%-10s | %-15s | %-15s | %-30s | %-12s | %-15s%n", "ID Máy", "Tên", "Khu Vực", "Cấu hình", "Trạng thái", "Giá/Giờ");
            System.out.println("----------------------------------------------------------------------------------------------------------");
            
            if (computers.isEmpty()) {
                System.out.println("Chưa có dữ liệu máy trạm trong hệ thống.");
            } else {
                for (Computer c : computers) {
                    System.out.printf("%-10s | %-15s | %-15s | %-30s | %-12s | %-15s%n",
                            FormatUtils.formatId("C", c.getComputerId()),
                            c.getName(),
                            c.getZone() != null ? c.getZone().name() : "N/A",
                            c.getHardwareConfig(),
                            FormatUtils.formatComputerStatus(c.getStatus()),
                            FormatUtils.formatVND(c.getPricePerHour()));
                }
            }
            System.out.println("==========================================================================================================");
        } catch (BusinessException e) {
            System.out.println("\033[31m[LỖI] " + e.getMessage() + "\033[0m");
        }
    }

    private void handleAdd() {
        System.out.println("\n--- THÊM MÁY TRẠM MỚI ---");
        String name = InputUtils.inputString("Nhập tên máy (vd VIP-03): ");
        System.out.println("Chọn khu vực: 1. VIP | 2. STANDARD | 3. ESPORT | 4. STREAMING | 5. COUPLE");
        int zoneChoice = InputUtils.inputInt("Lựa chọn (1-5): ", 1, 5);
        com.cyber.model.enums.ComputerZone zone;
        switch (zoneChoice) {
            case 1: zone = com.cyber.model.enums.ComputerZone.VIP; break;
            case 2: zone = com.cyber.model.enums.ComputerZone.STANDARD; break;
            case 3: zone = com.cyber.model.enums.ComputerZone.ESPORT; break;
            case 4: zone = com.cyber.model.enums.ComputerZone.STREAMING; break;
            case 5: zone = com.cyber.model.enums.ComputerZone.COUPLE; break;
            default: zone = com.cyber.model.enums.ComputerZone.STANDARD;
        }
        
        String config = InputUtils.inputString("Nhập cấu hình máy: ");
        BigDecimal price = InputUtils.inputBigDecimal("Nhập giá/giờ (đ): ", BigDecimal.ZERO);

        // Mặc định tạo mới là AVAILABLE
        Computer newComp = new Computer(name, zone, config, com.cyber.model.enums.ComputerStatus.AVAILABLE, price);
        
        try {
            computerService.addComputer(newComp);
            System.out.println("\033[32m[THÀNH CÔNG] Thêm máy trạm thành công!\033[0m");
        } catch (BusinessException e) {
            System.out.println("\033[31m[LỖI] " + e.getMessage() + "\033[0m");
        }
    }

    private void handleEdit() {
        System.out.println("\n--- SỬA THÔNG TIN MÁY TRẠM ---");
        int id = InputUtils.inputInt("Nhập ID máy (số nguyên, ví dụ máy C-001 thì nhập 1): ");
        try {
            Computer existing = computerService.getComputerById(id);
            if (existing == null) {
                System.out.println("\033[33m[THÔNG BÁO] Không tìm thấy máy tính nào với ID " + FormatUtils.formatId("C", id) + "\033[0m");
                return;
            }
            System.out.println("Đang chỉnh sửa cho: " + existing.getName());
            
            String name = InputUtils.inputString("Nhập tên máy mới (Cũ: " + existing.getName() + "): ");
            
            System.out.println("Chọn khu vực mới (Cũ: " + (existing.getZone() != null ? existing.getZone().name() : "N/A") + "):");
            System.out.println("1. VIP | 2. STANDARD | 3. ESPORT | 4. STREAMING | 5. COUPLE");
            int zoneChoice = InputUtils.inputInt("Lựa chọn mới (1-5): ", 1, 5);
            com.cyber.model.enums.ComputerZone zone;
            switch (zoneChoice) {
                case 1: zone = com.cyber.model.enums.ComputerZone.VIP; break;
                case 2: zone = com.cyber.model.enums.ComputerZone.STANDARD; break;
                case 3: zone = com.cyber.model.enums.ComputerZone.ESPORT; break;
                case 4: zone = com.cyber.model.enums.ComputerZone.STREAMING; break;
                case 5: zone = com.cyber.model.enums.ComputerZone.COUPLE; break;
                default: zone = com.cyber.model.enums.ComputerZone.STANDARD;
            }
            
            String config = InputUtils.inputString("Nhập cấu hình mới (Cũ: " + existing.getHardwareConfig() + "): ");
            System.out.println("Chọn trạng thái (Cũ: " + FormatUtils.formatComputerStatus(existing.getStatus()) + "): 1. AVAILABLE  |  2. IN_USE  |  3. MAINTENANCE");
            int stChoice = InputUtils.inputInt("Lựa chọn mới (1-3): ", 1, 3);
            com.cyber.model.enums.ComputerStatus status = stChoice == 1 ? com.cyber.model.enums.ComputerStatus.AVAILABLE : 
                    (stChoice == 2 ? com.cyber.model.enums.ComputerStatus.IN_USE : com.cyber.model.enums.ComputerStatus.MAINTENANCE);
            
            BigDecimal price = InputUtils.inputBigDecimal("Nhập giá/giờ (Cũ: " + FormatUtils.formatVND(existing.getPricePerHour()) + "): ", BigDecimal.ZERO);

            Computer updated = new Computer(name, zone, config, status, price);
            updated.setComputerId(id); // Giữ ID gốc
            
            computerService.updateComputer(updated);
            System.out.println("\033[32m[THÀNH CÔNG] Cập nhật thông tin thành công!\033[0m");
            
        } catch (BusinessException e) {
            System.out.println("\033[31m[LỖI] " + e.getMessage() + "\033[0m");
        }
    }

    private void handleDelete() {
        System.out.println("\n--- XÓA MÁY TRẠM ---");
        int id = InputUtils.inputInt("Nhập ID máy cần xóa (số nguyên): ");
        String confirm = InputUtils.inputString("Bạn có chắc chắn muốn xóa không? (Y/N): ", "^[YyNn]$", "Chỉ nhập Y hoặc N.");
        
        if (confirm.equalsIgnoreCase("Y")) {
            try {
                computerService.deleteComputer(id);
                System.out.println("\033[32m[THÀNH CÔNG] Xóa máy trạm thành công!\033[0m");
            } catch (BusinessException e) {
                System.out.println("\033[31m[LỖI] " + e.getMessage() + "\033[0m");
            }
        } else {
            System.out.println("Đã hủy thao tác xóa.");
        }
    }
}
