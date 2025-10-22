package com.bank;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:src/main/resources/db/bank.db";

    // ✅ Static method for uniform access
    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL);
            return conn;
        } catch (SQLException e) {
            System.out.println("❌ Database connection failed: " + e.getMessage());
            return null;
        }
    }

    // ✅ Create accounts table if not exists
    public static void createTableIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS accounts (
                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                          accountNumber TEXT NOT NULL UNIQUE,
                          accountHolder TEXT NOT NULL,
                          phone TEXT NOT NULL,
                          balance REAL NOT NULL
                      );
                
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("✅ Table 'accounts' ready in database.");
        } catch (SQLException e) {
            System.out.println("❌ Failed to create table: " + e.getMessage());
        }
    }
}
