package com.cyber.dao;

import com.cyber.model.SystemLog;
import com.cyber.model.enums.LogType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * DAO Interface cho bảng system_logs.
 * <p>
 * insertLog nhận Connection từ ngoài để có thể tham gia vào Transaction
 * cùng với các thao tác nghiệp vụ chính (topUp, updateOrder, v.v.).
 * <p>
 * Các method read tự mở Connection riêng (chỉ đọc, không cần transaction).
 */
public interface ILogDAO {

    /**
     * Ghi một bản ghi log vào DB.
     * <b>Chạy trong cùng Transaction</b> của Connection được truyền vào.
     *
     * @param conn   Connection đang có transaction mở sẵn
     * @param log    Bản ghi log cần lưu
     */
    void insertLog(Connection conn, SystemLog log) throws SQLException;

    /**
     * Lấy toàn bộ log theo loại (không phân biệt actor).
     * Dùng cho: Admin xem mọi loại log; Staff xem COMPUTER và FB log.
     *
     * @param logType loại log cần lấy
     * @return Danh sách log, sắp xếp mới nhất trước
     */
    List<SystemLog> getLogsByType(LogType logType) throws SQLException;

    /**
     * Lấy log theo loại và actor (Staff chỉ được xem log của chính mình đối với USER log).
     *
     * @param logType loại log
     * @param actorId ID của Staff/Admin cần lọc
     * @return Danh sách log, sắp xếp mới nhất trước
     */
    List<SystemLog> getLogsByTypeAndActor(LogType logType, int actorId) throws SQLException;
}
