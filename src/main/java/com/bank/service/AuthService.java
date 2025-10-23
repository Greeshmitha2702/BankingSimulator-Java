package com.bank.service;

import com.bank.dao.Database;
import com.bank.util.PasswordUtil;
import java.sql.*;

public class AuthService {

    // ✅ Login existing user
    // ✅ Login existing user (fixed version)
    public boolean loginUser(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                System.out.println("❌ Invalid username!");
                return false;
            }

            String storedPass = rs.getString("password");
            if (PasswordUtil.checkPassword(password, storedPass)) {
                System.out.println("✅ Login successful. Welcome, " + username + "!");
                return true;
            } else {
                System.out.println("❌ Incorrect password!");
                return false;
            }

        } catch (SQLException e) {
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
            String hashedPassword = PasswordUtil.hashPassword(password); // hash here
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, accountNumber);
            pstmt.executeUpdate();
            System.out.println("✅ User registered successfully!");
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed: users.username")) {
                System.out.println("❌ Username already exists. Please choose another.");
            } else {
                System.out.println("❌ Database error during registration: " + e.getMessage());
            }
            return false;
        }
    }

    // ✅ Check if username exists
    public boolean userExists(String username) {
        String sql = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            System.out.println("❌ Error checking user existence: " + e.getMessage());
            return false;
        }
    }
    // AuthService.java
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
            System.out.println("❌ Database error: " + e.getMessage());
        }
        return null; // no account linked
    }

}
