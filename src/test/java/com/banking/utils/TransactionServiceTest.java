// =========================
// TransactionServiceTest.java
// =========================
package com.banking.utils;

import com.bank.model.Transaction;
import com.bank.service.TransactionService;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceTest {

    private TransactionService txService;

    @BeforeEach
    void setup() {
        txService = new TransactionService();
    }

    @Test
    void testRecordTransaction() {
        // Insert a test entry
        txService.recordTransaction("ACC1763562810903", "TEST_DEPOSIT", 500.0, null);

        // Retrieve history
        List<Transaction> history = txService.getTransactions("ACC1763562810903");

        assertNotNull(history);
        assertFalse(history.isEmpty());

        Transaction latest = history.get(0);

        assertEquals("ACC1763562810903", latest.getAccountNumber());
        assertEquals("TEST_DEPOSIT", latest.getType());
        assertEquals(500.0, latest.getAmount());
    }

    @Test
    void testGetTransactionsEmptyList() {
        List<Transaction> history = txService.getTransactions("NON_EXIST_ACC");
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }
}
