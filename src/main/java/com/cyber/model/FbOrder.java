package com.cyber.model;
import com.cyber.model.enums.FbOrderStatus;
import java.math.BigDecimal;

public class FbOrder {
    private int orderId;
    private int userId;
    private Integer bookingId;
    private FbOrderStatus status;
    private BigDecimal totalAmount;
    private String userName;
    private String computerName;

    public FbOrder(int userId, Integer bookingId, FbOrderStatus status, BigDecimal totalAmount) {
        this.userId = userId;
        this.bookingId = bookingId;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Integer getBookingId() {
        return bookingId;
    }

    public FbOrderStatus getStatus() {
        return status;
    }

    public void setStatus(FbOrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getComputerName() {
        return computerName;
    }

    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }
}