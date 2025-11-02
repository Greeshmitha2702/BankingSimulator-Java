package com.bank.dao;

import java.sql.Connection;
import java.sql.SQLException;

public class DBTest {
    public static void main(String[] args) {
        try (Connection conn = Database.getConnection()) {
            if (conn != null) {
                System.out.println("✅ Database connection test successful!");
            } else {
                System.out.println("❌ Database connection test failed!");
            }
        } catch (SQLException e) {
            System.out.println("❌ Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
