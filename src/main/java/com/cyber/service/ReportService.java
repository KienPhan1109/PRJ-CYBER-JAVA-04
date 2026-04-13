package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IReportDAO;
import com.cyber.dao.impl.ReportDAOImpl;
import com.cyber.exception.BusinessException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class ReportService {
    private static final ReportService INSTANCE = new ReportService();
    private final IReportDAO reportDAO;

    private ReportService() {
        this.reportDAO = ReportDAOImpl.getInstance();
    }

    public static ReportService getInstance() {
         return INSTANCE;
    }

    public BigDecimal getTotalBookingRevenue(Timestamp start, Timestamp end, Integer staffId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) { 
            return reportDAO.getTotalBookingRevenue(conn, start, end, staffId); 
        } catch (SQLException e) { 
            throw new BusinessException("DB_ERROR", "Lỗi lấy doanh thu máy: " + e.getMessage()); 
        }
    }

    public BigDecimal getTotalFbRevenue(Timestamp start, Timestamp end, Integer staffId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) { 
            return reportDAO.getTotalFbRevenue(conn, start, end, staffId); 
        } catch (SQLException e) { 
            throw new BusinessException("DB_ERROR", "Lỗi lấy doanh thu F&B: " + e.getMessage()); 
        }
    }

    public List<Map<String, Object>> getTopSellingItems(Timestamp start, Timestamp end, int limit, Integer staffId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) { 
            return reportDAO.getTopSellingItems(conn, start, end, limit, staffId); 
        } catch (SQLException e) { 
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách món F&B hot: " + e.getMessage()); 
        }
    }

    public List<Map<String, Object>> getMachineUsageStats(Timestamp start, Timestamp end, Integer staffId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) { 
            return reportDAO.getMachineUsageStats(conn, start, end, staffId); 
        } catch (SQLException e) { 
            throw new BusinessException("DB_ERROR", "Lỗi lấy thống kê máy: " + e.getMessage()); 
        }
    }
}
