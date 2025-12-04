package com.banking.utils;

import com.bank.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    void testHashPasswordConsistency() {
        String pwd = "test123";
        String hashed1 = PasswordUtil.hashPassword(pwd);
        String hashed2 = PasswordUtil.hashPassword(pwd);

        // BCrypt uses a random salt, so hashes should NOT be equal
        assertNotEquals(hashed1, hashed2);
    }

    @Test
    void testPasswordVerification() {
        String pwd = "helloPass";
        String hashed = PasswordUtil.hashPassword(pwd);

        assertTrue(PasswordUtil.checkPassword(pwd, hashed));
        assertFalse(PasswordUtil.checkPassword("wrong", hashed));
    }
}
