package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.exception.BusinessException;
import com.cyber.model.Computer;
import com.cyber.model.User;
import com.cyber.model.enums.ComputerStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ComputerServiceTest {

    private static final ComputerService computerService = ComputerService.getInstance();
    private static final String TEST_PC_NAME = "TEST_PC_999";
    private static User dummyAdmin;
    private static int targetComputerId = -1;

    @BeforeAll
    public static void setup() {
        cleanTestData();
        dummyAdmin = new User();
        dummyAdmin.setUserId(9999);
        dummyAdmin.setUsername("admin_bot");
        dummyAdmin.setRole(new com.cyber.model.Role(2, "ADMIN")); // admin

        try {
            Computer c = new Computer();
            c.setName(TEST_PC_NAME);
            c.setZone(com.cyber.model.enums.ComputerZone.VIP);
            c.setHardwareConfig("i9 14900k, RTX 4090");
            c.setPricePerHour(new BigDecimal("25000"));
            c.setStatus(ComputerStatus.AVAILABLE);

            computerService.addComputer(c, dummyAdmin);
            
            // Tìm ID của PC vừa thêm
            List<Computer> list = computerService.getAllComputers();
            for (Computer pc : list) {
                if (pc.getName().equals(TEST_PC_NAME)) {
                    targetComputerId = pc.getComputerId();
                    break;
                }
            }
        } catch (BusinessException ignored) {}
    }

    @AfterAll
    public static void teardown() {
        cleanTestData();
    }

    private static void cleanTestData() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM computers WHERE name LIKE 'TEST_PC_%'")) {
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    @Test
    @DisplayName("Kiểm tra báo lỗi chặn triệt để tên máy bị trùng lặp")
    public void testComputerNameUniqueValidation() {
        Computer duplicatePC = new Computer();
        duplicatePC.setName(TEST_PC_NAME); // Tên đã bị lấy trên Setup
        duplicatePC.setPricePerHour(new BigDecimal("10000"));

        BusinessException ex = assertThrows(BusinessException.class, 
            () -> computerService.addComputer(duplicatePC, dummyAdmin));
        
        assertEquals("DUPLICATE_NAME", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("đã tồn tại") || ex.getMessage().contains("Tên máy"));
    }

    @Test
    @DisplayName("Đối chiếu query Customer chỉ thấy máy không bị ẩn")
    public void testCustomerCannotSeeHiddenComputers() throws BusinessException {
        // Cho ẩn máy TEST đi
        computerService.toggleComputerStatus(targetComputerId, dummyAdmin);

        // Giả lập Customer xem toàn bộ máy qua service
        // Do ComputerService hiện tại chưa có filter List trả về ngoại trừ trong lấy máy theo Khu vực 
        // Nhưng Customer xem máy trống qua getAvailableComputersByZone
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        List<Computer> availableVIPs = computerService.getAvailableComputersByZone(com.cyber.model.enums.ComputerZone.VIP, now, now);

        boolean found = availableVIPs.stream().anyMatch(c -> c.getComputerId() == targetComputerId);
        assertFalse(found, "Vì máy đang bị HIDDEN, Customer sẽ KHÔNG bao giờ nhìn thấy máy này ở danh sách available!");

        // Khôi phục
        computerService.toggleComputerStatus(targetComputerId, dummyAdmin);
    }
}
