package com.bank.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    // generate hashed password
    public static String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    // verify
    public static boolean checkPassword(String plain, String hashed) {
        return BCrypt.checkpw(plain, hashed);
    }
}
