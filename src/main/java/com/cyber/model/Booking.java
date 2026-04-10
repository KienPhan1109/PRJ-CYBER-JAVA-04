package com.cyber.model;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class Booking {
    private int bookingId;
    private int userId;
    private int computerId;
    private Timestamp startTime;
    private Timestamp endTime;
    private String status;
    private BigDecimal totalFee;
    private BigDecimal hourlyRateSnapshot;
    
    // Additional field for display
    private String computerName;
    private String userName;

    public Booking(int bookingId, int userId, int computerId, Timestamp startTime, Timestamp endTime, String status, BigDecimal totalFee, BigDecimal hourlyRateSnapshot) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.computerId = computerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.totalFee = totalFee;
        this.hourlyRateSnapshot = hourlyRateSnapshot;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getComputerId() {
        return computerId;
    }

    public void setComputerId(int computerId) {
        this.computerId = computerId;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public String getComputerName() {
        return computerName;
    }

    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public BigDecimal getHourlyRateSnapshot() {
        return hourlyRateSnapshot;
    }

    public void setHourlyRateSnapshot(BigDecimal hourlyRateSnapshot) {
        this.hourlyRateSnapshot = hourlyRateSnapshot;
    }
}