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

    public User(String username, String passwordHash, Role role, BigDecimal balance, String fullName, String phone, Timestamp createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.balance = balance;
        this.fullName = fullName;
        this.phone = phone;
        this.createdAt = createdAt;
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