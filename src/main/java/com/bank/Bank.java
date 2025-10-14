package com.bank;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Bank {

    // -----------------------------
    // Load all accounts from database
    // -----------------------------
    public List<Account> getAllAccountsFromDB() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT accountNumber, accountHolder, balance FROM accounts";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String accNo = rs.getString("accountNumber");
                String holder = rs.getString("accountHolder");
                double balance = rs.getDouble("balance");
                accounts.add(new Account(accNo, holder, balance));
            }

        } catch (SQLException e) {
            System.out.println("❌ Failed to load accounts: " + e.getMessage());
        }

        return accounts;
    }

    // -----------------------------
    // Create new account
    // -----------------------------
    public void createAccount(String accountNumber, String holderName, double initialDeposit) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            System.out.println("❌ Invalid account number.");
            return;
        }
        if (holderName == null || holderName.isEmpty()) {
            System.out.println("❌ Invalid holder name.");
            return;
        }
        if (initialDeposit <= 0) {
            System.out.println("❌ Initial deposit must be greater than zero.");
            return;
        }

        String sql = "INSERT INTO accounts(accountNumber, accountHolder, balance) VALUES(?,?,?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            pstmt.setString(2, holderName);
            pstmt.setDouble(3, initialDeposit);
            pstmt.executeUpdate();

            System.out.println("✅ Account created successfully for " + holderName + "!");

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
    }

    // -----------------------------
    // Deposit money
    // -----------------------------
    public void deposit(String accountNumber, double amount) {
        if (amount <= 0) {
            System.out.println("❌ Deposit amount must be greater than zero.");
            return;
        }

        String sql = "UPDATE accounts SET balance = balance + ? WHERE accountNumber = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amount);
            pstmt.setString(2, accountNumber);
            int updated = pstmt.executeUpdate();

            if (updated > 0) {
                System.out.println("✅ Deposited ₹" + amount + " successfully!");
            } else {
                System.out.println("❌ Account not found!");
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
    }

    // -----------------------------
    // Withdraw money
    // -----------------------------
    public void withdraw(String accountNumber, double amount) {
        if (amount <= 0) {
            System.out.println("❌ Withdrawal amount must be greater than zero.");
            return;
        }

        String sqlSelect = "SELECT balance FROM accounts WHERE accountNumber = ?";
        String sqlUpdate = "UPDATE accounts SET balance = balance - ? WHERE accountNumber = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect)) {

            pstmtSelect.setString(1, accountNumber);
            ResultSet rs = pstmtSelect.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");
                if (amount > balance) {
                    System.out.println("❌ Insufficient balance!");
                    return;
                }
            } else {
                System.out.println("❌ Account not found!");
                return;
            }

            try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate)) {
                pstmtUpdate.setDouble(1, amount);
                pstmtUpdate.setString(2, accountNumber);
                pstmtUpdate.executeUpdate();
                System.out.println("✅ Withdrew ₹" + amount + " successfully!");
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
    }

    // -----------------------------
    // Check balance
    // -----------------------------
    public void checkBalance(String accountNumber) {
        String sql = "SELECT accountHolder, balance FROM accounts WHERE accountNumber = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String holder = rs.getString("accountHolder");
                double balance = rs.getDouble("balance");
                System.out.println("💰 Balance for " + holder + ": ₹" + balance);
            } else {
                System.out.println("❌ Account not found!");
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
    }

    // -----------------------------
    // Display all accounts
    // -----------------------------
    public void displayAllAccounts() {
        String sql = "SELECT accountNumber, accountHolder, balance FROM accounts";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- All Accounts ---");
            boolean hasAccounts = false;
            while (rs.next()) {
                hasAccounts = true;
                String accNo = rs.getString("accountNumber");
                String holder = rs.getString("accountHolder");
                double balance = rs.getDouble("balance");
                System.out.println(holder + " (Account No: " + accNo + ", Balance: ₹" + balance + ")");
            }

            if (!hasAccounts) {
                System.out.println("⚠️ No accounts to display.");
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
    }
}
