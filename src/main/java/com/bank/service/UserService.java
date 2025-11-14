package com.bank.service;

import com.bank.dao.Database;
import com.bank.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Scanner;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_ATTEMPTS = 3;

    // 🔑 User Login Method
    public boolean login(Scanner sc) {
        System.out.print("Enter Username: ");
        String username = sc.nextLine();
        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                boolean locked = rs.getInt("locked") == 1;
                if (locked) {
                    System.out.println("🔒 Your account is locked. Please reset your password using 'Forgot Password'.");
                    logger.warn("Login attempt on locked account: {}", username);
                    return false;
                }

                String storedPass = rs.getString("password");
                int attempts = rs.getInt("failed_attempts");

                if (storedPass.equals(password)) {
                    resetFailedAttempts(username);
                    System.out.println("✅ Login successful. Welcome, " + username + "!");
                    logger.info("User {} logged in successfully.", username);
                    return true;
                } else {
                    attempts++;
                    if (attempts >= MAX_ATTEMPTS) {
                        lockAccount(username);
                        System.out.println("❌ Too many failed attempts. Account locked!");
                        logger.warn("User {} account locked after 3 failed attempts.", username);
                    } else {
                        updateFailedAttempts(username, attempts);
                        System.out.println("❌ Incorrect password (" + attempts + "/3). Try again.");
                    }
                }
            } else {
                System.out.println("❌ User not found!");
            }

        } catch (SQLException e) {
            logger.error("Database error during login for {}", username, e);
        }
        return false;
    }

    // 🧩 Reset failed attempts
    private void resetFailedAttempts(String username) {
        String sql = "UPDATE users SET failed_attempts = 0 WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error resetting failed attempts for {}", username, e);
        }
    }

    // 🧩 Update failed attempts
    private void updateFailedAttempts(String username, int attempts) {
        String sql = "UPDATE users SET failed_attempts = ? WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating failed attempts for {}", username, e);
        }
    }

    // 🔒 Lock user account
    private void lockAccount(String username) {
        String sql = "UPDATE users SET locked = 1 WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error locking account for {}", username, e);
        }
    }

    // 🔓 Unlock user account
    private void unlockAccount(String username) {
        String sql = "UPDATE users SET locked = 0, failed_attempts = 0 WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error unlocking account for {}", username, e);
        }
    }

    // 🧠 Forgot Password
    public void forgotPassword(Scanner sc) {
        System.out.print("Enter your Account Number: ");
        String accNo = sc.nextLine();

        String sql = "SELECT username, email, password, locked FROM users WHERE accNo = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accNo);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");
                String password = rs.getString("password");
                boolean locked = rs.getInt("locked") == 1;

                if (email == null || email.isEmpty()) {
                    System.out.println("❌ No email linked to this account.");
                    return;
                }

                // Send password via email
                EmailService.sendEmail(
                        email,
                        "🔐 Banking Simulator - Password Recovery",
                        "Hello " + username + ",\n\nYour password is: " + password +
                                "\n\nIf you didn’t request this, please contact support.\n\n— Banking Simulator Team"
                );

                if (locked) {
                    unlockAccount(username);
                    logger.info("Account unlocked automatically after password reset for {}", username);
                }

                System.out.println("📩 Password sent to your registered email!");
                logger.info("Password recovery email sent to {}", email);

            } else {
                System.out.println("❌ Account not found!");
            }

        } catch (SQLException e) {
            logger.error("Error in forgotPassword() for accNo {}", accNo, e);
        }
    }
}
