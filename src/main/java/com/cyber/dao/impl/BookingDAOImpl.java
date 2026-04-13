package com.cyber.dao.impl;

import com.cyber.dao.IBookingDAO;
import com.cyber.model.Booking;
import com.cyber.model.enums.BookingStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookingDAOImpl implements IBookingDAO {
    private static final BookingDAOImpl instance = new BookingDAOImpl();

    private BookingDAOImpl() {}

    public static BookingDAOImpl getInstance() {
        return instance;
    }

    private Booking mapRowToBooking(ResultSet rs) throws SQLException {
        String statusStr = rs.getString("status");
        BookingStatus status = (statusStr != null) ? BookingStatus.valueOf(statusStr.toUpperCase()) : BookingStatus.ACTIVE;
        
        Booking b = new Booking(
                rs.getInt("booking_id"),
                rs.getInt("user_id"),
                rs.getInt("computer_id"),
                rs.getTimestamp("start_time"),
                rs.getTimestamp("end_time"),
                status,
                rs.getBigDecimal("total_fee"),
                rs.getBigDecimal("hourly_rate_snapshot")
        );

        int sId = rs.getInt("staff_id");
        if (!rs.wasNull()) {
            b.setStaffId(sId);
        }
        return b;
    }

    @Override
    public int createBooking(Connection conn, Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (user_id, computer_id, start_time, end_time, status, total_fee, hourly_rate_snapshot) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, booking.getUserId());
            stmt.setInt(2, booking.getComputerId());
            stmt.setTimestamp(3, booking.getStartTime());
            if (booking.getEndTime() != null) {
                stmt.setTimestamp(4, booking.getEndTime());
            } else {
                stmt.setNull(4, Types.TIMESTAMP);
            }
            stmt.setString(5, booking.getStatus() != null ? booking.getStatus().name() : BookingStatus.ACTIVE.name());
            stmt.setBigDecimal(6, booking.getTotalFee());
            stmt.setBigDecimal(7, booking.getHourlyRateSnapshot());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Không thể tạo Booking.");
    }

    @Override
    public boolean isComputerAvailable(Connection conn, int computerId, Timestamp start, Timestamp end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookings WHERE computer_id = ? AND status IN ('PENDING', 'ACTIVE')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, computerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }
        }
        return false;
    }

    @Override
    public boolean isComputerAvailableForReservation(Connection conn, int computerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookings WHERE computer_id = ? AND status = 'RESERVED'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, computerId);
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
    public List<Booking> findActiveBookingsByUserId(Connection conn, int userId) throws SQLException {
        String sql = "SELECT b.*, c.name as computer_name " +
                "FROM bookings b " +
                "JOIN computers c ON b.computer_id = c.computer_id " +
                "WHERE b.user_id = ? AND b.status = 'ACTIVE' " +
                "ORDER BY b.start_time DESC";
        return executeBookingListQuery(conn, sql, userId);
    }

    @Override
    public List<Booking> findAllBookingsByUserId(Connection conn, int userId) throws SQLException {
        String sql = "SELECT b.*, c.name as computer_name " +
                "FROM bookings b " +
                "JOIN computers c ON b.computer_id = c.computer_id " +
                "WHERE b.user_id = ? " +
                "ORDER BY b.created_at DESC";
        return executeBookingListQuery(conn, sql, userId);
    }

    @Override
    public List<Booking> findAllActiveBookings(Connection conn) throws SQLException {
        String sql = "SELECT b.*, c.name as computer_name " +
                "FROM bookings b " +
                "JOIN computers c ON b.computer_id = c.computer_id " +
                "WHERE b.status = 'ACTIVE' " +
                "ORDER BY b.start_time ASC";
        return executeBookingListQuery(conn, sql, null);
    }

    private List<Booking> executeBookingListQuery(Connection conn, String sql, Integer userId) throws SQLException {
        List<Booking> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (userId != null) {
                stmt.setInt(1, userId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Booking b = mapRowToBooking(rs);
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
                    return mapRowToBooking(rs);
                }
            }
        }
        return null;
    }

    @Override
    public void updateBooking(Connection conn, Booking booking) throws SQLException {
        String sql = "UPDATE bookings SET start_time = ?, end_time = ?, status = ?, total_fee = ?, hourly_rate_snapshot = ?, staff_id = COALESCE(?, staff_id) WHERE booking_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, booking.getStartTime());
            stmt.setTimestamp(2, booking.getEndTime());
            stmt.setString(3, booking.getStatus() != null ? booking.getStatus().name() : BookingStatus.ACTIVE.name());
            stmt.setBigDecimal(4, booking.getTotalFee());
            stmt.setBigDecimal(5, booking.getHourlyRateSnapshot());
            if (booking.getStaffId() != null) {
                stmt.setInt(6, booking.getStaffId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            stmt.setInt(7, booking.getBookingId());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Booking> findPendingBookings(Connection conn) throws SQLException {
        String sql = "SELECT b.*, c.name as computer_name, u.full_name as user_name " +
                "FROM bookings b " +
                "JOIN computers c ON b.computer_id = c.computer_id " +
                "JOIN users u ON b.user_id = u.user_id " +
                "WHERE b.status IN ('PENDING', 'RESERVED') " +
                "ORDER BY b.created_at ASC";
        return executeDetailedBookingListQuery(conn, sql, null);
    }

    @Override
    public List<Booking> findOverdueReservations(Connection conn, int overdueMinutes) throws SQLException {
        String sql = "SELECT b.*, c.name as computer_name, u.full_name as user_name " +
                "FROM bookings b " +
                "JOIN computers c ON b.computer_id = c.computer_id " +
                "JOIN users u ON b.user_id = u.user_id " +
                "WHERE b.status = 'RESERVED' " +
                "AND b.start_time < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        return executeDetailedBookingListQuery(conn, sql, overdueMinutes);
    }

    private List<Booking> executeDetailedBookingListQuery(Connection conn, String sql, Integer param) throws SQLException {
        List<Booking> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (param != null) {
                stmt.setInt(1, param);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Booking b = mapRowToBooking(rs);
                    b.setComputerName(rs.getString("computer_name"));
                    b.setUserName(rs.getString("user_name"));
                    list.add(b);
                }
            }
        }
        return list;
    }

    @Override
    public Booking findNextReservation(Connection conn, int computerId) throws SQLException {
        String sql = "SELECT * FROM bookings WHERE computer_id = ? AND status = 'RESERVED' ORDER BY start_time ASC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, computerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToBooking(rs);
                }
            }
        }
        return null;
    }
}