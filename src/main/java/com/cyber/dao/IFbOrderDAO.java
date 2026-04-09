package com.cyber.dao;

import com.cyber.model.FbOrder;
import com.cyber.model.enums.FbOrderStatus;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface IFbOrderDAO {
    int createOrder(Connection conn, FbOrder order) throws SQLException;
    List<FbOrder> findAllOrdersByStatus(Connection conn, FbOrderStatus status) throws SQLException;
    List<FbOrder> findAllActiveOrdersWithDetails(Connection conn) throws SQLException;
    List<FbOrder> findActiveOrdersByUserIdWithDetails(Connection conn, int userId) throws SQLException;
    List<FbOrder> findAllOrdersByUserIdWithDetails(Connection conn, int userId) throws SQLException;
    FbOrder findOrderById(Connection conn, int orderId) throws SQLException;
    void updateOrderStatus(Connection conn, int orderId, FbOrderStatus newStatus) throws SQLException;
}