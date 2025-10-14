package com.bank;

import java.sql.Connection;

public class DBTest {
    public static void main(String[] args) {
        Connection conn = Database.getConnection(); // now valid ✅
        if (conn != null) {
            System.out.println("✅ Database connection test successful!");
        } else {
            System.out.println("❌ Database connection test failed!");
        }
    }
}
