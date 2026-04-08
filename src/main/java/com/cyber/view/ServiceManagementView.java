package com.cyber.view;

import com.cyber.util.PrintUtils;

import com.cyber.exception.BusinessException;
import com.cyber.model.ServiceItem;
import com.cyber.service.ServiceItemService;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;

import java.math.BigDecimal;
import java.util.List;

public class ServiceManagementView {

    private final ServiceItemService serviceService;

    public ServiceManagementView() {
        this.serviceService = ServiceItemService.getInstance();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n--- QUẢN LÝ DỊCH VỤ F&B ---");
            System.out.println("1. Xem danh mục F&B");
            System.out.println("2. Thêm món mới");
            System.out.println("3. Sửa thông tin món");
            System.out.println("4. Xoá món");
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
            List<ServiceItem> items = serviceService.getAllServiceItems();
            System.out.println("\n============================================================================================================================");
            System.out.printf("%-10s | %-25s | %-35s | %-15s | %-10s | %-15s%n", "ID Món", "Tên Món", "Mô tả", "Giá", "Tồn Kho", "Trạng thái");
            System.out.println("----------------------------------------------------------------------------------------------------------------------------");
            
            if (items.isEmpty()) {
                System.out.println("Chưa có danh mục nào.");
            } else {
                for (ServiceItem item : items) {
                    System.out.printf("%-10s | %-25s | %-35s | %-15s | %-10s | %-15s%n",
                            FormatUtils.formatId("S", item.getItemId()),
                            item.getName(),
                            item.getDescription(),
                            FormatUtils.formatVND(item.getPrice()),
                            item.getStockQuantity(),
                            FormatUtils.formatServiceItemStatus(item.getStatus()));
                }
            }
            System.out.println("============================================================================================================================");
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleAdd() {
        System.out.println("\n--- THÊM MÓN MỚI ---");
        ServiceItem newItem = new ServiceItem();
        newItem.inputData(false);
        
        try {
            serviceService.addServiceItem(newItem);
            System.out.println("\033[32m[THÀNH CÔNG] Thêm món F&B thành công!\033[0m");
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleEdit() {
        System.out.println("\n--- SỬA MÓN F&B ---");
        int id = InputUtils.inputInt("Nhập ID món cần sửa (số nguyên): ");
        try {
            ServiceItem existing = serviceService.getServiceItemById(id);
            if (existing == null) {
                PrintUtils.printWarning("Không tìm thấy món F&B nào với ID " + FormatUtils.formatId("S", id) + "");
                return;
            }
            
            System.out.println("Đang chỉnh sửa: " + existing.getName());
            
            existing.inputData(true);
            
            serviceService.updateServiceItem(existing);
            System.out.println("\033[32m[THÀNH CÔNG] Cập nhật món thành công!\033[0m");
            
        } catch (BusinessException e) {
            PrintUtils.printError(e.getMessage());
        }
    }

    private void handleDelete() {
        System.out.println("\n--- XÓA MÓN F&B ---");
        int id = InputUtils.inputInt("Nhập ID món cần xóa (số nguyên): ");
        String confirm = InputUtils.inputString("Bạn có chắc chắn muốn xóa không? (Y/N): ", "^[YyNn]$", "Chỉ nhập Y hoặc N.");
        
        if (confirm.equalsIgnoreCase("Y")) {
            try {
                serviceService.deleteServiceItem(id);
                System.out.println("\033[32m[THÀNH CÔNG] Xóa món thành công!\033[0m");
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
            }
        } else {
            System.out.println("Đã hủy thao tác xóa.");
        }
    }
}
