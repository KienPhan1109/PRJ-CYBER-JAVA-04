package com.cyber.dao;

import com.cyber.domain.fb.FbMenuItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface IFbMenuItemDAO {
    List<FbMenuItem> getAllActiveItems(Connection conn) throws SQLException;

    List<FbMenuItem> getAllItemsForAdmin(Connection conn) throws SQLException;

    FbMenuItem findById(Connection conn, int menuItemId) throws SQLException;

    List<FbMenuItem> findByCategoryId(Connection conn, int categoryId) throws SQLException;

    int create(Connection conn, FbMenuItem item) throws SQLException;

    void update(Connection conn, FbMenuItem item) throws SQLException;

    void deleteItem(Connection conn, int menuItemId) throws SQLException;

    void deductStock(Connection conn, int menuItemId, int quantity) throws SQLException;

    void addStock(Connection conn, int menuItemId, int quantity) throws SQLException;

    FbMenuItem findByName(Connection conn, String name) throws SQLException;
}
