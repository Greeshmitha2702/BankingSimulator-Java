package com.bank.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class TransactionDAO {

    private static final Logger logger = LoggerFactory.getLogger(TransactionDAO.class);

    public static void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                accountNumber TEXT NOT NULL,
                type TEXT NOT NULL,
                amount REAL NOT NULL,
                targetAccount TEXT,
                timestamp DATETIME DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (accountNumber) REFERENCES accounts(accountNumber)
            );
        """;

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Transactions table ready.");
            logger.info("Transactions table verified/created successfully.");
        } catch (SQLException e) {
            System.out.println("❌ Error creating transactions table: " + e.getMessage());
            logger.error("Failed to create transactions table", e);
        }
    }

    public static void recordTransaction(Connection conn, String accountNumber, String type, double amount, String targetAccount) {
        String sql = "INSERT INTO transactions(accountNumber, type, amount, targetAccount) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setString(4, targetAccount);
            ps.executeUpdate();

            logger.info("Transaction recorded: {} ₹{} (Target: {})", type, amount, targetAccount);
        } catch (SQLException e) {
            System.out.println("⚠️ Error recording transaction: " + e.getMessage());
            logger.error("Failed to record transaction for account {}", accountNumber, e);
        }
    }
}
