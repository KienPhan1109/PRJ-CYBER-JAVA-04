package com.cyber.view;

import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

import com.cyber.exception.BusinessException;
import com.cyber.model.Computer;
import com.cyber.model.User;
import com.cyber.service.ComputerService;

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
            System.out.println("5. Xóa máy trạm");
            System.out.println("0. Quay lại");
            System.out.println("====================================");

            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-5): ", 0, 5);

            switch (choice) {
                case 1 -> displayList();
                case 2 -> handleAdd();
                case 3 -> handleEdit();
                case 4 -> handleToggleHideShow();
                case 5 -> handleDelete();
                case 0 -> {
                    return;
                }
            }
        }
    }

    private void displayList() {
        try {
            List<Computer> computers = computerService.getAllComputers();
            System.out.println("\n" + "=".repeat(100));
            System.out.println("  DANH SÁCH MÁY TOÀN HỆ THỐNG");
            System.out.println("=".repeat(100));
            System.out.printf("%-7s | %-15s | %-12s | %-30s | %-12s | %-15s%n", "ID Máy", "Tên", "Khu Vực", "Cấu hình", "Trạng thái", "Giá / Giờ");
            System.out.println("-".repeat(100));

            for (Computer c : computers) {
                System.out.printf("%-7s | %-15s | %-12s | %-30s | %-21s | %-15s%n",
                        FormatUtils.formatId("C", c.getComputerId()),
                        FormatUtils.truncate(c.getName(), 15),
                        FormatUtils.formatValue(c.getZone()),
                        FormatUtils.truncate(c.getHardwareConfig(), 30),
                        FormatUtils.formatComputerStatus(c.getStatus()),
                        FormatUtils.formatVND(c.getPricePerHour())
                );
            }
            System.out.println("=".repeat(100));
            System.out.println("Tổng cộng: " + computers.size() + " máy tính.");
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
            PrintUtils.printSuccess("Thêm máy trạm thành công!");
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
            PrintUtils.printSuccess("Cập nhật thông tin thành công!");
            
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleToggleHideShow() {
        System.out.println("\n--- ẨN / HIỆN MÁY TRẠM ---");
        int id = InputUtils.inputInt("Nhập ID máy cần Ẩn/Hiện (số nguyên): ");
        try {
            Computer comp = computerService.getComputerById(id);
            if (comp == null) {
                PrintUtils.printWarning("Không tìm thấy máy với ID " + FormatUtils.formatId("C", id));
                return;
            }
            boolean isHidden = comp.getStatus() == com.cyber.model.enums.ComputerStatus.HIDDEN;
            String action = isHidden ? "HIỆN" : "ẨN";
            String confirmMsg = String.format("Bạn có chắc chắn muốn %s máy [%s] không? (Y/N): ", action, comp.getName());
            String confirm = InputUtils.inputString(confirmMsg, "^[YyNn]$", "Chỉ nhập Y hoặc N.");

            if (confirm.equalsIgnoreCase("Y")) {
                computerService.toggleComputerStatus(id, adminUser);
                PrintUtils.printSuccess("Đã " + action + " máy trạm [" + comp.getName() + "] thành công!");
            } else {
                System.out.println("Đã hủy thao tác.");
            }
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleDelete() {
        System.out.println("\n--- XÓA MÁY TRẠM (VĨNH VIỄN) ---");
        PrintUtils.printWarning("Lưu ý: Máy bị xóa sẽ không thể khôi phục. Lịch sử booking vẫn được giữ nguyên.");
        PrintUtils.printWarning("Lưu ý: Chỉ có thể xóa máy đang ở trạng thái ẨN (HIDDEN).");
        int id = InputUtils.inputInt("Nhập ID máy cần xóa (0 để hủy): ", 0, Integer.MAX_VALUE);
        if (id == 0) return;

        try {
            Computer comp = computerService.getComputerById(id);
            if (comp == null) {
                PrintUtils.printWarning("Không tìm thấy máy với ID " + FormatUtils.formatId("C", id));
                return;
            }
            System.out.println("Bạn sắp XÓA máy: " + comp.getName() + " (Khu vực: " + comp.getZone() + ")");
            String confirm = InputUtils.inputString("Xác nhận XÓA? (Nhập 'DELETE' để xác nhận): ");
            if (confirm.equals("DELETE")) {
                computerService.deleteComputer(id, adminUser);
                PrintUtils.printSuccess("Đã xóa máy trạm [" + comp.getName() + "] thành công!");
            } else {
                System.out.println("Đã hủy thao tác.");
            }
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }
}
