package com.bank.service;

import com.bank.dao.Database;
import com.bank.model.Transaction;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransactionService {

    // ✅ Record a transaction
    public void recordTransaction(String accountNumber, String type, double amount, String targetAccount) {
        String sql = "INSERT INTO transactions(accountNumber, type, amount, targetAccount) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            pstmt.setString(2, type);
            pstmt.setDouble(3, amount);
            pstmt.setString(4, targetAccount);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("❌ Error recording transaction: " + e.getMessage());
        }
    }

    // ✅ Retrieve transaction history for an account (in IST)
    public List<Transaction> getTransactions(String accountNumber) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE accountNumber = ? ORDER BY timestamp DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("timestamp");
                // Convert to IST
                LocalDateTime utcTime = ts.toLocalDateTime();
                ZonedDateTime istTime = utcTime.atZone(ZoneId.of("UTC"))
                        .withZoneSameInstant(ZoneId.of("Asia/Kolkata"));
                String formattedTime = istTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                list.add(new Transaction(
                        rs.getString("accountNumber"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getString("targetAccount"),
                        formattedTime
                ));
            }

        } catch (SQLException e) {
            System.out.println("❌ Error fetching transactions: " + e.getMessage());
        }

        return list;
    }
}
