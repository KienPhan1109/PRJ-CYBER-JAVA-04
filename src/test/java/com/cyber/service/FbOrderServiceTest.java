package com.cyber.service;

import com.cyber.domain.fb.FbMenuItem;
import com.cyber.domain.fb.discount.*;
import com.cyber.model.enums.FBStatus;
import com.cyber.model.enums.FbTemperature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class FbOrderServiceTest {

    private FbMenuItem sampleFood;
    private FbMenuItem sampleDrink;

    @BeforeEach
    public void setup() {
        // Tạo dữ liệu mốc (Không cần chạm Database)
        sampleFood = new FbMenuItem(1, 1, "Mì Xào Bò", "Mì xào thơm ngon", new BigDecimal("45000"), 10, 5, "ALL", FbTemperature.NONE, FBStatus.ACTIVE, false);
        sampleDrink = new FbMenuItem(2, 2, "Trà Sữa", "Trà sữa truyền thống", new BigDecimal("30000"), 50, 2, "ALL", FbTemperature.COLD, FBStatus.ACTIVE, false);
    }

    @Test
    @DisplayName("Kiểm tra tính giá khi cấu trúc giỏ hàng theo mô hình phẳng Flat Item")
    public void testFlatOrderCalculation() {
        // Cấu hình: Trà Sữa (30k) + Trân châu đen (7k)
        // Trong mô hình phẳng, giỏ hàng (Cart) lưu trữ danh sách các FbCartItem
        FbOrderService.FbCartItem drinkItem = new FbOrderService.FbCartItem(
            sampleDrink.getMenuItemId(), 
            1, 
            sampleDrink.getBasePrice(), 
            sampleDrink.getName(), 
            "{}", 
            BigDecimal.ZERO, 
            ""
        );
        
        FbOrderService.FbCartItem toppingItem = new FbOrderService.FbCartItem(
            3, // ID giả mạo cho Topping
            1, 
            new BigDecimal("7000"), 
            "Trân châu đen", 
            "{}", 
            BigDecimal.ZERO, 
            ""
        );

        // Tổng tiền = Base Price của Drink + Base Price của Topping = 37,000 VNĐ
        BigDecimal expectedPrice = new BigDecimal("37000");
        BigDecimal calculatedTotal = drinkItem.getFinalPrice().multiply(new BigDecimal(drinkItem.getQuantity()))
                                     .add(toppingItem.getFinalPrice().multiply(new BigDecimal(toppingItem.getQuantity())));
        
        assertEquals(expectedPrice, calculatedTotal, "Trường hợp tính sai tổng tiền mô hình Phẳng!");
    }


    @Test
    @DisplayName("Kiểm tra các chiến lược giảm giá (Strategy Pattern)")
    public void testDiscountStrategies() {
        BigDecimal basePrice = new BigDecimal("100000"); // Đơn hàng 100,000 VNĐ

        // 1. Không giảm giá
        IDiscountStrategy noDiscount = NoDiscountStrategy.getInstance();
        assertEquals(basePrice, noDiscount.applyDiscount(basePrice));
        assertEquals(BigDecimal.ZERO, noDiscount.calculateDiscountAmount(basePrice));

        // 2. Giảm 10% (VIP)
        IDiscountStrategy vipDiscount = new PercentageDiscountStrategy(10, "VIP");
        BigDecimal expectedVipTotal = new BigDecimal("90000.00");
        BigDecimal expectedVipDiscount = new BigDecimal("10000.00");
        
        assertEquals(expectedVipTotal, vipDiscount.applyDiscount(basePrice).setScale(2, java.math.RoundingMode.HALF_UP));
        assertEquals(expectedVipDiscount, vipDiscount.calculateDiscountAmount(basePrice).setScale(2, java.math.RoundingMode.HALF_UP));

        // 3. Giảm cố định 25,000 VNĐ (Voucher)
        IDiscountStrategy voucherDiscount = new FixedAmountDiscountStrategy(new BigDecimal("25000"), "VOUCHER_25K");
        BigDecimal expectedVoucherTotal = new BigDecimal("75000");
        
        assertEquals(expectedVoucherTotal, voucherDiscount.applyDiscount(basePrice));
        assertEquals(new BigDecimal("25000"), voucherDiscount.calculateDiscountAmount(basePrice));
    }
}
