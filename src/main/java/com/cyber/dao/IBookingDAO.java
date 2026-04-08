package com.cyber.dao;
import com.cyber.model.Booking;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

public interface IBookingDAO {
    int createBooking(Connection conn, Booking booking) throws SQLException;
    boolean isComputerAvailable(Connection conn, int computerId, Timestamp start, Timestamp end) throws SQLException;
    boolean hasDependentBookings(Connection conn, int computerId) throws SQLException;
}