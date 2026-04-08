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
        String sql = "INSERT INTO bookings (user_id, computer_id, start_time, end_time, status, total_fee) VALUES (?, ?, ?, ?, 'PENDING', ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, booking.getUserId());
            stmt.setInt(2, booking.getComputerId());
            stmt.setTimestamp(3, booking.getStartTime());
            stmt.setTimestamp(4, booking.getEndTime());
            stmt.setBigDecimal(5, booking.getTotalFee());
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
}