package com.cyber.dao.impl;
import com.cyber.dao.IBookingDAO;
import com.cyber.model.Booking;
import java.sql.*;

public class BookingDAOImpl implements IBookingDAO {

    // Singleton Pattern
    private static BookingDAOImpl instance;
    private BookingDAOImpl() {}
    public static synchronized BookingDAOImpl getInstance() {
        if (instance == null) {
            instance = new BookingDAOImpl();
        }
        return instance;
    }

    @Override
    public int createBooking(Connection conn, Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (user_id, computer_id, start_time, end_time, status, total_fee, hourly_rate_snapshot) VALUES (?, ?, ?, ?, 'PENDING', ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, booking.getUserId());
            stmt.setInt(2, booking.getComputerId());
            stmt.setTimestamp(3, booking.getStartTime());
            stmt.setTimestamp(4, booking.getEndTime());
            stmt.setBigDecimal(5, booking.getTotalFee());
            stmt.setBigDecimal(6, booking.getHourlyRateSnapshot());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Không thể tạo Booking.");
    }

    @Override
    public boolean isComputerAvailable(Connection conn, int computerId, Timestamp start, Timestamp end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookings WHERE computer_id = ? AND status != 'CANCELLED' AND (start_time < ? AND end_time > ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, computerId);
            stmt.setTimestamp(2, end);
            stmt.setTimestamp(3, start);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        }
        return false;
    }

    @Override
    public boolean hasDependentBookings(Connection conn, int computerId) throws SQLException {
        String sql = "SELECT 1 FROM bookings WHERE computer_id = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, computerId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public java.util.List<Booking> findActiveBookingsByUserId(Connection conn, int userId) throws SQLException {
        java.util.List<Booking> list = new java.util.ArrayList<>();
        String sql = "SELECT b.*, c.name as computer_name " +
                     "FROM bookings b " +
                     "JOIN computers c ON b.computer_id = c.computer_id " +
                     "WHERE b.user_id = ? AND b.status IN ('PENDING', 'IN_PROGRESS', 'ACTIVE') " +
                     "ORDER BY b.start_time DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Booking b = new Booking(
                        rs.getInt("booking_id"),
                        rs.getInt("user_id"),
                        rs.getInt("computer_id"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"),
                        rs.getString("status"),
                        rs.getBigDecimal("total_fee"),
                        rs.getBigDecimal("hourly_rate_snapshot")
                    );
                    b.setComputerName(rs.getString("computer_name"));
                    list.add(b);
                }
            }
        }
        return list;
    }

    @Override
    public java.util.List<Booking> findAllActiveBookings(Connection conn) throws SQLException {
        java.util.List<Booking> list = new java.util.ArrayList<>();
        String sql = "SELECT b.*, c.name as computer_name " +
                     "FROM bookings b " +
                     "JOIN computers c ON b.computer_id = c.computer_id " +
                     "WHERE b.status = 'ACTIVE' " +
                     "ORDER BY b.start_time ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Booking b = new Booking(
                        rs.getInt("booking_id"),
                        rs.getInt("user_id"),
                        rs.getInt("computer_id"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"),
                        rs.getString("status"),
                        rs.getBigDecimal("total_fee"),
                        rs.getBigDecimal("hourly_rate_snapshot")
                    );
                    b.setComputerName(rs.getString("computer_name"));
                    list.add(b);
                }
            }
        }
        return list;
    }

    @Override
    public Booking findById(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT * FROM bookings WHERE booking_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Booking(
                        rs.getInt("booking_id"),
                        rs.getInt("user_id"),
                        rs.getInt("computer_id"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"),
                        rs.getString("status"),
                        rs.getBigDecimal("total_fee"),
                        rs.getBigDecimal("hourly_rate_snapshot")
                    );
                }
            }
        }
        return null;
    }

    @Override
    public void updateBooking(Connection conn, Booking booking) throws SQLException {
        String sql = "UPDATE bookings SET start_time = ?, end_time = ?, status = ?, total_fee = ?, hourly_rate_snapshot = ? WHERE booking_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, booking.getStartTime());
            stmt.setTimestamp(2, booking.getEndTime());
            stmt.setString(3, booking.getStatus());
            stmt.setBigDecimal(4, booking.getTotalFee());
            stmt.setBigDecimal(5, booking.getHourlyRateSnapshot());
            stmt.setInt(6, booking.getBookingId());
            stmt.executeUpdate();
        }
    }
}