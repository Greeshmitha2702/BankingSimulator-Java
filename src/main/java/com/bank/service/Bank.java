package com.bank.service;

import com.bank.dao.Database;
import com.bank.model.Account;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Bank {
    private final TransactionService transactionService = new TransactionService();

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
                String phone=rs.getString("phone");
                accounts.add(new Account(accNo, holder,phone, balance));
            }

        } catch (SQLException e) {
            System.out.println("❌ Failed to load accounts: " + e.getMessage());
        }

        return accounts;
    }
    // ✅ Name should contain only alphabets and spaces
    public boolean isValidName(String name) {
        return name != null && name.matches("[A-Za-z ]+");
    }

    // ✅ Amount should be positive
    public boolean isPositive(double amount) {
        return amount > 0;
    }

    // ✅ Phone number must be exactly 10 digits
    public boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\d{10}");
    }


    // -----------------------------
    // Create new account
    // -----------------------------
    public void createAccount(String holderName,String phone, double initialDeposit) {
        if (!isValidName(holderName)) {
            System.out.println("❌ Invalid name. Only alphabets allowed.");
            return;
        }
        if (!isValidPhone(phone)) {
            System.out.println("❌ Invalid phone number. Must be 10 digits.");
            return;
        }
        if (!isPositive(initialDeposit)) {
            System.out.println("❌ Invalid amount. Must be greater than 0.");
            return;
        }

        String accountNumber = "ACC" + System.currentTimeMillis();
        System.out.println("✅ Your Account Number: " + accountNumber);

        String sql = "INSERT INTO accounts(accountNumber, accountHolder, phone, balance) VALUES(?,?,?,?)";


        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            pstmt.setString(2, holderName);
            pstmt.setString(3, phone);
            pstmt.setDouble(4, initialDeposit);

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
        if (!isPositive(amount)) {
            System.out.println("❌ Deposit amount must be greater than zero.");
            return;
        }

        String sqlUpdate = "UPDATE accounts SET balance = balance + ? WHERE accountNumber = ?";
        String sqlSelect = "SELECT balance FROM accounts WHERE accountNumber = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate);
             PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect)) {

            pstmtUpdate.setDouble(1, amount);
            pstmtUpdate.setString(2, accountNumber);
            int updated = pstmtUpdate.executeUpdate();

            if (updated > 0) {
                // Fetch updated balance
                pstmtSelect.setString(1, accountNumber);
                ResultSet rs = pstmtSelect.executeQuery();
                if (rs.next()) {
                    double newBalance = rs.getDouble("balance");
                    System.out.println("✅ Deposited ₹" + amount + " successfully!");
                    com.bank.dao.TransactionDAO.recordTransaction(conn, accountNumber, "deposit", amount, null);
                    System.out.println("💰 New Balance: ₹" + newBalance);
                }
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
        if (!isPositive(amount)) {
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

                // Fetch new balance
                pstmtSelect.setString(1, accountNumber);
                ResultSet rsNew = pstmtSelect.executeQuery();
                if (rsNew.next()) {
                    double newBalance = rsNew.getDouble("balance");
                    System.out.println("✅ Withdrew ₹" + amount + " successfully!");
                    com.bank.dao.TransactionDAO.recordTransaction(conn, accountNumber, "withdraw", amount, null);
                    System.out.println("💰 Remaining Balance: ₹" + newBalance);
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
    }

    public void transfer(String fromAccount, String toAccount, double amount) {
        if (!isPositive(amount)) {
            System.out.println("❌ Amount must be greater than zero.");
            return;
        }

        if (!accountExists(toAccount)) {
            System.out.println("❌ Target account not found!");
            return;
        }

        String sqlCheck = "SELECT balance FROM accounts WHERE accountNumber = ?";
        String sqlWithdraw = "UPDATE accounts SET balance = balance - ? WHERE accountNumber = ?";
        String sqlDeposit = "UPDATE accounts SET balance = balance + ? WHERE accountNumber = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(sqlCheck)) {

            checkStmt.setString(1, fromAccount);
            ResultSet rs = checkStmt.executeQuery();
            if (!rs.next()) {
                System.out.println("❌ Source account not found!");
                return;
            }

            double balance = rs.getDouble("balance");
            if (balance < amount) {
                System.out.println("❌ Insufficient balance for transfer!");
                return;
            }

            conn.setAutoCommit(false);
            try (PreparedStatement withdrawStmt = conn.prepareStatement(sqlWithdraw);
                 PreparedStatement depositStmt = conn.prepareStatement(sqlDeposit)) {

                withdrawStmt.setDouble(1, amount);
                withdrawStmt.setString(2, fromAccount);
                withdrawStmt.executeUpdate();

                depositStmt.setDouble(1, amount);
                depositStmt.setString(2, toAccount);
                depositStmt.executeUpdate();

                conn.commit();
                System.out.println("✅ Transferred ₹" + amount + " from " + fromAccount + " → " + toAccount);// Record transaction for the source account
                // Record transaction for sender (debit)
                com.bank.dao.TransactionDAO.recordTransaction(conn, fromAccount, "transfer", amount, toAccount);

                // Record transaction for receiver (credit)
                com.bank.dao.TransactionDAO.recordTransaction(conn, toAccount, "credit", amount, fromAccount);

            } catch (SQLException e) {
                conn.rollback();
                System.out.println("❌ Transfer failed: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
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
        String sql = "SELECT accountNumber, accountHolder FROM accounts";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- All Accounts ---");
            boolean hasAccounts = false;
            while (rs.next()) {
                hasAccounts = true;
                String accNo = rs.getString("accountNumber");
                String holder = rs.getString("accountHolder");
                System.out.println(holder + " (Account No: " + accNo+")");
            }

            if (!hasAccounts) {
                System.out.println("⚠️ No accounts to display.");
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
    }
    // ✅ Check if account exists in DB
    public boolean accountExists(String accountNumber) {
        String sql = "SELECT 1 FROM accounts WHERE accountNumber = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // true if account exists
        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
            return false;
        }
    }

}
