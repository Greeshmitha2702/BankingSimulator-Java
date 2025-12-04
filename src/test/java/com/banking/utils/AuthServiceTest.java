package com.banking.utils;

import com.bank.service.AuthService;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthServiceTest {

    private AuthService auth;

    @BeforeEach
    void setup() {
        auth = new AuthService();
    }

    @Test
    @Order(1)
    void testRegisterAndLogin() {
        // Use correct method name
        boolean created = auth.registerUser("testUser", "pass123", "ACC1763562810903");
        assertTrue(created);

        boolean login = auth.loginUser("testUser", "pass123");
        assertTrue(login);
    }

    @Test
    @Order(2)
    void testLoginFailure() {
        // loginUser() not login()
        assertFalse(auth.loginUser("unknown", "abc"));
    }

    @Test
    @Order(3)
    void testPasswordReset() {
        boolean reset = auth.resetPasswordAndEmail("ACC1763562810903");
        assertTrue(reset);
    }
}
