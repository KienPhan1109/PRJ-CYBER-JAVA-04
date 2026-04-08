package com.cyber.view;

import com.cyber.exception.BusinessException;
import com.cyber.model.FbOrder;
import com.cyber.model.User;
import com.cyber.service.FbOrderService;
import com.cyber.service.UserService;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

import java.math.BigDecimal;
import java.util.List;

public class StaffMainView {

    private final User staffUser;
    private final UserService userService;
    private final FbOrderService fbOrderService;

    public StaffMainView(User staffUser) {
        this.staffUser = staffUser;
        this.userService = UserService.getInstance();
        this.fbOrderService = FbOrderService.getInstance();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("          NHÂN VIÊN (STAFF) PANEL         ");
            System.out.println("          Xin chào: " + staffUser.getFullName());
            System.out.println("==========================================");
            System.out.println("1. Nạp tiền cho Khách Hàng");
            System.out.println("2. Quản lý Đơn hàng F&B (Mới nhất)");
            System.out.println("0. Đăng xuất");
            System.out.println("==========================================");

            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-2): ", 0, 2);

            try {
                switch (choice) {
                    case 1:
                        topUpUser();
                        break;
                    case 2:
                        manageFbOrders();
                        break;
                    case 0:
                        PrintUtils.printWarning("Đang đăng xuất khỏi hệ thống Staff...");
                        return;
                }
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
            } catch (Exception e) {
                PrintUtils.printError("Lỗi hệ thống: " + e.getMessage());
            }
        }
    }

    private void topUpUser() throws BusinessException {
        System.out.println("\n--- NẠP TIỀN KHÁCH HÀNG ---");
        int id = InputUtils.inputInt("Nhập ID người dùng cần nạp: ", 1, Integer.MAX_VALUE);
        BigDecimal amount = InputUtils.inputBigDecimal("Nhập số tiền muốn nạp (VND): ", BigDecimal.ONE);
        
        userService.topUpUser(id, amount);
        PrintUtils.printSuccess("Đã nạp " + FormatUtils.formatVND(amount) + " thành công cho User ID: " + id);
    }

    private void manageFbOrders() throws BusinessException {
        while (true) {
            List<FbOrder> pendingOrders = fbOrderService.getPendingOrders();
            System.out.println("\n--- DANH SÁCH ĐƠN HÀNG PENDING ---");
            if (pendingOrders.isEmpty()) {
                System.out.println("Chưa có đơn hàng nào cần xử lý.");
                return;
            }

            System.out.printf("%-10s | %-10s | %-12s | %-15s | %-15s\n", "Order ID", "User ID", "Booking ID", "Tổng Tiền", "Trạng thái");
            System.out.println("----------------------------------------------------------------------");
            for (FbOrder order : pendingOrders) {
                System.out.printf("%-10d | %-10d | %-12s | %-15s | %-15s\n",
                        order.getOrderId(),
                        order.getUserId(),
                        order.getBookingId() != null ? order.getBookingId().toString() : "N/A",
                        FormatUtils.formatVND(order.getTotalAmount()),
                        order.getStatus());
            }
            System.out.println("----------------------------------------------------------------------");
            System.out.println("Nhập Order ID để cập nhật trạng thái (HOẶC nhập 0 để Quay Lại):");
            int orderId = InputUtils.inputInt("Order ID: ", 0, Integer.MAX_VALUE);
            if (orderId == 0) return;

            FbOrder target = pendingOrders.stream().filter(o -> o.getOrderId() == orderId).findFirst().orElse(null);
            if (target == null) {
                PrintUtils.printError("Order ID không hợp lệ trong danh sách Pending.");
                continue;
            }

            System.out.println("Cập nhật trạng thái cho Order #" + orderId + ":");
            System.out.println("1. Xác nhận đang làm (PREPARING)");
            System.out.println("2. Đã giao xong (DELIVERED)");
            System.out.println("3. Hủy bỏ (CANCELLED)");
            int action = InputUtils.inputInt("Chọn thao tác (1-3): ", 1, 3);
            
            String newStatus = action == 1 ? "PREPARING" : (action == 2 ? "DELIVERED" : "CANCELLED");
            fbOrderService.updateOrderStatus(orderId, newStatus);
            PrintUtils.printSuccess("Đã cập nhật Order #" + orderId + " sang trạng thái: " + newStatus);
        }
    }
}
