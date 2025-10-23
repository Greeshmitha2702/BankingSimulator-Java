package com.bank.dao;

import com.bank.model.User;
import java.sql.*;

public class UserDAO {

    // create user row
    public static void createUser(String accountNumber, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users(accountNumber, password_hash) VALUES(?,?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
        }
    }

    // find user by accountNumber
    public static User findByAccountNumber(String accountNumber) {
        String sql = "SELECT accountNumber, password_hash FROM users WHERE accountNumber = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(rs.getString("accountNumber"), rs.getString("password_hash"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
