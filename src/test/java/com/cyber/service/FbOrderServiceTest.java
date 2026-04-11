package com.cyber.service;

import com.cyber.domain.fb.*;
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
    @DisplayName("Kiểm tra tính giá khi lồng ghép nhiều Decorator (Size + Topping)")
    public void testOrderWithMultipleDecorators() {
        IBillable drink = new SingleItem(sampleDrink);
        
        // Cấu hình: Trà Sữa Size L (+10,000) + Trân châu (+7,000) + Thạch cà phê (+6,000)
        drink = new SizeDecorator(drink, SizeDecorator.SizeType.L);
        drink = new ToppingDecorator(drink, "Trân châu đen", new BigDecimal("7000"));
        drink = new ToppingDecorator(drink, "Thạch cà phê", new BigDecimal("6000"));
        
        // Tính tổng tiền mong đợi: 30000 + 10000 + 7000 + 6000 = 53000
        BigDecimal expectedPrice = new BigDecimal("53000");
        assertEquals(expectedPrice, drink.calculatePrice(), "Trường hợp tính sai tổng tiền Decorator!");
        
        // Kiểm tra Description
        String desc = drink.getDescription();
        assertTrue(desc.contains("Size L"), "Thiếu mô tả Size L");
        assertTrue(desc.contains("Trân châu đen"), "Thiếu mô tả Topping 1");
        assertTrue(desc.contains("Thạch cà phê"), "Thiếu mô tả Topping 2");
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
