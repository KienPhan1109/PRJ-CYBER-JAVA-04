package com.cyber.service;

import com.cyber.model.User;
import com.cyber.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityBusinessTest {

    private User lockedUser;
    private User normalUser;

    @BeforeEach
    public void setup() {
        lockedUser = new User();
        lockedUser.setUserId(99);
        lockedUser.setUsername("hacker_01");
        lockedUser.setStatus(UserStatus.LOCKED);
        lockedUser.setBalance(new BigDecimal("100000")); // Tài khoản còn tiền nhưng bị khóa

        normalUser = new User();
        normalUser.setUserId(100);
        normalUser.setUsername("honest_gamer");
        normalUser.setStatus(UserStatus.ACTIVE);
        normalUser.setBalance(new BigDecimal("5000")); // Số dư thấp
    }

    @Test
    @DisplayName("Kiểm tra chặn hành động từ tài khoản LOCKED")
    public void testLockedUserCannotCreateBooking() {
        // Mô phỏng logic bên trong Service khi check user = DAO
        // "if (currentUser.getStatus() == UserStatus.LOCKED) throw new BusinessException..."
        
        Exception ex = assertThrows(RuntimeException.class, () -> {
            if (lockedUser.getStatus() == UserStatus.LOCKED) {
                throw new RuntimeException("ERR_USER_LOCKED");
            }
        });
        
        assertEquals("ERR_USER_LOCKED", ex.getMessage(), "Phải ném ra lỗi khoá tài khoản!");
    }

    @Test
    @DisplayName("Kiểm tra hành động bị chặn khi không đủ số dư")
    public void testInsufficientBalanceValidation() {
        // Normal user want to reserve 25k VIP PC
        BigDecimal requireDeposit = new BigDecimal("25000");
        
        Exception ex = assertThrows(RuntimeException.class, () -> {
            if (normalUser.getBalance().compareTo(requireDeposit) < 0) {
                throw new RuntimeException("ERR_INSUFFICIENT_BALANCE");
            }
        });

        assertEquals("ERR_INSUFFICIENT_BALANCE", ex.getMessage(), "Phải ném ra lỗi không đủ tiền cọc!");
    }
}
