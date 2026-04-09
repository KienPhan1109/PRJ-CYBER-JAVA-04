package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IFbMenuItemDAO;
import com.cyber.dao.IFbOptionDAO;
import com.cyber.dao.IFbOrderDAO;
import com.cyber.dao.IFbOrderDetailDAO;
import com.cyber.dao.impl.FbMenuItemDAOImpl;
import com.cyber.dao.impl.FbOptionDAOImpl;
import com.cyber.dao.impl.FbOrderDAOImpl;
import com.cyber.dao.impl.FbOrderDetailDAOImpl;
import com.cyber.exception.BusinessException;
import com.cyber.model.FbOrder;
import com.cyber.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class FbOrderService {

    private static FbOrderService instance;
    private final IFbOrderDAO       orderDAO;
    private final IFbMenuItemDAO    menuItemDAO;
    private final IFbOptionDAO      optionDAO;
    private final IFbOrderDetailDAO orderDetailDAO;

    private FbOrderService() {
        this.orderDAO       = FbOrderDAOImpl.getInstance();
        this.menuItemDAO    = FbMenuItemDAOImpl.getInstance();
        this.optionDAO      = FbOptionDAOImpl.getInstance();
        this.orderDetailDAO = FbOrderDetailDAOImpl.getInstance();
    }

    public static synchronized FbOrderService getInstance() {
        if (instance == null) {
            instance = new FbOrderService();
        }
        return instance;
    }

    // -------------------------------------------------------
    // Staff: Quản lý đơn hàng
    // -------------------------------------------------------

    public List<FbOrder> getPendingOrders() throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return orderDAO.findAllActiveOrdersWithDetails(conn);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách Order: " + e.getMessage());
        }
    }

    public List<FbOrder> getActiveOrdersByUserId(int userId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return orderDAO.findActiveOrdersByUserIdWithDetails(conn, userId);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy danh sách Order của khách: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getOrderDetails(int orderId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return orderDetailDAO.findDetailsByOrderId(conn, orderId);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy chi tiết đơn: " + e.getMessage());
        }
    }

    public void updateOrderStatus(int orderId, String newStatus) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            orderDAO.updateOrderStatus(conn, orderId, newStatus);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật Order: " + e.getMessage());
        }
    }


    // -------------------------------------------------------
    // Customer: Đặt đồ ăn nâng cao (Phase 2)
    // Dùng fb_menu_items + Decorator + Strategy
    // Lưu vào fb_order_details với đầy đủ config JSON và discount info
    // -------------------------------------------------------

    /**
     * Đặt đơn hàng F&B nâng cao.
     * View chịu trách nhiệm build IBillable (Decorator chain) và Strategy,
     * đóng gói kết quả vào FbAdvancedCartItem rồi truyền xuống đây.
     * Service chịu trách nhiệm: kiểm tra user, trừ tiền, trừ stock, lưu DB — trong 1 transaction.
     */
    public void orderFoodAdvanced(int userId, Integer bookingId,
                                  List<FbAdvancedCartItem> cartItems) throws BusinessException {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BusinessException("ERR_EMPTY_CART", "Giỏ hàng trống, không thể đặt đơn.");
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            com.cyber.dao.IUserDAO userDAO = com.cyber.dao.impl.UserDAOImpl.getInstance();

            // 1. Kiểm tra user
            User currentUser = userDAO.findById(conn, userId);
            if (currentUser == null) {
                throw new BusinessException("ERR_USER_NOT_FOUND", "Không tìm thấy người dùng.");
            }
            if (currentUser.getStatus() == com.cyber.model.enums.UserStatus.LOCKED) {
                throw new BusinessException("ERR_USER_LOCKED", "Tài khoản đang bị khóa.");
            }

            // 2. Tính tổng từ cart items (giá đã tính qua Decorator + Strategy ở View)
            BigDecimal totalAmount = cartItems.stream()
                    .map(FbAdvancedCartItem::getFinalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (currentUser.getBalance().compareTo(totalAmount) < 0) {
                throw new BusinessException("ERR_INSUFFICIENT_BALANCE",
                        "Số dư không đủ. Cần: " + com.cyber.util.FormatUtils.formatVND(totalAmount)
                        + " | Hiện có: " + com.cyber.util.FormatUtils.formatVND(currentUser.getBalance()));
            }

            // 3. Trừ tiền
            userDAO.deductBalance(conn, userId, totalAmount);

            // 4. Tạo fb_order header
            FbOrder newOrder = new FbOrder(userId, bookingId, "PENDING", totalAmount);
            int newOrderId = orderDAO.createOrder(conn, newOrder);

            // 5. Lưu từng cart item + trừ stock
            for (FbAdvancedCartItem cartItem : cartItems) {
                menuItemDAO.deductStock(conn, cartItem.getMenuItemId(), cartItem.getQuantity());

                orderDetailDAO.saveOrderDetail(
                        conn,
                        newOrderId,
                        cartItem.getMenuItemId(),
                        cartItem.getQuantity(),
                        cartItem.getFinalPrice(),
                        cartItem.getItemDescription(),
                        cartItem.getItemConfigJson(),
                        cartItem.getDiscountApplied(),
                        cartItem.getDiscountStrategyName()
                );
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi lên đơn nâng cao: " + e.getMessage());
        } catch (BusinessException be) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw be;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    // -------------------------------------------------------
    // Inner DTO: Dữ liệu một dòng trong giỏ hàng nâng cao
    // -------------------------------------------------------

    /**
     * Data carrier từ View -> Service.
     * View build object này sau khi áp dụng Decorator + Strategy.
     * Service chỉ nhận và persist — không chứa logic tính giá.
     */
    public static class FbAdvancedCartItem {
        private final int        menuItemId;
        private final int        quantity;
        private final BigDecimal finalPrice;
        private final String     itemDescription;
        private final String     itemConfigJson;
        private final BigDecimal discountApplied;
        private final String     discountStrategyName;

        public FbAdvancedCartItem(int menuItemId, int quantity, BigDecimal finalPrice,
                                  String itemDescription, String itemConfigJson,
                                  BigDecimal discountApplied, String discountStrategyName) {
            this.menuItemId           = menuItemId;
            this.quantity             = quantity;
            this.finalPrice           = finalPrice;
            this.itemDescription      = itemDescription;
            this.itemConfigJson       = itemConfigJson;
            this.discountApplied      = discountApplied;
            this.discountStrategyName = discountStrategyName;
        }

        public int        getMenuItemId()           { return menuItemId; }
        public int        getQuantity()              { return quantity; }
        public BigDecimal getFinalPrice()            { return finalPrice; }
        public String     getItemDescription()       { return itemDescription; }
        public String     getItemConfigJson()        { return itemConfigJson; }
        public BigDecimal getDiscountApplied()       { return discountApplied; }
        public String     getDiscountStrategyName()  { return discountStrategyName; }
    }
}
