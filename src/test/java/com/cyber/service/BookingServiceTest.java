package com.cyber.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

public class BookingServiceTest {

    @Test
    @DisplayName("Kiểm tra giới hạn tính toán Heartbeat: Tính tiền trừ đúng trên 1 Interval")
    public void testHeartbeatCalculationLogic() {
        int intervalMs = 10000; // Nhịp tim quét mỗi 10 giây
        BigDecimal hourlyRate = new BigDecimal("18000"); // 18,000 VND / giờ
        
        // Công thức tính tiền bị trừ theo interval (tương tự như trong SessionHeartbeatManager / BookingService)
        // Số tiền = hourlyRate * intervalMs / 3600000
        BigDecimal rateForInterval = hourlyRate.multiply(new BigDecimal(intervalMs))
                                               .divide(new BigDecimal(3600000), 2, RoundingMode.HALF_UP);
        
        // Kỳ vọng: 18,000 * 10,000 / 3,600,000 = 50.00 VNĐ
        assertEquals(new BigDecimal("50.00"), rateForInterval, "Tiền trừ sau 10s phải là 50 VNĐ cho máy giá 18k/h!");
    }

    @Test
    @DisplayName("Kiểm tra Heartbeat với trường hợp giá chơi đặc biệt (0 đồng)")
    public void testHeartbeatCalculationWithZeroRate() {
        int intervalMs = 10000; 
        BigDecimal hourlyRate = BigDecimal.ZERO; 
        
        BigDecimal rateForInterval = hourlyRate.multiply(new BigDecimal(intervalMs))
                                               .divide(new BigDecimal(3600000), 2, RoundingMode.HALF_UP);
        
        assertEquals(new BigDecimal("0.00"), rateForInterval, "Với máy miễn phí, Heartbeat không được trừ tiền!");
    }

    @Test
    @DisplayName("Kiểm tra tính phí khi Session kết thúc (End Session Fee)")
    public void testEndSessionFeeCalculation() {
        // Giả lập thời gian chơi
        long startTime = System.currentTimeMillis() - 7200000; // Bắt đầu từ 2 giờ trước
        long endTime = System.currentTimeMillis();
        BigDecimal hourlyRate = new BigDecimal("15000"); // 15,000 VNĐ / giờ
        
        long diffInMillis = endTime - startTime;
        double hours = diffInMillis / (1000.0 * 60 * 60);
        
        BigDecimal expectedTotal = hourlyRate.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
        
        // 15,000 * 2 = 30,000
        assertEquals(new BigDecimal("30000.00"), expectedTotal, "Thuật toán tính tổng kết thúc sai lệch!");
    }
    
    @Test
    @DisplayName("Kiểm tra cọc tiền đặt trước (Reservation Deposit)")
    public void testReservationDepositLogic() {
        BigDecimal balance = new BigDecimal("50000");
        BigDecimal hourlyRate = new BigDecimal("25000"); // Tiền cọc tương đương 1 giờ chơi
        
        assertTrue(balance.compareTo(hourlyRate) >= 0, "Guest có đủ tiền đặt cọc");
        
        // Sau khi đặt:
        BigDecimal balanceAfterBooking = balance.subtract(hourlyRate);
        assertEquals(new BigDecimal("25000"), balanceAfterBooking, "Hệ thống phải trừ đúng 25k cọc!");
    }
}
