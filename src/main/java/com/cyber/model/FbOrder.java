package com.cyber.model;
import java.math.BigDecimal;

public class FbOrder {
    private int orderId;
    private int bookingId;
    private String status;
    private BigDecimal totalAmount;

    public FbOrder(int bookingId, String status, BigDecimal totalAmount) {
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

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}