package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IBookingDAO;
import com.cyber.dao.IComputerDAO;
import com.cyber.dao.impl.BookingDAOImpl;
import com.cyber.dao.impl.ComputerDAOImpl;
import com.cyber.exception.BusinessException;
import com.cyber.model.Computer;
import com.cyber.model.User;
import com.cyber.model.enums.LogType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ComputerService {

    private static ComputerService instance;
    private final IComputerDAO computerDAO;
    private final IBookingDAO bookingDAO;
    private final LogService logService;

    private ComputerService() {
        this.computerDAO = ComputerDAOImpl.getInstance();
        this.bookingDAO = BookingDAOImpl.getInstance();
        this.logService = LogService.getInstance();
    }

    public static synchronized ComputerService getInstance() {
        if (instance == null) {
            instance = new ComputerService();
        }
        return instance;
    }

    public List<Computer> getAllComputers() throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return computerDAO.getAllComputersForAdmin(conn);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách máy: " + e.getMessage());
        }
    }

    public List<Computer> getAvailableComputersByZone(com.cyber.model.enums.ComputerZone zone, java.sql.Timestamp start, java.sql.Timestamp end) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            com.cyber.dao.IBookingDAO bookingDAO = com.cyber.dao.impl.BookingDAOImpl.getInstance();
            return computerDAO.getAllActiveComputers(conn).stream()
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

    public void addComputer(Computer computer, User actor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            if (computerDAO.checkNameExists(conn, computer.getName())) {
                throw new BusinessException("DUPLICATE_NAME", "Tên máy '" + computer.getName() + "' đã tồn tại.");
            }
            computerDAO.addComputer(conn, computer);

            // Ghi log COMPUTER
            String action = String.format("Thêm máy trạm mới: %s (Khu vực: %s, Giá: %s/h)",
                    computer.getName(),
                    computer.getZone() != null ? computer.getZone().name() : "N/A",
                    com.cyber.util.FormatUtils.formatVND(computer.getPricePerHour()));
            logService.log(conn, LogType.COMPUTER, actor, action, null);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("DB_ERROR", "Lỗi thêm máy: " + e.getMessage());
        } catch (BusinessException be) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw be;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
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

    public void updateComputer(Computer computer, User actor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            Computer existing = computerDAO.findById(conn, computer.getComputerId());
            if (existing == null || existing.isDeleted()) {
                throw new BusinessException("NOT_FOUND", "Không tìm thấy máy có ID = " + computer.getComputerId());
            }

            // Quy tắc vàng (Hard Validation): Không cho sửa nếu đang IN_USE
            if (existing.getStatus() == com.cyber.model.enums.ComputerStatus.IN_USE) {
                throw new BusinessException("IN_USE", "Lỗi: Máy đang có khách sử dụng. Khách phải đăng xuất thì Admin mới được phép Sửa/Xóa/Bảo trì máy này!");
            }

            // Check name duplicate if name changed
            if (!existing.getName().equals(computer.getName()) && computerDAO.checkNameExists(conn, computer.getName())) {
                throw new BusinessException("DUPLICATE_NAME", "Tên máy '" + computer.getName() + "' đã được sử dụng bởi máy khác.");
            }
            computerDAO.updateComputer(conn, computer);

            // Ghi log COMPUTER
            String action = String.format("Cập nhật máy trạm: %s (ID: %d)",
                    computer.getName(), computer.getComputerId());
            logService.log(conn, LogType.COMPUTER, actor, action, computer.getComputerId());

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật máy: " + e.getMessage());
        } catch (BusinessException be) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw be;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public void toggleComputerStatus(int id, User actor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            Computer existing = computerDAO.findById(conn, id);
            if (existing == null || existing.isDeleted()) {
                throw new BusinessException("NOT_FOUND", "Không tìm thấy máy có ID = " + id);
            }

            // Quy tắc vàng (Hard Validation): Không cho sửa nếu đang IN_USE
            if (existing.getStatus() == com.cyber.model.enums.ComputerStatus.IN_USE) {
                throw new BusinessException("IN_USE", "Lỗi: Máy đang có khách sử dụng. Khách phải đăng xuất thì Admin mới được phép Sửa/Xóa/Bảo trì máy này!");
            }

            // Toggle Soft delete / Hidden status
            String actionVerb;
            if (existing.getStatus() == com.cyber.model.enums.ComputerStatus.HIDDEN) {
                existing.setStatus(com.cyber.model.enums.ComputerStatus.AVAILABLE);
                actionVerb = "Hiện";
            } else {
                existing.setStatus(com.cyber.model.enums.ComputerStatus.HIDDEN);
                actionVerb = "Ẩn";
            }
            
            // Re-use update method instead of delete query, or keep delete query if we modified it?
            // Wait, we modified deleteComputer in DAO to just set status = 'HIDDEN'.
            // It's better to just call updateComputer in DAO since we changed the whole object's status.
            computerDAO.updateComputer(conn, existing);

            // Ghi log COMPUTER
            String action = String.format("%s máy trạm: %s (ID: %d)",
                    actionVerb, existing.getName(), id);
            logService.log(conn, LogType.COMPUTER, actor, action, id);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("DB_ERROR", "Lỗi xóa máy (có thể máy đang có booking): " + e.getMessage());
        } catch (BusinessException be) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw be;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
