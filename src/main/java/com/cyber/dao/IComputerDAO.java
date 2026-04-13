package com.cyber.dao;

import com.cyber.model.Computer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface IComputerDAO {
    List<Computer> getAllActiveComputers(Connection conn) throws SQLException;

    List<Computer> getAllComputersForAdmin(Connection conn) throws SQLException;

    Computer findById(Connection conn, int computerId) throws SQLException;

    boolean checkNameExists(Connection conn, String name) throws SQLException;

    void addComputer(Connection conn, Computer computer) throws SQLException;

    void updateComputer(Connection conn, Computer computer) throws SQLException;

    void deleteComputer(Connection conn, int computerId) throws SQLException;
}
