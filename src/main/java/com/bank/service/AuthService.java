package com.bank.service;

import com.bank.dao.Database;
import com.bank.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // ✅ Login existing user
    public boolean loginUser(String username, String password) {
        String sql = "SELECT password, locked FROM users WHERE username = ?";
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
            if (locked) {
                System.out.println("🔒 Your user is locked. Use Forgot Password to reset and unlock.");
                return false;
            }

            String storedHash = rs.getString("password");
            if (PasswordUtil.checkPassword(password, storedHash)) {
                logger.info("✅ Login successful for user: {}", username);
                System.out.println("✅ Login successful. Welcome, " + username + "!");
                return true;
            } else {
                logger.warn("Incorrect password for user: {}", username);
                System.out.println("❌ Incorrect password!");
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
        String sqlInsert = "INSERT INTO users(username, password, accountNumber, email, locked) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection()) {

            // fetch email from accounts (might be null)
            String email = null;
            String q = "SELECT email FROM accounts WHERE accountNumber = ?";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setString(1, accountNumber);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    email = rs.getString("email");
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                pstmt.setString(1, username);
                String hashedPassword = PasswordUtil.hashPassword(password);
                pstmt.setString(2, hashedPassword);
                pstmt.setString(3, accountNumber);
                pstmt.setString(4, email);
                pstmt.setInt(5, 0); // not locked
                pstmt.executeUpdate();
            }

            logger.info("✅ New user registered: {} linked to {} (email={})", username, accountNumber, email);
            System.out.println("✅ User registered successfully!");
            if (email != null && !email.isEmpty()) {
                EmailService.sendEmail(
                        email,
                        "Welcome to Banking Simulator 🎉",
                        "Hello " + username + ",\n\nYour account has been successfully created and linked to " + accountNumber +
                                ".\n\nHappy Banking!\n\n— Banking Simulator Team"
                );
            }
            return true;

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                logger.warn("Username already exists: {}", username);
                System.out.println("❌ Username already exists. Please choose another.");
            } else {
                logger.error("Database error during registration for {}: {}", username, e.getMessage(), e);
                System.out.println("❌ Database error during registration: " + e.getMessage());
            }
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
        String findUserSql = "SELECT username, email FROM users WHERE accountNumber = ?";
        String findAccountEmail = "SELECT email FROM accounts WHERE accountNumber = ?";
        String username = null;
        String email = null;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(findUserSql)) {

            ps.setString(1, accountNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                username = rs.getString("username");
                email = rs.getString("email"); // could be null if not set in users
            } else {
                // fallback to accounts table for email only
                try (PreparedStatement ps2 = conn.prepareStatement(findAccountEmail)) {
                    ps2.setString(1, accountNumber);
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        email = rs2.getString("email");
                    }
                }
            }

            if (username == null && (email == null || email.isEmpty())) {
                // no user and no email found
                return false;
            }

            // generate temporary password (6 chars alphanumeric)
            String temp = String.valueOf((int)(Math.random() * 900000) + 100000); // 6-digit temp
            String hashed = PasswordUtil.hashPassword(temp);

            // update users.password if a user exists for accountNumber
            if (username != null) {
                String updUser = "UPDATE users SET password=?, locked=0 WHERE accountNumber=?";
                try (PreparedStatement upd = conn.prepareStatement(updUser)) {
                    upd.setString(1, hashed);
                    upd.setString(2, accountNumber);
                    upd.executeUpdate();
                }
            } else {
                // no user row exists; can't reset login password — inform caller
                return false;
            }

            // unlock accounts.locked as well
            String unlockAcc = "UPDATE accounts SET locked = 0 WHERE accountNumber = ?";
            try (PreparedStatement u2 = conn.prepareStatement(unlockAcc)) {
                u2.setString(1, accountNumber);
                u2.executeUpdate();
            }

            // send email (plain temp password)
            if (email != null && !email.isEmpty()) {
                String subject = "🔐 Banking Simulator — Temporary Password";
                String body = "Hello,\n\nA temporary password has been generated for your account (" + accountNumber + ").\n\n" +
                        "Temporary password: " + temp + "\n\n" +
                        "Please login and immediately change your password.\n\n— Banking Simulator Team";
                EmailService.sendEmail(email, subject, body);
                logger.info("Temporary password emailed to {}", email);
                return true;
            } else {
                // email missing for account
                return false;
            }

        } catch (SQLException e) {
            logger.error("Error during resetPasswordAndEmail for {}: {}", accountNumber, e.getMessage(), e);
            return false;
        }
    }
}
