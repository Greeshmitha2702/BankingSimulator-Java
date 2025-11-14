package com.bank.dao;

import java.sql.Connection;
import java.sql.Statement;

public class CreateTable {
    public static void main(String[] args) {

        String dropTransactions = "DROP TABLE IF EXISTS transactions;";
        String dropUsers = "DROP TABLE IF EXISTS users;";
        String dropAccounts = "DROP TABLE IF EXISTS accounts;";

        String createAccountsTable = """
    CREATE TABLE IF NOT EXISTS accounts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        accountNumber TEXT NOT NULL UNIQUE,
        accountHolder TEXT NOT NULL,
        phone TEXT NOT NULL,
        email TEXT,
        balance REAL NOT NULL,
        locked INTEGER DEFAULT 0,
        alertThreshold REAL DEFAULT 0
    );
""";


        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                email TEXT,
                accountNumber TEXT UNIQUE,
                failed_attempts INTEGER DEFAULT 0,
                locked INTEGER DEFAULT 0,
                FOREIGN KEY (accountNumber)
                    REFERENCES accounts(accountNumber)
                    ON DELETE CASCADE
            );
        """;

        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                accountNumber TEXT NOT NULL,
                type TEXT NOT NULL,
                amount REAL NOT NULL,
                targetAccount TEXT,
                timestamp DATETIME DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (accountNumber)
                    REFERENCES accounts(accountNumber)
                    ON DELETE CASCADE,
                FOREIGN KEY (targetAccount)
                    REFERENCES accounts(accountNumber)
                    ON DELETE SET NULL
            );
        """;

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // enable FK before creating
            stmt.execute("PRAGMA foreign_keys = ON;");

            stmt.execute(dropTransactions);
            stmt.execute(dropUsers);
            stmt.execute(dropAccounts);

            stmt.execute(createAccountsTable);
            stmt.execute(createUsersTable);
            stmt.execute(createTransactionsTable);

            System.out.println("✅ All tables recreated successfully with proper foreign keys.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error while recreating tables: " + e.getMessage());
        }
    }
}
