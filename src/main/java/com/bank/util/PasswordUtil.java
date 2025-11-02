package com.bank.util;

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;

/**
 * Utility for password hashing, verification and temporary password generation.
 * Requires the 'org.mindrot:jbcrypt' library on the classpath.
 */
public final class PasswordUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private PasswordUtil() { /* no instances */ }

    /**
     * Hash a plain password with BCrypt.
     */
    public static String hashPassword(String plain) {
        if (plain == null) return null;
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    /**
     * Verify a plain password against a stored BCrypt hash.
     */
    public static boolean checkPassword(String plain, String hashed) {
        if (plain == null || hashed == null) return false;
        try {
            return BCrypt.checkpw(plain, hashed);
        } catch (IllegalArgumentException e) {
            // in case hashed value is malformed
            return false;
        }
    }

    /**
     * Generate a cryptographically secure temporary password made of letters+digits.
     * @param length length of the temporary password (recommended >= 6)
     */
    public static String generateTempPassword(int length) {
        if (length <= 0) length = 8;
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = SECURE_RANDOM.nextInt(CHARSET.length());
            sb.append(CHARSET.charAt(idx));
        }
        return sb.toString();
    }
}
