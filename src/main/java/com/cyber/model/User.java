package com.cyber.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import com.cyber.model.enums.UserStatus;
import com.cyber.util.InputUtils;
import com.cyber.util.PrintUtils;

public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private Role role;
    private BigDecimal balance;
    private String fullName;
    private String phone;
    private UserStatus status;
    private Timestamp createdAt;
    private boolean isDeleted;

    public User() {}

    public String inputRegisterData() {
        this.username = InputUtils.inputString("Tên đăng nhập: ");
        String password;
        while (true) {
            password = InputUtils.inputRegisterPassword("Mật khẩu (>= 8 ký tự, 1 Hoa, 1 thường, 1 số, 1 ký tự đặc biệt): ");
            String confirmPassword = InputUtils.inputPassword("Xác nhận mật khẩu: ");
            if (!password.equals(confirmPassword)) {
                PrintUtils.printError("Mật khẩu xác nhận không khớp. Vui lòng nhập lại!");
            } else {
                break;
            }
        }
        this.fullName = InputUtils.inputString("Họ và tên: ");
        this.phone = InputUtils.inputString("Số điện thoại: ", "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", "Số điện thoại không đúng định dạng VN (VD: 09xxxxxxxx hoặc +849xxxxxxxx).");
        return password;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
}