package com.cyber.domain.fb;
import com.cyber.model.enums.FBStatus;
import com.cyber.model.enums.FbTemperature;
import java.math.BigDecimal;

/**
 * POJO Entity ánh xạ bảng fb_menu_items trong database.
 * Đây là đối tượng "raw data" được load từ DAO, sẽ được bọc vào SingleItem.
 */
public class FbMenuItem {

    private int    menuItemId;
    private int    categoryId;
    private String categoryName;        // Joined thêm để hiển thị
    private String name;
    private String description;
    private BigDecimal basePrice;
    private int    stockQuantity;
    private int    prepTimeInMinutes;   // Thời gian chuẩn bị (phút)
    private String itemTags;            // "Spicy,Vegan,BestSeller"
    private String availability;        // "ALL" hoặc "06:00-22:00"
    private FbTemperature temperatureLevel;    // HOT / COLD / ICED / NONE
    private FBStatus status;              // ACTIVE / OUT_OF_STOCK / HIDDEN

    public FbMenuItem() {}

    public FbMenuItem(int menuItemId, int categoryId, String name,
                      String description, BigDecimal basePrice,
                      int stockQuantity, int prepTimeInMinutes,
                      String itemTags, String availability,
                      FbTemperature temperatureLevel, FBStatus status) {
        this.menuItemId       = menuItemId;
        this.categoryId       = categoryId;
        this.name             = name;
        this.description      = description;
        this.basePrice        = basePrice;
        this.stockQuantity    = stockQuantity;
        this.prepTimeInMinutes = prepTimeInMinutes;
        this.itemTags         = itemTags;
        this.availability     = availability;
        this.temperatureLevel = temperatureLevel;
        this.status           = status;
    }

    // -------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------
    public int getMenuItemId() { return menuItemId; }
    public void setMenuItemId(int menuItemId) { this.menuItemId = menuItemId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public int getPrepTimeInMinutes() { return prepTimeInMinutes; }
    public void setPrepTimeInMinutes(int prepTimeInMinutes) { this.prepTimeInMinutes = prepTimeInMinutes; }

    public String getItemTags() { return itemTags; }
    public void setItemTags(String itemTags) { this.itemTags = itemTags; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public FbTemperature getTemperatureLevel() { return temperatureLevel; }
    public void setTemperatureLevel(FbTemperature temperatureLevel) { this.temperatureLevel = temperatureLevel; }

    public FBStatus getStatus() { return status; }
    public void setStatus(FBStatus status) { this.status = status; }
}
