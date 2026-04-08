package com.cyber.dao;
import com.cyber.model.FbOrder;
import com.cyber.model.OrderDetail;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface IFbOrderDAO {
    int createOrder(Connection conn, FbOrder order) throws SQLException;
    void createOrderDetails(Connection conn, List<OrderDetail> details) throws SQLException;
    boolean hasDependentOrders(Connection conn, int itemId) throws SQLException;
}