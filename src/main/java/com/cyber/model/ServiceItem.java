package com.cyber.model;

import com.cyber.model.enums.ServiceItemStatus;

import java.math.BigDecimal;

public class ServiceItem {
    private int itemId;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private ServiceItemStatus status;

    public ServiceItem() {}

    public ServiceItem(String name, String description, BigDecimal price, int stockQuantity, ServiceItemStatus status) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.status = status;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public ServiceItemStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceItemStatus status) {
        this.status = status;
    }
}