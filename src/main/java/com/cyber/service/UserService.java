package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IUserDAO;
import com.cyber.dao.impl.UserDAOImpl;
import com.cyber.exception.BusinessException;
import com.cyber.model.SystemLog;
import com.cyber.model.User;
import com.cyber.model.enums.LogType;
import com.cyber.model.enums.UserStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserService {
    private static UserService instance;
    private final IUserDAO   userDAO;
    private final LogService logService;

    private UserService() {
        this.userDAO    = UserDAOImpl.getInstance();
        this.logService = LogService.getInstance();
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public List<User> getAllUsers() throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return userDAO.getAllUsers(conn);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách User: " + e.getMessage());
        }
    }

    public List<User> searchUsersByName(String name) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return userDAO.searchUsersByName(conn, name);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách User: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin User theo ID (realtime từ DB).
     * Dùng khi View cần refresh dữ liệu mà không vi phạm 3-Tier.
     */
    public User getUserById(int userId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            User user = userDAO.findById(conn, userId);
            if (user == null) {
                throw new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng (ID=" + userId + ")");
            }
            return user;
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi truy vấn User: " + e.getMessage());
        }
    }

    public boolean checkUsernameDuplicate(String username, int excludeUserId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<User> all = userDAO.getAllUsers(conn);
            return all.stream().anyMatch(u -> u.getUserId() != excludeUserId && u.getUsername().equalsIgnoreCase(username));
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi kiểm tra trùng lặp: " + e.getMessage());
        }
    }

    public boolean verifyPassword(int userId, String rawPassword) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            User user = userDAO.findById(conn, userId);
            if (user == null) return false;
            // Dùng AuthService để hash nhưng AuthService đóng gói, ta cần hash ở đây
            // Giải pháp gọn là check qua userDAO.findByUsernameAndPassword
            // Tuy nhiên hash string là private trong AuthService.
            // Để tái sử dụng, gọi thủ công mãnh liệt qua hàm login nếu được, 
            // hoặc ta add hàm updateLoginInfo vào AuthService xử lý cho chuẩn.
            throw new BusinessException("NOT_IMPL", "Chưa hỗ trợ");
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi kiểm tra mật khẩu: " + e.getMessage());
        }
    }

    public void updateUserStatus(int userId, UserStatus status,
                                  User actor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User user = userDAO.findById(conn, userId);
            if (user == null) {
                throw new BusinessException("USER_NOT_FOUND",
                        "Không tìm thấy người dùng (ID=" + userId + ")");
            }

            // Bảo vệ ADMIN: Không thể khóa tài khoản ADMIN
            if (user.getRole() == com.cyber.model.enums.UserRole.ADMIN) {
                throw new BusinessException("SECURITY_ERROR", "Không được phép thay đổi trạng thái của Quản trị viên (ADMIN).");
            }

            // Ràng buộc: Muốn KHÓA phải có số dư = 0
            if (status == UserStatus.LOCKED && user.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException("BALANCE_EXISTS",
                        "Không thể khóa tài khoản khi số dư còn " + com.cyber.util.FormatUtils.formatVND(user.getBalance()) + ". Vui lòng rút tiền về 0đ trước!");
            }

            userDAO.updateUserStatus(conn, userId, status);

            // Ghi log trong cùng transaction
            String action = String.format("Đổi trạng thái User #%d (%s) -> %s",
                    userId, user.getUsername(), status.name());
            logService.log(conn, LogType.USER, actor, action, userId);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("DB_ERROR",
                    "Lỗi cập nhật trạng thái User: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public void topUpUser(int userId, BigDecimal amount,
                           User actor) throws BusinessException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Số tiền nạp phải lớn hơn 0");
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User user = userDAO.findById(conn, userId);
            if (user == null) {
                throw new BusinessException("USER_NOT_FOUND",
                        "Không tìm thấy người dùng (ID=" + userId + ")");
            }

            // Chặn nạp tiền cho tài khoản bị khóa (LOCKED)
            if (user.getStatus() == UserStatus.LOCKED) {
                throw new BusinessException("ACCESS_DENIED", "Tài khoản đang bị khóa, không được phép nạp tiền.");
            }

            userDAO.addBalance(conn, userId, amount);

            // Ghi log trong cùng transaction
            String action = String.format("Nạp %s cho User #%d (%s)",
                    com.cyber.util.FormatUtils.formatVND(amount),
                    userId, user.getUsername());
            logService.log(conn, LogType.USER, actor, action, userId);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("DB_ERROR",
                    "Lỗi nạp tiền cho User: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public void updateUser(User user, User actor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            User existing = userDAO.findById(conn, user.getUserId());
            if (existing == null || existing.isDeleted()) {
                throw new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng");
            }

            // Bảo vệ ADMIN: Không được sửa thông tin ADMIN từ trang quản lý chung
            if (existing.getRole() == com.cyber.model.enums.UserRole.ADMIN) {
                throw new BusinessException("SECURITY_ERROR", "Không được phép chỉnh sửa thông tin của Quản trị viên (ADMIN).");
            }

            // Kiểm tra trùng username nếu username thay đổi
            if (!existing.getUsername().equalsIgnoreCase(user.getUsername())) {
                List<User> allUsers = userDAO.getAllUsers(conn);
                boolean duplicateExists = allUsers.stream()
                        .anyMatch(u -> u.getUserId() != user.getUserId()
                                && u.getUsername().equalsIgnoreCase(user.getUsername()));
                if (duplicateExists) {
                    throw new BusinessException("DUPLICATE_USERNAME",
                            "Tên tài khoản '" + user.getUsername() + "' đã tồn tại! Vui lòng chọn tên khác.");
                }
            }

            conn.setAutoCommit(false);
            try {
                userDAO.updateUser(conn, user);

                // Ghi audit log
                if (actor != null) {
                    String action = String.format("Sửa thông tin User #%d (%s)",
                            user.getUserId(), user.getUsername());
                    logService.log(conn, LogType.USER, actor, action, user.getUserId());
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật User: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public void deleteUser(int userId, User actor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User existing = userDAO.findById(conn, userId);
            if (existing == null || existing.isDeleted()) {
                throw new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng");
            }
            if (existing.getRole() == com.cyber.model.enums.UserRole.ADMIN) {
                throw new BusinessException("SECURITY_ERROR", "Không được phép xóa tài khoản Quản trị viên (ADMIN).");
            }
            if (existing.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException("BALANCE_EXISTS",
                        "Tài khoản vẫn còn số dư " + com.cyber.util.FormatUtils.formatVND(existing.getBalance()) + ". Vui lòng rút tiền về 0đ trước khi xóa!");
            }

            userDAO.deleteUser(conn, userId);

            String action = String.format("Xóa (soft) User #%d (%s)", userId, existing.getUsername());
            logService.log(conn, LogType.USER, actor, action, userId);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("DB_ERROR", "Lỗi xóa User: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public void deductMoney(int userId, double amount, int staffId, String reason) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int rows = userDAO.updateBalance(conn, userId, BigDecimal.valueOf(-amount));
            if (rows == 0) {
                throw new BusinessException("INSUFFICIENT_FUNDS", "Số dư không đủ để thanh toán!");
            }

            // Ghi log qua LogService (trong cùng transaction)
            User staffActor = new User();
            staffActor.setUserId(staffId);
            logService.log(conn, LogType.USER, staffActor, reason, userId);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("DB_ERROR", "Lỗi trừ tiền: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Trừ tiền thủ công cho User (Admin/Staff thao tác).
     * Không cho số dư âm.
     */
    public void deductBalanceManual(int userId, BigDecimal amount, User actor) throws BusinessException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Số tiền trừ phải lớn hơn 0.");
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User user = userDAO.findById(conn, userId);
            if (user == null) {
                throw new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng (ID=" + userId + ")");
            }
            if (user.getBalance().compareTo(amount) < 0) {
                throw new BusinessException("INSUFFICIENT_FUNDS",
                        "Số dư hiện tại là " + com.cyber.util.FormatUtils.formatVND(user.getBalance())
                        + ", không đủ để trừ " + com.cyber.util.FormatUtils.formatVND(amount) + ". Số dư không được âm!");
            }

            userDAO.deductBalance(conn, userId, amount);

            String action = String.format("Trừ tiền %s cho User #%d (%s)",
                    com.cyber.util.FormatUtils.formatVND(amount), userId, user.getUsername());
            logService.log(conn, LogType.USER, actor, action, userId);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("DB_ERROR", "Lỗi trừ tiền: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
