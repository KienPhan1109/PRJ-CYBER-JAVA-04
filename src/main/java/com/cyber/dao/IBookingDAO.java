package com.cyber.dao;
import com.cyber.model.Booking;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;


public interface IBookingDAO {
    int createBooking(Connection conn, Booking booking) throws SQLException;

    boolean hasDependentBookings(Connection conn, int computerId) throws SQLException;

    boolean isComputerAvailable(Connection conn, int computerId, Timestamp start, Timestamp end) throws SQLException;

    boolean isComputerAvailableForReservation(Connection conn, int computerId) throws SQLException;

    List<Booking> findActiveBookingsByUserId(Connection conn, int userId) throws SQLException;

    List<Booking> findAllBookingsByUserId(Connection conn, int userId) throws SQLException;

    List<Booking> findAllActiveBookings(Connection conn) throws SQLException;

    Booking findById(Connection conn, int bookingId) throws SQLException;

    void updateBooking(Connection conn, Booking booking) throws SQLException;

    List<Booking> findPendingBookings(Connection conn) throws SQLException;

    List<Booking> findOverdueReservations(Connection conn, int overdueMinutes) throws SQLException;

    Booking findNextReservation(Connection conn, int computerId) throws SQLException;
}