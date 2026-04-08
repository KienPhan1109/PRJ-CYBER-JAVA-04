package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IBookingDAO;
import com.cyber.dao.IComputerDAO;
import com.cyber.dao.impl.BookingDAOImpl;
import com.cyber.dao.impl.ComputerDAOImpl;
import com.cyber.exception.BusinessException;
import com.cyber.model.Computer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ComputerService {

    private static ComputerService instance;
    private final IComputerDAO computerDAO;
    private final IBookingDAO bookingDAO;

    private ComputerService() {
        this.computerDAO = ComputerDAOImpl.getInstance();
        this.bookingDAO = BookingDAOImpl.getInstance();
    }

    public static synchronized ComputerService getInstance() {
        if (instance == null) {
            instance = new ComputerService();
        }
        return instance;
    }

    public List<Computer> getAllComputers() throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return computerDAO.getAllComputers(conn);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách máy: " + e.getMessage());
        }
    }

    public List<Computer> getAvailableComputersByZone(com.cyber.model.enums.ComputerZone zone, java.sql.Timestamp start, java.sql.Timestamp end) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            com.cyber.dao.IBookingDAO bookingDAO = com.cyber.dao.impl.BookingDAOImpl.getInstance();
            return computerDAO.getAllComputers(conn).stream()
                .filter(c -> c.getStatus() == com.cyber.model.enums.ComputerStatus.AVAILABLE 
                          && (zone == null || c.getZone() == zone))
                .filter(c -> {
                    try {
                        return bookingDAO.isComputerAvailable(conn, c.getComputerId(), start, end);
                    } catch (SQLException e) {
                        return false;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách máy: " + e.getMessage());
        }
    }

    public void addComputer(Computer computer) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (computerDAO.checkNameExists(conn, computer.getName())) {
                throw new BusinessException("DUPLICATE_NAME", "Tên máy '" + computer.getName() + "' đã tồn tại.");
            }
            computerDAO.addComputer(conn, computer);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi thêm máy: " + e.getMessage());
        }
    }
    
    public Computer getComputerById(int id) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return computerDAO.findById(conn, id);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi tìm kiếm máy: " + e.getMessage());
        }
    }

    public boolean isNameExists(String name) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return computerDAO.checkNameExists(conn, name);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi kiểm tra tên máy: " + e.getMessage());
        }
    }

    public void updateComputer(Computer computer) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Computer existing = computerDAO.findById(conn, computer.getComputerId());
            if (existing == null) {
                throw new BusinessException("NOT_FOUND", "Không tìm thấy máy có ID = " + computer.getComputerId());
            }
            // Check name duplicate if name changed
            if (!existing.getName().equals(computer.getName()) && computerDAO.checkNameExists(conn, computer.getName())) {
                throw new BusinessException("DUPLICATE_NAME", "Tên máy '" + computer.getName() + "' đã được sử dụng bởi máy khác.");
            }
            computerDAO.updateComputer(conn, computer);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật máy: " + e.getMessage());
        }
    }

    public void deleteComputer(int id) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Computer existing = computerDAO.findById(conn, id);
            if (existing == null) {
                throw new BusinessException("NOT_FOUND", "Không tìm thấy máy có ID = " + id);
            }
            if (bookingDAO.hasDependentBookings(conn, id)) {
                throw new BusinessException("DEPENDENCY_ERROR", "Không thể xóa. Máy trạm này đã từng hoặc đang được Order/Booking.");
            }
            computerDAO.deleteComputer(conn, id);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi xóa máy (có thể máy đang có booking): " + e.getMessage());
        }
    }
}
