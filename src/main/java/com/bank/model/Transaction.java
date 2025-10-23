package com.bank.model;

public class Transaction {
    private final String accountNumber;
    private final String type;
    private final double amount;
    private final String targetAccount;
    private final String timestamp; // Store as IST string

    public Transaction(String accountNumber, String type, double amount, String targetAccount, String timestamp) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.targetAccount = targetAccount;
        this.timestamp = timestamp;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getTargetAccount() {
        return targetAccount;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s ₹%.2f%s", timestamp, type.toUpperCase(), amount,
                targetAccount != null ? " → " + targetAccount : "");
    }
}
