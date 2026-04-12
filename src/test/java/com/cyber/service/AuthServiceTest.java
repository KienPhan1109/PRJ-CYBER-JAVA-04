package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.exception.BusinessException;
import com.cyber.model.User;
import com.cyber.model.enums.UserStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private static final AuthService authService = AuthService.getInstance();
    private static String TEST_USER_1;
    private static String TEST_USER_2;
    private static String TEST_USER_LOCKED;
    private static final String PWD = "SecurePassword123!";

    @BeforeAll
    public static void setup() {
        TEST_USER_1 = "auth1_" + UUID.randomUUID().toString().substring(0, 6);
        TEST_USER_2 = "auth2_" + UUID.randomUUID().toString().substring(0, 6);
        TEST_USER_LOCKED = "locked_" + UUID.randomUUID().toString().substring(0, 6);

        // Đăng ký account chuẩn bị cho các bài Test Đăng nhập (Login)
        try {
            User u1 = new User();
            u1.setUsername(TEST_USER_1);
            u1.setFullName("Normal Guy");
            u1.setRole(new com.cyber.model.Role(1, "CUSTOMER"));
            authService.register(u1, PWD);

            User uLocked = new User();
            uLocked.setUsername(TEST_USER_LOCKED);
            uLocked.setFullName("Locked Guy");
            uLocked.setRole(new com.cyber.model.Role(1, "CUSTOMER"));
            authService.register(uLocked, "Locked123!");
            
            // Tìm UserId và Khoá (Lock) nó.
            try (Connection conn = DatabaseConnection.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE username=?")) {
                ps.setString(1, TEST_USER_LOCKED);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("user_id");
                        UserService.getInstance().updateUserStatus(id, UserStatus.LOCKED, uLocked);
                    }
                }
            }
        } catch (Exception e) {}
    }

    @AfterAll
    public static void teardown() {
        cleanTestData();
    }

    private static void cleanTestData() {
        // Dọn dẹp Database sau khi test xong để không để lại rác
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username IN (?, ?, ?)")) {
            ps.setString(1, TEST_USER_1);
            ps.setString(2, TEST_USER_2);
            ps.setString(3, TEST_USER_LOCKED);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    @Test
    @DisplayName("Đăng ký thành công tài khoản mới hợp lệ")
    public void testRegisterSuccess() {
        String uniqueName = "new_" + UUID.randomUUID().toString().substring(0, 8);
        User newUser = new User();
        newUser.setUsername(uniqueName);
        newUser.setFullName("Test User 1");
        newUser.setPhone("0901234567");
        newUser.setRole(new com.cyber.model.Role(1, "CUSTOMER"));

        assertDoesNotThrow(() -> authService.register(newUser, PWD), "Không được throw lỗi khi đăng ký hợp lệ");
        
        // Clean up sau khi tạo thành công để không rác DB
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
            ps.setString(1, uniqueName);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    @Test
    @DisplayName("Kiểm tra báo lỗi khi cố đăng ký username đã tồn tại")
    public void testRegisterDuplicateUsername() {
        User newUser1 = new User();
        newUser1.setUsername(TEST_USER_2);
        newUser1.setFullName("Test User 2");
        newUser1.setRole(new com.cyber.model.Role(1, "CUSTOMER"));

        // Đăng ký lần 1
        assertDoesNotThrow(() -> authService.register(newUser1, PWD));

        // Đăng ký lần 2 trùng Username
        User newUser2 = new User();
        newUser2.setUsername(TEST_USER_2);
        newUser2.setFullName("Chôm Tên");
        newUser2.setRole(new com.cyber.model.Role(1, "CUSTOMER"));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(newUser2, PWD));
        assertEquals("REGISTER_FAILED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("đã tồn tại"));
    }

    @Test
    @DisplayName("Đăng nhập thành công với password")
    public void testLoginSuccess() throws BusinessException {
        // User này đã được Test chuẩn bị sẵn trong @BeforeAll
        User loggedInUser = assertDoesNotThrow(() -> authService.login(TEST_USER_1, PWD));
        assertNotNull(loggedInUser);
        assertEquals(TEST_USER_1, loggedInUser.getUsername());
    }

    @Test
    @DisplayName("Đăng nhập thất bại do sai mật khẩu")
    public void testLoginFailure() {
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(TEST_USER_1, "WrongPassword!"));
        assertEquals("AUTH_FAILED", exception.getErrorCode());
    }

    @Test
    @DisplayName("Ngăn chặn đăng nhập vào tài khoản đang bị khóa (LOCKED)")
    public void testLoginLockedUser() {
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(TEST_USER_LOCKED, "Locked123!"));
        assertEquals("ACCOUNT_LOCKED", exception.getErrorCode());
    }
}
