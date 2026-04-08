package com.cyber.view;

import com.cyber.exception.BusinessException;
import com.cyber.model.*;
import com.cyber.model.enums.ComputerZone;
import com.cyber.service.BookingService;
import com.cyber.service.ComputerService;
import com.cyber.service.FbOrderService;
import com.cyber.service.ServiceItemService;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CustomerMainView {

    private final User customerUser;
    private final BookingService bookingService;
    private final ComputerService computerService;
    private final ServiceItemService itemService;
    private final FbOrderService orderService;

    // Optional: Keep track of active booking for this session if they booked just now
    private Integer currentBookingId = null; 

    public CustomerMainView(User customerUser) {
        this.customerUser = customerUser;
        this.bookingService = BookingService.getInstance();
        this.computerService = ComputerService.getInstance();
        this.itemService = ServiceItemService.getInstance();
        this.orderService = FbOrderService.getInstance();
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("         KHÁCH HÀNG (CUSTOMER) PANEL      ");
            System.out.println("         Xin chào: " + customerUser.getFullName());
            System.out.println("         Số dư khả dụng: " + FormatUtils.formatVND(customerUser.getBalance()));
            System.out.println("==========================================");
            System.out.println("1. Đặt máy trạm (Booking)");
            System.out.println("2. Đặt đồ ăn / Thức uống (F&B)");
            System.out.println("0. Đăng xuất");
            System.out.println("==========================================");

            int choice = InputUtils.inputInt("Vui lòng chọn chức năng (0-2): ", 0, 2);

            try {
                switch (choice) {
                    case 1:
                        bookComputerFlow();
                        break;
                    case 2:
                        orderFoodFlow();
                        break;
                    case 0:
                        PrintUtils.printWarning("Đang đăng xuất khỏi hệ thống Khách hàng...");
                        return;
                }
            } catch (BusinessException e) {
                PrintUtils.printError(e.getMessage());
            } catch (Exception e) {
                PrintUtils.printError("Lỗi hệ thống: " + e.getMessage());
            }
        }
    }

    private void bookComputerFlow() throws BusinessException {
        System.out.println("\n--- ĐẶT MÁY TRẠM ---");
        System.out.println("Lưu ý: Tiền cọc sẽ được trừ ngay lập tức vào số dư.");
        int hours = InputUtils.inputInt("Bạn muốn đặt máy trong mấy giờ? (Tối thiểu 1h): ", 1, 24);

        System.out.println("Chọn khu vực:");
        System.out.println("1. VIP | 2. STANDARD | 3. ESPORT | 4. STREAMING | 5. COUPLE | 6. BẤT KỲ");
        int zoneChoice = InputUtils.inputInt("Chọn (1-6): ", 1, 6);
        
        ComputerZone zone = null;
        switch (zoneChoice) {
            case 1: zone = ComputerZone.VIP; break;
            case 2: zone = ComputerZone.STANDARD; break;
            case 3: zone = ComputerZone.ESPORT; break;
            case 4: zone = ComputerZone.STREAMING; break;
            case 5: zone = ComputerZone.COUPLE; break;
        }

        Timestamp start = new Timestamp(System.currentTimeMillis());
        Timestamp end = new Timestamp(System.currentTimeMillis() + (hours * 3600000L));
        List<Computer> availableComputers = computerService.getAvailableComputersByZone(zone, start, end);
        if (availableComputers.isEmpty()) {
            PrintUtils.printWarning("Rất tiếc! Hiện tại không có máy Trống nào phù hợp ở khu vực bạn chọn.");
            return;
        }

        System.out.println("\nDANH SÁCH MÁY TRỐNG:");
        System.out.printf("%-5s | %-15s | %-15s | %-30s | %-15s\n", "ID", "Tên Máy", "Khu Vực", "Cấu hình", "Đơn giá/h");
        System.out.println("---------------------------------------------------------------------------------------");
        for (Computer c : availableComputers) {
            System.out.printf("%-5d | %-15s | %-15s | %-30s | %-15s\n", 
                c.getComputerId(), c.getName(), c.getZone(), c.getHardwareConfig(), 
                FormatUtils.formatVND(c.getPricePerHour()));
        }
        System.out.println("---------------------------------------------------------------------------------------");

        int computerId = InputUtils.inputInt("Vui lòng nhập ID máy muốn đặt (HOẶC nhập 0 để hủy): ", 0, Integer.MAX_VALUE);
        if (computerId == 0) {
            System.out.println("Đã hủy thao tác đặt máy.");
            return;
        }

        Computer targetComputer = availableComputers.stream().filter(c -> c.getComputerId() == computerId).findFirst().orElse(null);
        if (targetComputer == null) {
            PrintUtils.printError("ID máy không tồn tại trong danh sách trống.");
            return;
        }

        BigDecimal fee = targetComputer.getPricePerHour().multiply(new BigDecimal(hours));
        System.out.println("------------------------------");
        System.out.println("DỰ TOÁN THUÊ MÁY:");
        System.out.println("Máy: " + targetComputer.getName());
        System.out.println("Thời gian: " + hours + " giờ");
        System.out.println("Tổng chi phí: " + FormatUtils.formatVND(fee));
        System.out.println("Số dư của bạn: " + FormatUtils.formatVND(customerUser.getBalance()));
        System.out.println("------------------------------");

        String confirm = InputUtils.inputString("Xác nhận Đặt và Trừ Tiền? (Y/N): ");
        if (confirm.equalsIgnoreCase("y")) {
            Booking newBooking = new Booking(0, customerUser.getUserId(), targetComputer.getComputerId(), start, end, "IN_PROGRESS", fee);
            bookingService.bookComputer(customerUser.getUserId(), newBooking);
            
            this.customerUser.setBalance(this.customerUser.getBalance().subtract(fee)); // Update local ref
            
            PrintUtils.printSuccess("Đặt máy thành công! Bạn có thể bắt đầu sử dụng máy " + targetComputer.getName());
        } else {
            System.out.println("Đã hủy đặt máy.");
        }
    }

    private void orderFoodFlow() throws BusinessException {
        System.out.println("\n--- MENU F&B ---");
        List<ServiceItem> items = itemService.getAllServiceItems();
        
        System.out.printf("%-5s | %-25s | %-15s | %-10s\n", "ID", "Tên Món", "Giá Tiền", "Tồn kho");
        System.out.println("------------------------------------------------------------------");
        for (ServiceItem item : items) {
            String statusText = "";
            if (item.getStatus() != com.cyber.model.enums.ServiceItemStatus.ACTIVE || item.getStockQuantity() == 0) {
                statusText = "\033[31m(Hết hàng)\033[0m";
            }
            System.out.printf("%-5d | %-25s | %-15s | %-10s %s\n", 
                item.getItemId(), item.getName(), FormatUtils.formatVND(item.getPrice()), item.getStockQuantity(), statusText);
        }
        System.out.println("------------------------------------------------------------------");

        List<OrderDetail> cart = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;

        while (true) {
            int itemId = InputUtils.inputInt("Nhập ID món muốn gọi (HOẶC 0 để Chốt Đơn/Thoát): ", 0, Integer.MAX_VALUE);
            if (itemId == 0) break;

            ServiceItem selectedItem = items.stream().filter(i -> i.getItemId() == itemId).findFirst().orElse(null);
            if (selectedItem == null) {
                PrintUtils.printError("Món không tồn tại.");
                continue;
            }
            if (selectedItem.getStatus() != com.cyber.model.enums.ServiceItemStatus.ACTIVE || selectedItem.getStockQuantity() == 0) {
                PrintUtils.printError("Món này đã hết hàng, vui lòng chọn món khác.");
                continue;
            }

            int qty = InputUtils.inputInt("Nhập số lượng: ", 1, selectedItem.getStockQuantity());
            
            // Note: Since terminal is independent of session logic here, we just sum it up
            OrderDetail detail = new OrderDetail(0, selectedItem.getItemId(), qty, selectedItem.getPrice());
            cart.add(detail);
            totalCost = totalCost.add(selectedItem.getPrice().multiply(new BigDecimal(qty)));
            System.out.println("Đã thêm " + qty + "x " + selectedItem.getName() + " vào giỏ.");
        }

        if (cart.isEmpty()) {
            System.out.println("Giỏ hàng của bạn đang trống.");
            return;
        }

        System.out.println("\n---- HOÁ ĐƠN DỰ KIẾN ----");
        for (OrderDetail od : cart) {
            String tempName = items.stream().filter(i -> i.getItemId() == od.getItemId()).findFirst().get().getName();
            System.out.printf("%-20s x%-3d = %s\n", tempName, od.getQuantity(), FormatUtils.formatVND(od.getUnitPrice().multiply(new BigDecimal(od.getQuantity()))));
        }
        System.out.println("TỔNG TIỀN F&B: " + FormatUtils.formatVND(totalCost));
        System.out.println("Số dư ví của bạn: " + FormatUtils.formatVND(customerUser.getBalance()));

        String payConfirm = InputUtils.inputString("Xác nhận thanh toán và Đặt món? (Y/N): ");
        if (payConfirm.equalsIgnoreCase("y")) {
            FbOrder newOrder = new FbOrder(customerUser.getUserId(), this.currentBookingId, "PENDING", totalCost);
            orderService.orderFoodIndependently(customerUser.getUserId(), this.currentBookingId, newOrder, cart);
            
            this.customerUser.setBalance(this.customerUser.getBalance().subtract(totalCost));
            PrintUtils.printSuccess("Đặt đồ ăn thành công! Lệnh đang chờ xử lý.");
        } else {
            System.out.println("Đã hủy đơn F&B.");
        }
    }
}
