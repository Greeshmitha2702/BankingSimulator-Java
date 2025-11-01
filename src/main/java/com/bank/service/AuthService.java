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
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                logger.warn("Invalid login attempt for username: {}", username);
                System.out.println("❌ Invalid username!");
                return false;
            }

            String storedPass = rs.getString("password");
            if (PasswordUtil.checkPassword(password, storedPass)) {
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

    // ✅ Register new user linked to account number
    public boolean registerUser(String username, String password, String accountNumber) {
        String sql = "INSERT INTO users(username, password, accountNumber) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            String hashedPassword = PasswordUtil.hashPassword(password);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, accountNumber);
            pstmt.executeUpdate();

            logger.info("✅ New user registered: {} linked to {}", username, accountNumber);
            System.out.println("✅ User registered successfully!");
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                logger.warn("Username already exists: {}", username);
                System.out.println("❌ Username already exists. Please choose another.");
            } else {
                logger.error("Database error during registration for {}: {}", username, e.getMessage());
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
}
