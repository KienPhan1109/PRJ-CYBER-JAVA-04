package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IFbOrderDAO;
import com.cyber.dao.IServiceItemDAO;
import com.cyber.dao.impl.FbOrderDAOImpl;
import com.cyber.dao.impl.ServiceItemDAOImpl;
import com.cyber.exception.BusinessException;
import com.cyber.model.ServiceItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ServiceItemService {

    private static ServiceItemService instance;
    private final IServiceItemDAO itemDAO;
    private final IFbOrderDAO orderDAO;

    private ServiceItemService() {
        this.itemDAO = ServiceItemDAOImpl.getInstance();
        this.orderDAO = FbOrderDAOImpl.getInstance();
    }

    public static synchronized ServiceItemService getInstance() {
        if (instance == null) {
            instance = new ServiceItemService();
        }
        return instance;
    }

    public List<ServiceItem> getAllServiceItems() throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return itemDAO.getAllServiceItems(conn);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh mục F&B: " + e.getMessage());
        }
    }

    public void addServiceItem(ServiceItem item) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            itemDAO.addServiceItem(conn, item);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi thêm món: " + e.getMessage());
        }
    }
    
    public ServiceItem getServiceItemById(int id) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return itemDAO.findById(conn, id);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi tìm kiếm món F&B: " + e.getMessage());
        }
    }

    public void updateServiceItem(ServiceItem item) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ServiceItem existing = itemDAO.findById(conn, item.getItemId());
            if (existing == null) {
                throw new BusinessException("NOT_FOUND", "Không tìm thấy món F&B có ID = " + item.getItemId());
            }
            itemDAO.updateServiceItem(conn, item);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật món: " + e.getMessage());
        }
    }

    public void deleteServiceItem(int id) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ServiceItem existing = itemDAO.findById(conn, id);
            if (existing == null) {
                throw new BusinessException("NOT_FOUND", "Không tìm thấy món có ID = " + id);
            }
            if (orderDAO.hasDependentOrders(conn, id)) {
                throw new BusinessException("DEPENDENCY_ERROR", "Không thể xóa món này do đã từng nằm trong hoá đơn F&B.");
            }
            itemDAO.deleteServiceItem(conn, id);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi xóa món (có thể đang có order sử dụng): " + e.getMessage());
        }
    }
}
