package com.bank.model;

public class User {
    private final String accountNumber;
    private final String passwordHash; // store hashed password only

    public User(String accountNumber, String passwordHash) {
        this.accountNumber = accountNumber;
        this.passwordHash = passwordHash;
    }

    public String getAccountNumber() { return accountNumber; }
    public String getPasswordHash() { return passwordHash; }
}
