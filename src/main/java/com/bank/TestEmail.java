package com.bank;

import com.bank.service.EmailService;

public class TestEmail {
    public static void main(String[] args) {
        EmailService.sendEmail(
                "lbinguma2@gitam.in", // 👉 put your own email here
                "Test Email from Banking Simulator",
                "Hello! 👋 This is a test email sent from your Banking Simulator project."
        );
    }
}
