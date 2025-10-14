package com.bank;

import java.sql.Connection;
import java.sql.Statement;

public class CreateTable {
    public static void main(String[] args) {
        String sql = "CREATE TABLE IF NOT EXISTS accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "accountNumber TEXT NOT NULL UNIQUE," +
                "name TEXT NOT NULL," +
                "balance REAL NOT NULL)";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("✅ Table created successfully!");

        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }
}
