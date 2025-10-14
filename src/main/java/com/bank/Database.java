package com.bank;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:src/main/resources/db/bank.db";

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to SQLite database!");
            return conn;
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
            return null;
        }
    }
}
