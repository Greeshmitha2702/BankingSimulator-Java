package com.bank.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Properties;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import java.io.File;

public class EmailService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String SENDER_EMAIL = dotenv.get("SENDER_EMAIL");
    private static final String APP_PASSWORD = dotenv.get("APP_PASSWORD");

    private static Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });
    }

    public static void sendEmail(String to, String subject, String messageText) {
        try {
            Message message = new MimeMessage(createSession());
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(messageText);

            Transport.send(message);
            System.out.println("📧 Email sent successfully to: " + to);

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static void sendEmailWithAttachment(String to, String subject, String messageText, String filePath) {
        try {
            Message message = new MimeMessage(createSession());
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            // Create text part
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(messageText);

            // Create file attachment part
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(new File(filePath));

            // Combine both into one message
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);

            Transport.send(message);
            System.out.println("📎 Email with attachment sent successfully to: " + to);

        } catch (Exception e) {
            System.err.println("❌ Failed to send email with attachment: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
