package com.banking.dao;

import com.bank.dao.Database;
import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

public class DAOTests {

    @Test
    void testUserInsertAndFetch() throws Exception {
        Connection conn = Database.getConnection();

        // Insert according to REAL TABLE STRUCTURE
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users(accountNumber, password_hash) VALUES(?,?)"
        );
        stmt.setString(1, "ACC1763562810903");
        stmt.setString(2, "hashedPass123");
        stmt.executeUpdate();

        // Fetch
        PreparedStatement sel = conn.prepareStatement(
                "SELECT * FROM users WHERE accountNumber=?"
        );
        sel.setString(1, "ACC1763562810903");
        ResultSet rs = sel.executeQuery();

        assertTrue(rs.next());
        assertEquals("hashedPass123", rs.getString("password_hash"));
    }


    @Test
    void testAccountDAOInsert() throws Exception {
        Connection conn = Database.getConnection();

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO accounts(accountNumber, accountHolder, phone, email, balance) VALUES(?,?,?,?,?)"
        );
        stmt.setString(1, "ACC1763562810903");
        stmt.setString(2, "Tester");
        stmt.setString(3, "9999999999");
        stmt.setString(4, "tester@mail.com");
        stmt.setDouble(5, 1000);

        int rows = stmt.executeUpdate();
        assertEquals(1, rows);
    }


    @Test
    void testTransactionDAOInsert() throws Exception {
        Connection conn = Database.getConnection();

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO transactions(accountNumber, type, amount, targetAccount) VALUES(?,?,?,?)"
        );
        stmt.setString(1, "ACC1763562810903");
        stmt.setString(2, "DEPOSIT");
        stmt.setDouble(3, 200);
        stmt.setString(4, null);

        int rows = stmt.executeUpdate();
        assertEquals(1, rows);
    }
}
