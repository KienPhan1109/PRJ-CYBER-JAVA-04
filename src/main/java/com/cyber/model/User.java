package com.cyber.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private Role role;
    private BigDecimal balance;
    private String fullName;
    private String phone;
    private Timestamp createdAt;

    public User() {}

    public String inputRegisterData() {
        this.username = com.cyber.util.InputUtils.inputString("Tên đăng nhập: ");
        String password;
        while (true) {
            password = com.cyber.util.InputUtils.inputPassword("Mật khẩu (tối thiểu 6 ký tự): ");
            String confirmPassword = com.cyber.util.InputUtils.inputPassword("Xác nhận mật khẩu: ");
            if (!password.equals(confirmPassword)) {
                System.out.println("\033[31m[LỖI] Mật khẩu xác nhận không khớp. Vui lòng nhập lại!\033[0m\n");
            } else {
                break;
            }
        }
        this.fullName = com.cyber.util.InputUtils.inputString("Họ và tên: ");
        this.phone = com.cyber.util.InputUtils.inputString("Số điện thoại: ", "^\\d{10,15}$", "Số điện thoại chỉ được chứa 10-15 chữ số.");
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}