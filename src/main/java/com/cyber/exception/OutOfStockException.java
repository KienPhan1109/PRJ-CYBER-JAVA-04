package com.cyber.exception;

class OutOfStockException extends BusinessException {
    public OutOfStockException(String itemName, int availableStock) { super("ERR_OUT_OF_STOCK", "Món '" + itemName + "' đã hết hàng hoặc không đủ. Tồn kho: " + availableStock); }
}