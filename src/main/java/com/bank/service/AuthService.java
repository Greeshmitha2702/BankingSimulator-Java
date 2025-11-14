package com.bank.service;

import com.bank.dao.Database;
import com.bank.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

import java.sql.*;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // ✅ Login existing user
    public boolean loginUser(String username, String password) {
        String sql = "SELECT password, locked, failed_attempts FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                logger.warn("Invalid login attempt for username: {}", username);
                System.out.println("❌ Invalid username!");
                return false;
            }

            boolean locked = rs.getInt("locked") == 1;
            int failed_attempts = rs.getInt("failed_attempts");
            String storedHash = rs.getString("password");

            if (locked) {
                logger.warn("🔒 Locked user {} attempted to log in.", username);
                System.out.println("🔒 Your user is locked. Use Forgot Password to reset and unlock.");
                return false;
            }

            if (PasswordUtil.checkPassword(password, storedHash)) {
                // ✅ Successful login
                String resetAttempts = "UPDATE users SET failed_attempts = 0 WHERE username = ?";
                try (PreparedStatement resetStmt = conn.prepareStatement(resetAttempts)) {
                    resetStmt.setString(1, username);
                    resetStmt.executeUpdate();
                }
                logger.info("✅ Login successful for user: {}", username);
                System.out.println("✅ Login successful. Welcome, " + username + "!");
                return true;
            } else {
                // ❌ Wrong password
                failed_attempts++;

                if (failed_attempts >= 3) {
                    // Lock user
                    String lockSql = "UPDATE users SET failed_attempts = ?, locked = 1 WHERE username = ?";
                    try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                        lockStmt.setInt(1, failed_attempts);
                        lockStmt.setString(2, username);
                        lockStmt.executeUpdate();
                    }
                    logger.warn("🔒 Account locked after 3 failed attempts: {}", username);
                    System.out.println("🔒 Account locked due to 3 failed login attempts!");
                } else {
                    // Just increment attempt count
                    String updateSql = "UPDATE users SET failed_attempts = ? WHERE username = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, failed_attempts);
                        updateStmt.setString(2, username);
                        updateStmt.executeUpdate();
                    }
                    logger.warn("❌ Incorrect password attempt {} for user {}", failed_attempts, username);
                    System.out.println("❌ Incorrect password! (" + failed_attempts + "/3)");
                }
                return false;
            }

        } catch (SQLException e) {
            logger.error("Database error during login for user {}: {}", username, e.getMessage());
            System.out.println("❌ Database error during login: " + e.getMessage());
            return false;
        }
    }

    // Register new user linked to account number (also stores email if account has one)
    public boolean registerUser(String username, String password, String accountNumber) {
        String checkUserSql = "SELECT COUNT(*) FROM users WHERE username = ? OR accountNumber = ?";
        String insertUserSql = "INSERT INTO users (username, password, accountNumber) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkUserSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertUserSql)) {

            // Check if username OR accountNumber already exist
            checkStmt.setString(1, username);
            checkStmt.setString(2, accountNumber);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("❌ Username or Account Number already linked to another user.");
                logger.warn("Duplicate registration attempt: username={} or accountNumber={}", username, accountNumber);
                return false;
            }

            // Hash the password
            String hashedPassword = PasswordUtil.hashPassword(password);

            // Insert new user
            insertStmt.setString(1, username);
            insertStmt.setString(2, hashedPassword);
            insertStmt.setString(3, accountNumber);
            insertStmt.executeUpdate();

            System.out.println("✅ Registration successful!");
            logger.info("New user registered successfully: {} linked to account {}", username, accountNumber);
            return true;

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                System.out.println("❌ Account Number already linked to another user.");
            } else {
                System.out.println("❌ Database error during registration: " + e.getMessage());
            }
            logger.error("Database error while registering user {}: {}", username, e.getMessage());
            return false;
        }
    }


    public boolean userExists(String username) {
        String sql = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            logger.error("Error checking user existence: {}", e.getMessage());
            System.out.println("❌ Error checking user existence: " + e.getMessage());
            return false;
        }
    }

    public String getLinkedAccount(String username) {
        String sql = "SELECT accountNumber FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("accountNumber");
            }
        } catch (SQLException e) {
            logger.error("Error retrieving linked account for {}: {}", username, e.getMessage());
            System.out.println("❌ Database error: " + e.getMessage());
        }
        return null;
    }

    // verify login password (for sensitive actions)
    public boolean verifyPassword(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password");
                return PasswordUtil.checkPassword(password, storedHash);
            }

        } catch (SQLException e) {
            logger.error("Error verifying password for {}: {}", username, e.getMessage());
        }
        return false;
    }

    /**
     * Reset password for accountNumber:
     * - find the linked username/email
     * - generate a temporary password
     * - store hashed password in users table
     * - unlock both users.locked and accounts.locked
     * - send the plain temporary password to the user's registered email
     *
     * Returns true if reset+email succeeded.
     */
    public boolean resetPasswordAndEmail(String accountNumber) {
        // ✅ ensure user's email is updated before fetching
        copyEmailFromAccount(accountNumber);
        String selectSql = "SELECT username, email FROM users WHERE accountNumber = ?";
        String updateSql = "UPDATE users SET password=?, failed_attempts=0, locked=0 WHERE accountNumber=?";

        try (Connection conn = Database.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            selectStmt.setString(1, accountNumber);
            ResultSet rs = selectStmt.executeQuery();

            if (!rs.next()) {
                logger.warn("⚠️ Password reset attempted for non-existing account: {}", accountNumber);
                System.out.println("⚠️ No such account found!");
                return false;
            }

            String username = rs.getString("username");
            String email = rs.getString("email");

            // Generate temporary password
            String tempPassword = PasswordUtil.generateTempPassword(8);
            String hashedPassword = PasswordUtil.hashPassword(tempPassword);

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, hashedPassword);
                updateStmt.setString(2, accountNumber);
                updateStmt.executeUpdate();
            }

            logger.info("🔓 Password reset and account unlocked for user: {}", username);
            System.out.println("✅ Password reset successfully!");
            System.out.println("🔓 Account unlocked. Temporary password generated.");

            // Send email (if you have a mail service)
            if (email != null && !email.isEmpty()) {
                EmailService.sendEmail(email,
                        "Password Reset - Banking System",
                        "Dear " + username + ",\n\nYour account password has been reset.\nTemporary password: "
                                + tempPassword + "\n\nPlease log in and change it immediately.\n\n- Banking App");
                logger.info("📧 Temporary password sent to {}", email);
            } else {
                System.out.println("⚠️ No email found for this account. Please contact support.");
                logger.warn("⚠️ No email associated with account: {}", accountNumber);
            }

            return true;

        } catch (SQLException e) {
            logger.error("❌ Database error while resetting password for account {}: {}", accountNumber, e.getMessage());
            System.out.println("❌ Database error: " + e.getMessage());
            return false;
        }
    }
    public boolean isAccountLinked(String accountNumber) {
        String sql = "SELECT 1 FROM users WHERE accountNumber = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // true if any user linked
        } catch (SQLException e) {
            logger.error("Database error checking if account {} is linked: {}", accountNumber, e.getMessage());
            return false;
        }
    }
    public boolean isUsernameTaken(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // true if username exists

        } catch (SQLException e) {
            logger.error("Database error checking username {}: {}", username, e.getMessage());
            return true; // safer to block registration on error
        }
    }
    public static void copyEmailFromAccount(String accountNumber) {
        String sql = """
        UPDATE users
        SET email = (
            SELECT email FROM accounts WHERE accounts.accountNumber = users.accountNumber
        )
        WHERE users.accountNumber = ?;
    """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNumber);
            int rows = stmt.executeUpdate();

            if (rows > 0)
                System.out.println("📩 Email copied from accounts to users for account: " + accountNumber);
            else
                System.out.println("⚠️ No matching user found to copy email for account: " + accountNumber);

        } catch (SQLException e) {
            System.err.println("❌ Error copying email from accounts to users: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // ✅ Update user details (username, password)
    public boolean updateUserDetails(String currentUsername, Scanner sc) {
        try (Connection conn = Database.getConnection()) {

            System.out.println("\n🛠️ What would you like to update?");
            System.out.println("1️⃣ Change Username");
            System.out.println("2️⃣ Change Password");
            System.out.print("Enter your choice: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> {
                    System.out.print("Enter new username: ");
                    String newUsername = sc.nextLine();

                    // Check if taken
                    String checkSql = "SELECT 1 FROM users WHERE username = ?";
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                        checkStmt.setString(1, newUsername);
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next()) {
                            System.out.println("❌ That username is already taken. Please choose another.");
                            return false;
                        }
                    }

                    String updateSql = "UPDATE users SET username = ? WHERE username = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, newUsername);
                        updateStmt.setString(2, currentUsername);
                        int rows = updateStmt.executeUpdate();
                        if (rows > 0) {
                            System.out.println("✅ Username updated successfully! Please log in again with your new username.");
                            return true; // 🔁 triggers logout
                        } else {
                            System.out.println("❌ Failed to update username.");
                        }
                    }
                }

                case "2" -> {
                    System.out.print("Enter your current password: ");
                    String currentPassword = sc.nextLine();

                    if (!verifyPassword(currentUsername, currentPassword)) {
                        System.out.println("❌ Incorrect current password!");
                        return false;
                    }

                    System.out.print("Enter new password: ");
                    String newPassword = sc.nextLine();
                    System.out.print("Confirm new password: ");
                    String confirmPassword = sc.nextLine();

                    if (!newPassword.equals(confirmPassword)) {
                        System.out.println("❌ Passwords do not match!");
                        return false;
                    }

                    String hashed = PasswordUtil.hashPassword(newPassword);
                    String updateSql = "UPDATE users SET password = ? WHERE username = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, hashed);
                        updateStmt.setString(2, currentUsername);
                        int rows = updateStmt.executeUpdate();
                        if (rows > 0) {
                            System.out.println("✅ Password updated successfully! Please log in again.");
                            return true; // 🔁 also triggers logout
                        } else {
                            System.out.println("❌ Failed to update password.");
                        }
                    }
                }

                default -> System.out.println("❌ Invalid choice.");
            }

        } catch (Exception e) {
            logger.error("Error updating user details: {}", e.getMessage());
            System.out.println("❌ Database error while updating user details: " + e.getMessage());
        }
        return false;
    }
}
