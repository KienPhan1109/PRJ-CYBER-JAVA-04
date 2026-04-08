package com.cyber.dao;
import com.cyber.model.ServiceItem;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface IServiceItemDAO {
    List<ServiceItem> getAllServiceItems(Connection conn) throws SQLException;
    ServiceItem findById(Connection conn, int itemId) throws SQLException;
    int addServiceItem(Connection conn, ServiceItem item) throws SQLException;
    void updateServiceItem(Connection conn, ServiceItem item) throws SQLException;
    void deleteServiceItem(Connection conn, int itemId) throws SQLException;
    void deductStock(Connection conn, int itemId, int quantity) throws SQLException;
}