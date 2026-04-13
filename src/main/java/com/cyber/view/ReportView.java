package com.cyber.view;

import com.cyber.exception.BusinessException;
import com.cyber.service.ReportService;
import com.cyber.util.FormatUtils;
import com.cyber.util.InputUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class ReportView {
    private final ReportService reportService;

    public ReportView() {
        this.reportService = ReportService.getInstance();
    }

    public void displayAdminReportMenu() {
        while (true) {
            System.out.println("\n--- BÁO CÁO THỐNG KÊ (ADMIN) ---");
            System.out.println("1. Báo cáo doanh thu hôm nay");
            System.out.println("2. Báo cáo doanh thu tháng này");
            System.out.println("3. Báo cáo doanh thu tùy chỉnh");
            System.out.println("0. Quay Lại");

            int choice = InputUtils.inputInt("Chọn chức năng (0-3): ", 0, 3);
            
            try {
                switch (choice) {
                    case 1:
                        showReport(LocalDate.now().atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX), null);
                        break;
                    case 2:
                        showReport(LocalDate.now().withDayOfMonth(1).atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX), null);
                        break;
                    case 3:
                        String startStr = InputUtils.inputString("Từ ngày (yyyy-MM-dd): ",
                                "^\\d{4}-\\d{2}-\\d{2}$", "Định dạng không hợp lệ. VD: 2026-04-01");
                        String endStr = InputUtils.inputString("Đến ngày (yyyy-MM-dd): ",
                                "^\\d{4}-\\d{2}-\\d{2}$", "Định dạng không hợp lệ. VD: 2026-04-30");
                        LocalDate start = LocalDate.parse(startStr);
                        LocalDate end = LocalDate.parse(endStr);
                        if (start.isAfter(end)) {
                            System.out.println("Lỗi: Ngày bắt đầu không thể sau ngày kết thúc.");
                        } else {
                            showReport(start.atStartOfDay(), end.atTime(LocalTime.MAX), null);
                        }
                        break;
                    case 0:
                        return;
                }
            } catch (Exception e) {
                System.out.println("Có lỗi khi tạo báo cáo: " + e.getMessage());
            }
        }
    }

    public void displayStaffReportMenu(int staffId) {
        System.out.println("\n--- BÁO CÁO DOANH THU CÁ NHÂN (CA HÔM NAY) ---");
        try {
            showReport(LocalDate.now().atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX), staffId);
        } catch (Exception e) {
            System.out.println("Có lỗi khi tạo báo cáo: " + e.getMessage());
        }
    }

    private void showReport(LocalDateTime start, LocalDateTime end, Integer staffId) throws BusinessException {
        Timestamp startTs = Timestamp.valueOf(start);
        Timestamp endTs = Timestamp.valueOf(end);

        BigDecimal bookingRev = reportService.getTotalBookingRevenue(startTs, endTs, staffId);
        BigDecimal fbRev = reportService.getTotalFbRevenue(startTs, endTs, staffId);
        BigDecimal totalRev = bookingRev.add(fbRev);

        System.out.println("\n==========================================");
        System.out.println("   TỔNG QUAN DOANH THU (" + FormatUtils.formatTimestamp(startTs) + " - " + FormatUtils.formatTimestamp(endTs) + ")");
        System.out.println("==========================================");
        System.out.printf("Doanh thu Máy trạm : %s%n", FormatUtils.formatVND(bookingRev));
        System.out.printf("Doanh thu F&B      : %s%n", FormatUtils.formatVND(fbRev));
        System.out.println("------------------------------------------");
        System.out.printf("TỔNG DOANH THU     : %s%n", FormatUtils.formatVND(totalRev));
        System.out.println("==========================================\n");

        List<Map<String, Object>> hotItems = reportService.getTopSellingItems(startTs, endTs, 5, staffId);
        System.out.println("--- TOP 5 MÓN F&B BÁN CHẠY NHẤT ---");
        if (hotItems.isEmpty()) {
            System.out.println("Chưa có dữ liệu.");
        } else {
            System.out.printf("%-30s | %-10s%n", "Tên Món", "Số Lượng");
            System.out.println("-".repeat(43));
            for (Map<String, Object> item : hotItems) {
                System.out.printf("%-30s | %-10d%n", FormatUtils.truncate((String) item.get("itemName"), 30), (Integer) item.get("totalQty"));
            }
        }
        
        System.out.println("\n--- THỐNG KÊ SỬ DỤNG MÁY ---");
        List<Map<String, Object>> hotMachines = reportService.getMachineUsageStats(startTs, endTs, staffId);
        if (hotMachines.isEmpty()) {
            System.out.println("Chưa có dữ liệu.");
        } else {
            System.out.printf("%-15s | %-15s%n", "Tên Máy", "Số lượt thuê");
            System.out.println("-".repeat(33));
            for (Map<String, Object> machine : hotMachines) {
                 System.out.printf("%-15s | %-15d%n", machine.get("computerName"), (Integer) machine.get("totalBookings"));
            }
        }
        System.out.println();
    }
}
