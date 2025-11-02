package com.bank.service;

import com.bank.dao.Database;
import com.bank.model.Account;
import com.bank.dao.TransactionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bank.report.ReportGenerator;

import java.sql.*;
import java.util.*;

public class Bank {
    private static final Scanner sc = new Scanner(System.in);
    private static final Logger logger = LoggerFactory.getLogger(Bank.class);
    private final TransactionService transactionService = new TransactionService();
    private final AuthService authService = new AuthService();

    // -----------------------------
    // Load all accounts from database
    // -----------------------------
    public List<Account> getAllAccountsFromDB() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT accountNumber, accountHolder, phone, balance FROM accounts";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String accNo = rs.getString("accountNumber");
                String holder = rs.getString("accountHolder");
                String phone = rs.getString("phone");
                double balance = rs.getDouble("balance");
                accounts.add(new Account(accNo, holder, phone, balance));
            }

            logger.info("Loaded {} accounts from database", accounts.size());

        } catch (SQLException e) {
            System.out.println("❌ Failed to load accounts: " + e.getMessage());
            logger.error("Error loading accounts from DB", e);
        }

        return accounts;
    }

    // ✅ Validation helpers
    public boolean isValidName(String name) {
        return name != null && name.matches("[A-Za-z ]+");
    }

    public boolean isPositive(double amount) {
        return amount > 0;
    }
    // ✅ Email validation
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    public boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\d{10}");
    }

    // -----------------------------
    // Create new account
    // -----------------------------
    public void createAccount(String holderName, String phone, double initialDeposit, String email) {
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
        String sql = "INSERT INTO accounts(accountNumber, accountHolder, phone, balance, email, locked, alertThreshold) VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            pstmt.setString(2, holderName);
            pstmt.setString(3, phone);
            pstmt.setDouble(4, initialDeposit);
            pstmt.setString(5, email);
            pstmt.setInt(6, 0); // not locked
            pstmt.setDouble(7, 1000.0); // default threshold

            pstmt.executeUpdate();

            System.out.println("\n✅ Account created successfully for " + holderName + "!");
            System.out.println("💳 Your Account Number: " + accountNumber);
            logger.info("New account created: {} ({}) with initial deposit ₹{} and email {}", accountNumber, holderName, initialDeposit, email);

            // Send a welcome email (optional)
            try {
                if (email != null && !email.isEmpty()) {
                    EmailService.sendEmail(
                            email,
                            "Welcome to Banking Simulator 🎉",
                            "Hello " + holderName + ",\n\n" +
                                    "Your new account (" + accountNumber + ") has been successfully created with an initial balance of ₹" + initialDeposit + ".\n\n" +
                                    "Thank you for choosing our Banking Simulator!\n\n" +
                                    "— Banking Simulator Team"
                    );
                    logger.info("📨 Welcome email sent to {}", email);
                }
            } catch (Exception e) {
                logger.error("❌ Failed to send welcome email to {}", email, e);
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
            logger.error("Error creating account for {}", holderName, e);
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
                pstmtSelect.setString(1, accountNumber);
                ResultSet rs = pstmtSelect.executeQuery();
                if (rs.next()) {
                    double newBalance = rs.getDouble("balance");
                    System.out.println("✅ Deposited ₹" + amount + " successfully!");
                    TransactionDAO.recordTransaction(conn, accountNumber, "deposit", amount, null);
                    System.out.println("💰 New Balance: ₹" + newBalance);
                    logger.info("Deposit ₹{} to account {}. New balance: ₹{}", amount, accountNumber, newBalance);
                }
            } else {
                System.out.println("❌ Account not found!");
                logger.warn("Deposit failed — account {} not found", accountNumber);
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
            logger.error("Error during deposit for account {}", accountNumber, e);
        }
    }

    // -----------------------------
    // Withdraw money (requires login password confirmation)
    // -----------------------------
    public void withdraw(String accountNumber, double amount, String username) {
        if (!isPositive(amount)) {
            System.out.println("❌ Withdrawal amount must be greater than zero.");
            return;
        }

        // confirm with login password
        if (!verifyPasswordPrompt(username, accountNumber)) {
            return;
        }

        String sqlSelect = "SELECT balance FROM accounts WHERE accountNumber = ?";
        String sqlUpdate = "UPDATE accounts SET balance = balance - ? WHERE accountNumber = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect)) {

            pstmtSelect.setString(1, accountNumber);
            ResultSet rs = pstmtSelect.executeQuery();

            if (!rs.next()) {
                System.out.println("❌ Account not found!");
                logger.warn("Withdrawal failed — account {} not found", accountNumber);
                return;
            }

            double balance = rs.getDouble("balance");
            if (amount > balance) {
                System.out.println("❌ Insufficient balance!");
                logger.warn("Withdrawal failed — insufficient balance in account {}", accountNumber);
                return;
            }

            try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate)) {
                pstmtUpdate.setDouble(1, amount);
                pstmtUpdate.setString(2, accountNumber);
                pstmtUpdate.executeUpdate();

                TransactionDAO.recordTransaction(conn, accountNumber, "withdraw", amount, null);
                System.out.println("✅ Withdrew ₹" + amount + " successfully!");
                System.out.println("💰 Remaining Balance: ₹" + (balance - amount));
                logger.info("Withdrawal ₹{} from account {}. Remaining: ₹{}", amount, accountNumber, (balance - amount));
                checkAndSendLowBalanceAlert(conn, accountNumber);
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
            logger.error("Error during withdrawal for account {}", accountNumber, e);
        }
    }

    // -----------------------------
    // Transfer money
    // -----------------------------
    public void transfer(String fromAccount, String toAccount, double amount) {
        if (!isPositive(amount)) {
            System.out.println("❌ Amount must be greater than zero.");
            return;
        }
        if (!accountExists(toAccount)) {
            System.out.println("❌ Target account not found!");
            logger.warn("Transfer failed — target account {} not found", toAccount);
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
                logger.warn("Transfer failed — source account {} not found", fromAccount);
                return;
            }

            double balance = rs.getDouble("balance");
            if (balance < amount) {
                System.out.println("❌ Insufficient balance for transfer!");
                logger.warn("Transfer failed — insufficient balance in {}", fromAccount);
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

                TransactionDAO.recordTransaction(conn, fromAccount, "transfer", amount, toAccount);
                TransactionDAO.recordTransaction(conn, toAccount, "credit", amount, fromAccount);

                conn.commit();

                System.out.println("✅ Transferred ₹" + amount + " from " + fromAccount + " → " + toAccount);
                logger.info("Transfer ₹{} from {} to {}", amount, fromAccount, toAccount);
                checkAndSendLowBalanceAlert(conn, fromAccount);

            } catch (SQLException e) {
                conn.rollback();
                System.out.println("❌ Transfer failed: " + e.getMessage());
                logger.error("Transfer rollback — {}", e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
            logger.error("Error during transfer between {} and {}", fromAccount, toAccount, e);
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
                logger.info("Checked balance for {} ({}): ₹{}", holder, accountNumber, balance);
                checkAndSendLowBalanceAlert(conn, accountNumber);
            } else {
                System.out.println("❌ Account not found!");
                logger.warn("Balance check failed — account {} not found", accountNumber);
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
            logger.error("Error checking balance for account {}", accountNumber, e);
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
                System.out.println(holder + " (Account No: " + accNo + ")");
            }

            if (!hasAccounts) {
                System.out.println("⚠️ No accounts to display.");
            }

            logger.info("Displayed all accounts.");

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
            logger.error("Error displaying all accounts", e);
        }
    }

    // -----------------------------
    // Utility
    // -----------------------------
    public boolean accountExists(String accountNumber) {
        String sql = "SELECT 1 FROM accounts WHERE accountNumber = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
            logger.error("Error checking existence of account {}", accountNumber, e);
            return false;
        }
    }

    // -----------------------------
    // Generate Transaction Report (PDF)
    // -----------------------------
    public void generateReport(String accountNumber) {
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT accountNumber, accountHolder, phone, balance FROM accounts WHERE accountNumber = ?";
            Account account = null;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, accountNumber);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    account = new Account(
                            rs.getString("accountNumber"),
                            rs.getString("accountHolder"),
                            rs.getString("phone"),
                            rs.getDouble("balance")
                    );
                }
            }

            if (account == null) {
                System.out.println("❌ Account not found for number: " + accountNumber);
                logger.warn("Report generation failed — account {} not found", accountNumber);
                return;
            }

            List<String[]> transactions = TransactionDAO.getTransactionsByAccount(accountNumber);

            if (transactions.isEmpty()) {
                System.out.println("⚠️ No transactions found for this account.");
                logger.info("No transactions found for report generation (account: {})", accountNumber);
                return;
            }

            ReportGenerator.generatePDFReport(account, transactions);

            String emailQuery = "SELECT email FROM accounts WHERE accountNumber = ?";
            String email = null;
            try (PreparedStatement ps = conn.prepareStatement(emailQuery)) {
                ps.setString(1, accountNumber);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    email = rs.getString("email");
                }
            }

            if (email != null && !email.isEmpty()) {
                String pdfPath = "reports/report_" + account.getAccountNumber() + ".pdf";
                EmailService.sendEmailWithAttachment(
                        email,
                        "Your Account Report 📊",
                        "Hello " + account.getAccountHolder() + ",\n\nAttached is your latest banking report.\n\n— Banking Simulator Team",
                        pdfPath
                );
                logger.info("📧 Report emailed to {}", email);
            } else {
                System.out.println("⚠️ No email linked to this account. Report saved locally only.");
            }

        } catch (Exception e) {
            System.out.println("❌ Error generating report: " + e.getMessage());
            logger.error("Error generating report for account {}", accountNumber, e);
        }
    }

    // -----------------------------
    // Low Balance Alert Helper
    // -----------------------------
    private void checkAndSendLowBalanceAlert(Connection conn, String accountNumber) {
        String sql = "SELECT accountHolder, email, balance, alertThreshold FROM accounts WHERE accountNumber = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String holder = rs.getString("accountHolder");
                String email = rs.getString("email");
                double balance = rs.getDouble("balance");
                double threshold = rs.getDouble("alertThreshold");

                if (balance < threshold && email != null && !email.isEmpty()) {
                    String subject = "⚠️ Low Balance Alert: Your Account " + accountNumber;
                    String body = "Dear " + holder + ",\n\n" +
                            "Your current account balance is ₹" + balance + ", which is below your set threshold of ₹" + threshold + ".\n" +
                            "Please deposit funds to avoid service interruptions.\n\n" +
                            "— Banking Simulator Team";

                    EmailService.sendEmail(email, subject, body);
                    logger.info("📧 Low balance alert sent to {} for account {} (₹{})", email, accountNumber, balance);
                }
            }
        } catch (Exception e) {
            logger.error("❌ Failed to send low balance alert for {}", accountNumber, e);
        }
    }

    // -----------------------------
    // Set Alert Threshold
    // -----------------------------
    public void setAlertThreshold(String accountNumber, double newThreshold) {
        String sql = "UPDATE accounts SET alertThreshold = ? WHERE accountNumber = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newThreshold);
            ps.setString(2, accountNumber);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Alert threshold updated to ₹" + newThreshold);
                logger.info("Alert threshold updated to ₹{} for account {}", newThreshold, accountNumber);
            } else {
                System.out.println("❌ Account not found or update failed.");
            }
        } catch (Exception e) {
            logger.error("❌ Failed to update alert threshold for {}", accountNumber, e);
        }
    }

    // ----------------------------
    // Password confirmation prompt (used for withdraw/update/delete)
    // ----------------------------
    private boolean verifyPasswordPrompt(String username, String accountNumber) {
        int attempts = 0;
        while (attempts < 3) {
            System.out.print("🔑 Enter your login password to confirm: ");
            String enteredPassword = sc.nextLine();

            boolean ok = authService.verifyPassword(username, enteredPassword);
            if (ok) return true;

            attempts++;
            System.out.println("❌ Incorrect password (" + attempts + "/3)");
        }

        // lock both user and account after 3 failed attempts
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps1 = conn.prepareStatement("UPDATE accounts SET locked = 1 WHERE accountNumber = ?")) {
                ps1.setString(1, accountNumber);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement("UPDATE users SET locked = 1 WHERE accountNumber = ?")) {
                ps2.setString(1, accountNumber);
                ps2.executeUpdate();
            }
            System.out.println("🔒 Account locked due to 3 incorrect attempts! Use Forgot Password to reset.");
            logger.warn("Account locked: {}", accountNumber);
        } catch (SQLException e) {
            logger.error("Error locking account {}", accountNumber, e);
        }
        return false;
    }

    // ----------------------------
    // Update Account Details (requires login password confirmation)
    // ----------------------------
    public void updateAccountDetails(String accNo, String currentUsername) {
        try (Connection conn = Database.getConnection()) {

            // Fetch current details
            String fetchQuery = "SELECT accountHolder, phone, email FROM accounts WHERE accountNumber = ?";
            String currentName = "", currentPhone = "", currentEmail = "";

            try (PreparedStatement ps = conn.prepareStatement(fetchQuery)) {
                ps.setString(1, accNo);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    currentName = rs.getString("accountHolder");
                    currentPhone = rs.getString("phone");
                    currentEmail = rs.getString("email");
                } else {
                    logger.error("❌ No account found for account number: {}", accNo);
                    return;
                }
            }

            logger.info("✏️ Updating details for account: {}", accNo);

            System.out.print("👤 New Name (press Enter to skip): ");
            String newName = sc.nextLine().trim();
            if (newName.isEmpty()) newName = currentName;

            System.out.print("📞 New Phone (press Enter to skip): ");
            String newPhone = sc.nextLine().trim();
            if (newPhone.isEmpty()) newPhone = currentPhone;

            System.out.print("📧 New Email (press Enter to skip): ");
            String newEmail = sc.nextLine().trim();
            if (newEmail.isEmpty()) newEmail = currentEmail;

            // Check if no changes
            if (newName.equals(currentName) && newPhone.equals(currentPhone) && newEmail.equals(currentEmail)) {
                logger.warn("⚠️ No changes detected for account: {}", accNo);
                System.out.println("⚠️ No changes detected — all values are same as before.");
                return;
            }

            // Validations
            if (!isValidName(newName)) {
                logger.warn("❌ Invalid name format entered for account: {}", accNo);
                System.out.println("❌ Invalid name. Only alphabets and spaces are allowed.");
                return;
            }
            if (!isValidPhone(newPhone)) {
                logger.warn("❌ Invalid phone format entered for account: {}", accNo);
                System.out.println("❌ Invalid phone number. Must be 10 digits.");
                return;
            }
            if (!isValidEmail(newEmail)) {
                logger.warn("❌ Invalid email format entered for account: {}", accNo);
                System.out.println("❌ Invalid email format.");
                return;
            }

            // Ensure users table is linked correctly
            String linkCheck = "UPDATE users SET accountNumber=? WHERE username=? AND (accountNumber IS NULL OR accountNumber='')";
            try (PreparedStatement linkStmt = conn.prepareStatement(linkCheck)) {
                linkStmt.setString(1, accNo);
                linkStmt.setString(2, currentUsername);
                linkStmt.executeUpdate();
            }

            // Update both tables
            String updateAccounts = "UPDATE accounts SET accountHolder=?, phone=?, email=? WHERE accountNumber=?";
            String updateUsers = "UPDATE users SET email=? WHERE accountNumber=?";

            try (PreparedStatement ps1 = conn.prepareStatement(updateAccounts);
                 PreparedStatement ps2 = conn.prepareStatement(updateUsers)) {

                ps1.setString(1, newName);
                ps1.setString(2, newPhone);
                ps1.setString(3, newEmail);
                ps1.setString(4, accNo);
                ps1.executeUpdate();

                ps2.setString(1, newEmail);
                ps2.setString(2, accNo);
                int userUpdated = ps2.executeUpdate();

                if (userUpdated > 0) {
                    logger.info("✅ Email also updated in users table for account: {}", accNo);
                } else {
                    logger.warn("⚠️ No user record found linked with accountNumber {}. Check your DB data.", accNo);
                }

                logger.info("✅ Account details successfully updated for account: {}", accNo);
                System.out.println("✅ Account details updated successfully!");
            }

        } catch (SQLException e) {
            logger.error("❌ Database error while updating account details for {}: {}", accNo, e.getMessage());
            System.out.println("❌ Database error: " + e.getMessage());
        }
    }

    // ----------------------------
    // Delete Account (requires login password confirmation)
    // ----------------------------
    public boolean deleteAccount(String accountNumber, String username) {
        try (Connection conn = Database.getConnection()) {
            if (!verifyPasswordPrompt(username, accountNumber)) return false;

            System.out.print("Are you sure you want to delete this account? Type YES to confirm: ");
            String confirm = sc.nextLine();
            if (!confirm.equalsIgnoreCase("YES")) {
                System.out.println("❌ Account deletion cancelled.");
                return false;
            }

            // Delete transactions first
            try (PreparedStatement delTx = conn.prepareStatement("DELETE FROM transactions WHERE accountNumber = ?")) {
                delTx.setString(1, accountNumber);
                delTx.executeUpdate();
            }

            // Delete from accounts
            try (PreparedStatement delAcc = conn.prepareStatement("DELETE FROM accounts WHERE accountNumber = ?")) {
                delAcc.setString(1, accountNumber);
                int deleted = delAcc.executeUpdate();

                // Delete from users table
                try (PreparedStatement delUser = conn.prepareStatement("DELETE FROM users WHERE accountNumber = ?")) {
                    delUser.setString(1, accountNumber);
                    delUser.executeUpdate();
                }

                if (deleted > 0) {
                    System.out.println("✅ Account deleted successfully.");
                    logger.warn("Account deleted: {}", accountNumber);
                    return true; // ✅ triggers logout in main program
                } else {
                    System.out.println("❌ Account not found.");
                }
            }
        } catch (Exception e) {
            logger.error("Error deleting account", e);
        }
        return false;
    }
    public boolean deleteBankAccount(String accNo) {
        String query = "DELETE FROM accounts WHERE accountNumber = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, accNo);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                logger.info("✅ Deleted bank account (and linked user via CASCADE): {}", accNo);
                System.out.println("✅ Bank account deleted successfully (linked user removed too).");
                return true;
            } else {
                logger.warn("⚠️ Attempted to delete non-existing account: {}", accNo);
                System.out.println("❌ Account not found.");
                return false;
            }

        } catch (SQLException e) {
            logger.error("❌ Database error while deleting account {}: {}", accNo, e.getMessage());
            e.printStackTrace();
            System.out.println("❌ Database error while deleting account: " + e.getMessage());
            return false;
        }
    }


}
