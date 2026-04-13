package com.cyber.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public interface IReportDAO {
    BigDecimal getTotalBookingRevenue(Connection conn, Timestamp start, Timestamp end, Integer staffId) throws SQLException;
    BigDecimal getTotalFbRevenue(Connection conn, Timestamp start, Timestamp end, Integer staffId) throws SQLException;
    List<Map<String, Object>> getTopSellingItems(Connection conn, Timestamp start, Timestamp end, int limit, Integer staffId) throws SQLException;
    List<Map<String, Object>> getMachineUsageStats(Connection conn, Timestamp start, Timestamp end, Integer staffId) throws SQLException;
}
