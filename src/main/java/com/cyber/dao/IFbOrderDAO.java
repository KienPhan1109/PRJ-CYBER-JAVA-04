package com.cyber.dao;

import com.cyber.model.FbOrder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface IFbOrderDAO {
    int createOrder(Connection conn, FbOrder order) throws SQLException;
    List<FbOrder> findAllOrdersByStatus(Connection conn, String status) throws SQLException;
    List<FbOrder> findAllActiveOrdersWithDetails(Connection conn) throws SQLException;
    List<FbOrder> findActiveOrdersByUserIdWithDetails(Connection conn, int userId) throws SQLException;
    void updateOrderStatus(Connection conn, int orderId, String newStatus) throws SQLException;
}