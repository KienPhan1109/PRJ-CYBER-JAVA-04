package com.cyber.view;

import com.cyber.util.PrintUtils;

import com.cyber.exception.BusinessException;
import com.cyber.model.Computer;
import com.cyber.model.User;
import com.cyber.service.ComputerService;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;

import java.util.List;

public class ComputerManagementView {
    private final ComputerService computerService;
    private final User adminUser;

    public ComputerManagementView(User adminUser) {
        this.computerService = ComputerService.getInstance();
        this.adminUser = adminUser;
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n====================================");
            System.out.println("          QUẢN LÝ MÁY TRẠM          ");
            System.out.println("====================================");
            System.out.println("1. Xem danh sách máy");
            System.out.println("2. Thêm máy trạm mới");
            System.out.println("3. Sửa thông tin máy");
            System.out.println("4. Ẩn/Hiện máy trạm");
            System.out.println("0. Quay lại");
            System.out.println("====================================");

            int choice = InputUtils.inputInt("Chọn chức năng (0-4): ", 0, 4);

            switch (choice) {
                case 1 -> displayList();
                case 2 -> handleAdd();
                case 3 -> handleEdit();
                case 4 -> handleToggleHideShow();
                case 0 -> {
                    return;
                }
            }
        }
    }

    private void displayList() {
        try {
            List<Computer> computers = computerService.getAllComputers();
            System.out.println("\n========================================================================================================================================");
            System.out.println("                                                     DANH SÁCH MÁY TOÀN HỆ THỐNG                                                        ");
            System.out.println("========================================================================================================================================");
            System.out.printf("%-7s | %-15s | %-15s | %-50s | %-18s | %-15s\n", "ID Máy", "Tên", "Khu Vực", "Cấu hình", "Trạng thái", "Giá / Giờ");
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
            
            if (computers.isEmpty()) {
                System.out.println("Chưa có dữ liệu máy trạm trong hệ thống.");
            } else {
                for (Computer c : computers) {
                    System.out.printf("%-7s | %-15s | %-15s | %-50s | %-27s | %-15s\n",
                            FormatUtils.formatId("C", c.getComputerId()),
                            c.getName(),
                            c.getZone() != null ? c.getZone().name() : "N/A",
                            c.getHardwareConfig(),
                            FormatUtils.formatComputerStatus(c.getStatus()),
                            FormatUtils.formatVND(c.getPricePerHour()));
                }
            }
            System.out.println("========================================================================================================================================");
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleAdd() {
        System.out.println("\n--- THÊM MÁY TRẠM MỚI ---");
        String name;
        while (true) {
            name = InputUtils.inputString("Nhập tên máy (vd VIP-03): ");
            try {
                if (computerService.isNameExists(name)) {
                    PrintUtils.printWarning("Tên máy '" + name + "' đã tồn tại. Vui lòng nhập tên khác.");
                } else {
                    break;
                }
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
            }
        }
        Computer newComp = new Computer();
        newComp.inputData(false, name);
        
        try {
            computerService.addComputer(newComp, adminUser);
            System.out.println("\033[32m[THÀNH CÔNG] Thêm máy trạm thành công!\033[0m");
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleEdit() {
        System.out.println("\n--- SỬA THÔNG TIN MÁY TRẠM ---");
        int id = InputUtils.inputInt("Nhập ID máy (số nguyên, ví dụ máy C-001 thì nhập 1): ");
        try {
            Computer existing = computerService.getComputerById(id);
            if (existing == null) {
                PrintUtils.printWarning("Không tìm thấy máy tính nào với ID " + FormatUtils.formatId("C", id) + "");
                return;
            }
            if (existing.getStatus() == com.cyber.model.enums.ComputerStatus.IN_USE) {
                throw new BusinessException("IN_USE", "Máy đang có khách sử dụng (IN_USE), không thể chỉnh sửa lúc này.");
            }
            if (existing.getStatus() == com.cyber.model.enums.ComputerStatus.HIDDEN) {
                throw new BusinessException("HIDDEN", "Máy đang bị ẩn (HIDDEN), vui lòng Hiện máy trước khi chỉnh sửa.");
            }
            System.out.println("Đang chỉnh sửa cho: " + existing.getName());
            
            String name;
            while (true) {
                name = InputUtils.inputStringUpdate("Nhập tên máy mới (Cũ: " + existing.getName() + ") [Enter để giữ nguyên]: ", existing.getName());
                if (name.equalsIgnoreCase(existing.getName())) {
                    break;
                }
                try {
                    if (computerService.isNameExists(name)) {
                        PrintUtils.printWarning("Tên máy '" + name + "' đã được sử dụng bởi máy khác. Vui lòng nhập tên khác.");
                    } else {
                        break;
                    }
                } catch (BusinessException e) {
                    PrintUtils.printError(e.getMessage());
                }
            }
            
            existing.inputData(true, name);
            
            computerService.updateComputer(existing, adminUser);
            System.out.println("\033[32m[THÀNH CÔNG] Cập nhật thông tin thành công!\033[0m");
            
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleToggleHideShow() {
        System.out.println("\n--- ẨN / HIỆN MÁY TRẠM ---");
        int id = InputUtils.inputInt("Nhập ID máy cần Ẩn/Hiện (số nguyên): ");
        String confirm = InputUtils.inputString("Bạn có chắc chắn muốn thay đổi trạng thái máy không? (Y/N): ", "^[YyNn]$", "Chỉ nhập Y hoặc N.");
        
        if (confirm.equalsIgnoreCase("Y")) {
            try {
                computerService.toggleComputerStatus(id, adminUser);
                System.out.println("\033[32m[THÀNH CÔNG] Đã thay đổi trạng thái máy trạm!\033[0m");
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
            }
        } else {
            System.out.println("Đã hủy thao tác.");
        }
    }
}
