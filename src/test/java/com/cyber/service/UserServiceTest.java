package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.exception.BusinessException;
import com.cyber.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private static final UserService userService = UserService.getInstance();
    private static final AuthService authService = AuthService.getInstance();
    
    private static final String TEST_USERNAME = "test_money_guy";
    private static final String ADMIN_TESTER  = "admin_auditor";
    
    private static int targetUserId = -1;
    private static User adminActor;

    @BeforeAll
    public static void setup() {
        cleanTestData();
        try {
            // Khởi tạo một User để bơm/trừ tiền
            User u = new User();
            u.setUsername(TEST_USERNAME);
            u.setFullName("Test Money");
            u.setRole(com.cyber.model.enums.UserRole.CUSTOMER);
            authService.register(u, "Admin@123");
            
            // Khởi tạo Admin Actor để làm Log Record
            adminActor = new User();
            adminActor.setUsername(ADMIN_TESTER);
            adminActor.setFullName("Admin Tester");
            adminActor.setRole(com.cyber.model.enums.UserRole.ADMIN); // Giả lập Admin / Staff
            adminActor.setUserId(9999);
            
            // Tìm id của test_money_guy vì register không trả về ID
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE username = ?")) {
                ps.setString(1, TEST_USERNAME);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        targetUserId = rs.getInt("user_id");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void teardown() {
        cleanTestData();
    }

    private static void cleanTestData() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM system_logs WHERE actor_id = ? OR action LIKE '%test_money_guy%'");
             PreparedStatement ps2 = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
            ps.setInt(1, 9999);
            ps2.setString(1, TEST_USERNAME);
            ps2.executeUpdate();
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    @Test
    @DisplayName("Nạp tiền 50k vào tài khoản thành công")
    public void testTopUpUserSuccess() throws BusinessException {
        assertTrue(targetUserId > 0, "Không lấy được ID tài khoản test");

        BigDecimal topUpAmount = new BigDecimal("50000");
        assertDoesNotThrow(() -> userService.topUpUser(targetUserId, topUpAmount, adminActor));

        // Kiểm chứng số dư bằng việc lấy lại từ DB
        User updatedUser = userService.getUserById(targetUserId);
        assertEquals(0, topUpAmount.compareTo(updatedUser.getBalance()), "Số dư phải khớp với số tiền vừa nạp!");
    }

    @Test
    @DisplayName("Chặn tuyệt đối nếu cố tình nạp số tiền âm (Hack hệ thống)")
    public void testTopUpUserInvalidAmount() {
        BigDecimal invalidAmount = new BigDecimal("-1000");

        BusinessException ex = assertThrows(BusinessException.class, 
            () -> userService.topUpUser(targetUserId, invalidAmount, adminActor));
            
        assertEquals("INVALID_AMOUNT", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("lớn hơn 0"));
    }

    @Test
    @DisplayName("Tính phí (trừ tiền) chuẩn xác mà không chạm vạch âm")
    public void testDeductMoneySuccess() {
        // Nạp thả ga 100k
        assertDoesNotThrow(() -> userService.topUpUser(targetUserId, new BigDecimal("100000"), adminActor));
        
        // Trừ 20k
        assertDoesNotThrow(() -> userService.deductMoney(targetUserId, 20000.0, adminActor.getUserId(), "Trừ tiền mua mỳ tôm"));
    }

    @Test
    @DisplayName("Chặn Staff thực hiện thao tác trả tiền nếu số dư User không đủ")
    public void testDeductMoneyInsufficientFunds() {
        // Cố tình trừ 99 triệu khi trong khoản không đủ
        BusinessException ex = assertThrows(BusinessException.class, 
            () -> userService.deductMoney(targetUserId, 99000000.0, adminActor.getUserId(), "Trừ phí dịch vụ quá tay"));
            
        assertEquals("INSUFFICIENT_FUNDS", ex.getErrorCode());
    }

    @Test
    @DisplayName("Từ chối xoá (Hard-delete/Soft-delete) tài khoản nếu còn tiền")
    public void testDeleteUserFailsIfBalanceExists() {
        // Tài khoản test_money_guy đang có tiền nhờ những test bên trên
        // Thử gọi deleteUser
        BusinessException ex = assertThrows(BusinessException.class, 
            () -> userService.deleteUser(targetUserId));
            
        assertEquals("BALANCE_EXISTS", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("rút tiền về 0đ"));
    }
}
