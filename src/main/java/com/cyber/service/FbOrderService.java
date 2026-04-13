package com.cyber.service;

import com.cyber.connection.DatabaseConnection;
import com.cyber.dao.IFbMenuItemDAO;
import com.cyber.dao.IFbOrderDAO;
import com.cyber.dao.IFbOrderDetailDAO;
import com.cyber.dao.impl.FbMenuItemDAOImpl;
import com.cyber.dao.impl.FbOrderDAOImpl;
import com.cyber.dao.impl.FbOrderDetailDAOImpl;
import com.cyber.exception.BusinessException;
import com.cyber.model.FbOrder;
import com.cyber.model.User;
import com.cyber.model.enums.FbOrderStatus;
import com.cyber.model.enums.LogType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class FbOrderService {

    private static final FbOrderService INSTANCE = new FbOrderService();
    private final IFbOrderDAO       orderDAO;
    private final IFbMenuItemDAO    menuItemDAO;
    private final IFbOrderDetailDAO orderDetailDAO;
    private final LogService        logService;

    private FbOrderService() {
        this.orderDAO       = FbOrderDAOImpl.getInstance();
        this.menuItemDAO    = FbMenuItemDAOImpl.getInstance();
        this.orderDetailDAO = FbOrderDetailDAOImpl.getInstance();
        this.logService     = LogService.getInstance();
    }

    public static FbOrderService getInstance() {
        return INSTANCE;
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

    /**
     * Lấy TẤT CẢ đơn hàng của user (bao gồm DELIVERED, CANCELLED).
     */
    public List<FbOrder> getAllOrdersByUserId(int userId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return orderDAO.findAllOrdersByUserIdWithDetails(conn, userId);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy lịch sử đơn hàng: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getOrderDetails(int orderId) throws BusinessException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return orderDetailDAO.findDetailsByOrderId(conn, orderId);
        } catch (SQLException e) {
            throw new BusinessException("DB_ERROR", "Lỗi lấy chi tiết đơn: " + e.getMessage());
        }
    }

    public void updateOrderStatus(int orderId, FbOrderStatus newStatus,
                                   User actor) throws BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            FbOrder order = orderDAO.findOrderById(conn, orderId);
            if (order == null) {
                throw new BusinessException("NOT_FOUND", "Không tìm thấy đơn hàng ID=" + orderId);
            }
            if (order.getStatus() == FbOrderStatus.CANCELLED) {
                throw new BusinessException("INVALID_ACTION", "Đơn hàng đã hủy, không thể thay đổi trạng thái.");
            }

            orderDAO.updateOrderStatus(conn, orderId, newStatus, actor.getUserId());

            // Hoàn tiền + hoàn kho nếu staff huỷ đơn
            if (newStatus == FbOrderStatus.CANCELLED) {
                com.cyber.dao.IUserDAO userDAO = com.cyber.dao.impl.UserDAOImpl.getInstance();
                userDAO.addBalance(conn, order.getUserId(), order.getTotalAmount());
                
                // Hoàn kho từng món trong đơn
                List<java.util.Map<String, Object>> details = orderDetailDAO.findDetailsByOrderId(conn, orderId);
                for (java.util.Map<String, Object> detail : details) {
                    int menuItemId = (int) detail.get("menu_item_id");
                    int quantity = (int) detail.get("quantity");
                    menuItemDAO.addStock(conn, menuItemId, quantity);
                }
                
                String refundAction = String.format("Hoàn tiền + hoàn kho đơn hàng huỷ #%d: %s", 
                    orderId, com.cyber.util.FormatUtils.formatVND(order.getTotalAmount()));
                logService.log(conn, LogType.USER, actor, refundAction, order.getUserId());
            }

            // Ghi log trong cùng transaction
            String action = String.format("Đổi trạng thái Đơn #%d -> %s", orderId, newStatus.name());
            logService.log(conn, LogType.FB, actor, action, orderId);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("DB_ERROR", "Lỗi cập nhật Order: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {} }
        }
    }


    // -------------------------------------------------------
    // Customer: Đặt đồ ăn nâng cao (Phase 2)
    // Dùng fb_menu_items + Decorator + Strategy
    // Lưu vào fb_order_details với đầy đủ config JSON và discount info
    // -------------------------------------------------------

    /**
     * Đặt đơn hàng F&B đơn giản hóa.
     */
    public void orderFood(int userId, Integer bookingId,
                                  List<FbCartItem> cartItems) throws BusinessException {
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

            // 2. Tính tổng kết từ giỏ hàng (Sau khi trừ giảm giá)
            BigDecimal totalAmount = cartItems.stream()
                    .map(FbCartItem::getFinalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalDiscount = cartItems.stream()
                    .map(i -> i.getDiscountApplied() != null ? i.getDiscountApplied() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            totalAmount = totalAmount.subtract(totalDiscount);

            if (currentUser.getBalance().compareTo(totalAmount) < 0) {
                throw new BusinessException("ERR_INSUFFICIENT_BALANCE",
                        "Số dư không đủ. Cần: " + com.cyber.util.FormatUtils.formatVND(totalAmount)
                        + " | Hiện có: " + com.cyber.util.FormatUtils.formatVND(currentUser.getBalance()));
            }

            // 3. Trừ tiền
            userDAO.deductBalance(conn, userId, totalAmount);

            // 4. Tạo fb_order header
            FbOrder newOrder = new FbOrder(userId, bookingId, FbOrderStatus.PENDING, totalAmount);
            int newOrderId = orderDAO.createOrder(conn, newOrder);

            // 5. Kiểm tra tính hợp lệ & Lưu từng cart item + trừ stock
            for (FbCartItem cartItem : cartItems) {
                com.cyber.domain.fb.FbMenuItem dbItem = menuItemDAO.findById(conn, cartItem.getMenuItemId());
                if (dbItem == null || dbItem.isDeleted()) {
                    throw new BusinessException("ITEM_INVALID", "Món ăn ID=" + cartItem.getMenuItemId() + " không tồn tại hoặc đã bị xóa.");
                }

                menuItemDAO.deductStock(conn, cartItem.getMenuItemId(), cartItem.getQuantity());

                orderDetailDAO.insertOrderDetail(
                        conn,
                        newOrderId,
                        cartItem.getMenuItemId(),
                        cartItem.getQuantity(),
                        cartItem.getFinalPrice(),       // Snapshot giá
                        dbItem.getName(),               // Snapshot tên món gốc
                        cartItem.getItemDescription(),
                        cartItem.getItemConfigJson(),
                        cartItem.getDiscountApplied(),
                        cartItem.getDiscountStrategyName()
                );
            }

            // 6. Ghi log FB trong cùng transaction
            String fbAction = String.format("Đặt đồ ăn đơn #%d (%d món) | Tổng: %s",
                    newOrderId,
                    cartItems.size(),
                    com.cyber.util.FormatUtils.formatVND(totalAmount));
            logService.log(conn, LogType.FB, currentUser, fbAction, newOrderId);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new BusinessException("DB_ERROR", "Lỗi CSDL khi đặt hàng: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * DTO: Giỏ hàng F&B
     */
    public static class FbCartItem {
        private final int        menuItemId;
        private final int        quantity;
        private final BigDecimal finalPrice;
        private final String     itemDescription;
        private final String     itemConfigJson;
        private BigDecimal       discountApplied;
        private String           discountStrategyName;

        public FbCartItem(int menuItemId, int quantity, BigDecimal finalPrice,
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

        public void setDiscountApplied(BigDecimal discountApplied) {
            this.discountApplied = discountApplied;
        }
        public void setDiscountStrategyName(String discountStrategyName) {
            this.discountStrategyName = discountStrategyName;
        }
    }
}
