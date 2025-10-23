package com.bank.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:src/main/resources/db/bank.db?busy_timeout=5000";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.out.println("❌ Database connection failed: " + e.getMessage());
            return null;
        }
    }

    public static void createTableIfNotExists() {
        String createAccountsTableSQL = """
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                accountNumber TEXT NOT NULL UNIQUE,
                accountHolder TEXT NOT NULL,
                phone TEXT,
                balance REAL NOT NULL
            );
        """;

        String createUsersTableSQL = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                accountNumber TEXT,
                FOREIGN KEY (accountNumber) REFERENCES accounts(accountNumber)
            );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createAccountsTableSQL);
            stmt.execute(createUsersTableSQL);
            System.out.println("✅ Tables 'accounts' and 'users' ready in database.");

            // Create transactions table
            TransactionDAO.createTableIfNotExists();
        } catch (SQLException e) {
            System.out.println("❌ Failed to create tables: " + e.getMessage());
        }
    }
}
