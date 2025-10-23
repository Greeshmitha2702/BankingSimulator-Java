package com.bank.dao;

import java.sql.Connection;
import java.sql.Statement;

public class CreateTable {
    public static void main(String[] args) {
        // Correct SQL for accounts table
        String createAccountsTableSQL = """
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                accountNumber TEXT NOT NULL UNIQUE,
                accountHolder TEXT NOT NULL,
                phone TEXT NOT NULL,
                balance REAL NOT NULL
            );
        """;

        // Correct SQL for users table with foreign key
        String createUsersTableSQL = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                accountNumber TEXT,
                FOREIGN KEY (accountNumber) REFERENCES accounts(accountNumber)
            );
        """;

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createAccountsTableSQL);
            stmt.execute(createUsersTableSQL);

            System.out.println("✅ Tables 'accounts' and 'users' ready in database.");

        } catch (Exception e) {
            System.out.println("❌ Error creating tables: " + e.getMessage());
        }
    }
}
