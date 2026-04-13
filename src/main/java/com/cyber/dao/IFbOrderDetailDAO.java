package com.cyber.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IFbOrderDetailDAO {
    void insertOrderDetail(Connection conn, int orderId, int menuItemId, int quantity,
                           BigDecimal unitPriceSnapshot, String itemNameSnapshot,
                           String itemDescription, String itemConfigJson,
                           BigDecimal discountApplied, String discountStrategyName) throws SQLException;

    List<Map<String, Object>> findDetailsByOrderId(Connection conn, int orderId) throws SQLException;

    BigDecimal calculateTotalOrder(Connection conn, int orderId) throws SQLException;
}
