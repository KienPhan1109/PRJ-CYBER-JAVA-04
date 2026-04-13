package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.ILogDAO;
import com.cyber.dao.impl.LogDAOImpl;
import com.cyber.exception.BusinessException;
import com.cyber.model.SystemLog;
import com.cyber.model.User;
import com.cyber.model.enums.LogType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Service Layer cho Hệ thống Audit Log.
 * <p>
 * PHÂN QUYỀN XEM LOG:
 * <ul>
 *   <li>COMPUTER log & FB log  → Admin và Staff đều xem được toàn bộ.</li>
 *   <li>USER log               → Admin xem toàn bộ;
 *                                Staff chỉ xem log do chính mình thực hiện.</li>
 * </ul>
 * <p>
 * TÍCH HỢP TRANSACTION:
 * Phương thức {@link #log(Connection, LogType, User, String, Integer)} nhận
 * Connection từ ngoài để ghi log trong cùng transaction với nghiệp vụ chính.
 */
public class LogService {

    private static final LogService INSTANCE = new LogService();
    private final ILogDAO logDAO;

    private LogService() {
        this.logDAO = LogDAOImpl.getInstance();
    }

    public static LogService getInstance() {
        return INSTANCE;
    }

    // =====================================================
    // WRITE — Ghi log trong Transaction của nghiệp vụ chính
    // =====================================================

    /**
     * Ghi một bản ghi audit log vào DB.
     * <p>
     * Phương thức này nhận Connection đang mở của transaction nghiệp vụ chính,
     * đảm bảo log được commit hoặc rollback cùng với nghiệp vụ đó.
     *
     * @param conn     Connection đang chứa transaction nghiệp vụ
     * @param logType  Nhóm log: USER, COMPUTER, FB
     * @param actor    User đang thực hiện hành động (Admin hoặc Staff)
     * @param action   Mô tả ngắn, VD: "Nạp 50,000 VND cho KH#5"
     * @param targetId ID đối tượng bị tác động (nullable nếu không áp dụng)
     */
    public void log(Connection conn, LogType logType, User actor,
                    String action, Integer targetId) throws BusinessException {
        try {
            int actorId = (actor != null) ? actor.getUserId() : 0;
            SystemLog log = new SystemLog(logType, actorId, action, targetId);
            logDAO.insertLog(conn, log);
        } catch (SQLException e) {
            // Ném BusinessException để caller có thể rollback transaction
            throw new BusinessException("LOG_ERROR",
                    "Lỗi ghi audit log: " + e.getMessage());
        }
    }

    /**
     * Tiện ích: Ghi log đơn giản với Connection mới (dùng khi không có transaction).
     * Ít được dùng; ưu tiên dùng {@link #log(Connection, LogType, User, String, Integer)}.
     */
    public void logStandalone(LogType logType, User actor,
                               String action, Integer targetId) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            int actorId = (actor != null) ? actor.getUserId() : 0;
            logDAO.insertLog(conn, new SystemLog(logType, actorId, action, targetId));
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("LOG_ERROR", "Lỗi ghi log standalone: " + e.getMessage());
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {} }
        }
    }

    // =====================================================
    // READ — Lấy log có phân quyền theo Role
    // =====================================================

    /**
     * Lấy danh sách log kiểm toán với phân quyền:
     * <ul>
     *   <li>COMPUTER log → Bất kỳ ai (Admin/Staff) đều xem full.</li>
     *   <li>FB log       → Bất kỳ ai (Admin/Staff) đều xem full.</li>
     *   <li>USER log     → Admin: xem full; Staff: chỉ xem log của mình.</li>
     * </ul>
     *
     * @param logType     Loại log muốn xem
     * @param currentUser Người đang đăng nhập (để kiểm tra role và actor_id)
     * @return Danh sách SystemLog, mới nhất trước
     */
    public List<SystemLog> getLogsWithPermission(LogType logType,
                                                  User currentUser) throws BusinessException {
        String roleName = currentUser.getRole() != null
                ? currentUser.getRole().name()
                : "";

        try (Connection conn = DatabaseConnection.getConnection()) {
            if ("ADMIN".equalsIgnoreCase(roleName) || logType == LogType.COMPUTER || logType == LogType.FB) {
                // Admin xem toàn bộ; Staff xem toàn bộ COMPUTER & FB log
                return logDAO.getLogsByType(conn, logType);
            } else {
                // Staff: chỉ xem USER log do chính mình thực hiện
                return logDAO.getLogsByTypeAndActor(conn, logType,
                        currentUser.getUserId());
            }
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR",
                    "Lỗi lấy audit log: " + e.getMessage());
        }
    }
}
