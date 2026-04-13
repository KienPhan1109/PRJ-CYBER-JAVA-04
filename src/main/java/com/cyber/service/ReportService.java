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
    private static ReportService instance;
    private final IReportDAO reportDAO;

    private ReportService() {
        this.reportDAO = ReportDAOImpl.getInstance();
    }

    public static synchronized ReportService getInstance() {
         if (instance == null) instance = new ReportService();
         return instance;
    }

    public BigDecimal getTotalBookingRevenue(Timestamp start, Timestamp end) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) { 
            return reportDAO.getTotalBookingRevenue(conn, start, end); 
        } catch (SQLException e) { 
            throw new BusinessException("DB_ERROR", "Lỗi lấy doanh thu máy: " + e.getMessage()); 
        }
    }

    public BigDecimal getTotalFbRevenue(Timestamp start, Timestamp end) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) { 
            return reportDAO.getTotalFbRevenue(conn, start, end); 
        } catch (SQLException e) { 
            throw new BusinessException("DB_ERROR", "Lỗi lấy doanh thu F&B: " + e.getMessage()); 
        }
    }

    public List<Map<String, Object>> getTopSellingItems(Timestamp start, Timestamp end, int limit) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) { 
            return reportDAO.getTopSellingItems(conn, start, end, limit); 
        } catch (SQLException e) { 
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách món F&B hot: " + e.getMessage()); 
        }
    }

    public List<Map<String, Object>> getMachineUsageStats(Timestamp start, Timestamp end) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) { 
            return reportDAO.getMachineUsageStats(conn, start, end); 
        } catch (SQLException e) { 
            throw new BusinessException("DB_ERROR", "Lỗi lấy thống kê máy: " + e.getMessage()); 
        }
    }
}
