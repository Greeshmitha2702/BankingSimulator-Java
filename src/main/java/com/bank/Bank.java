package com.bank;

import java.util.HashMap;
import java.util.Map;

public class Bank {
    private final Map<String, Account> accounts = new HashMap<>();

    public boolean isValidName(String name) {
        return name.matches("[A-Za-z ]+");
    }

    public boolean isValidAccountNumber(String accNo) {
        return accNo.matches("\\d+");
    }

    public void createAccount(String accountNumber, String holderName, double initialDeposit) {
        if (!isValidAccountNumber(accountNumber)) {
            System.out.println("❌ Invalid account number. Use only digits.");
            return;
        }

        if (!isValidName(holderName)) {
            System.out.println("❌ Invalid name. Name should contain only alphabets and spaces.");
            return;
        }

        if (initialDeposit <= 0) {
            System.out.println("❌ Initial deposit must be greater than zero.");
            return;
        }

        if (accounts.containsKey(accountNumber)) {
            System.out.println("❌ Account number already exists!");
            return;
        }

        Account acc = new Account(accountNumber, holderName, initialDeposit);
        accounts.put(accountNumber, acc);
        System.out.println("✅ Account created successfully for " + holderName + "!");
    }

    public void deposit(String accountNumber, double amount) {
        Account acc = accounts.get(accountNumber);
        if (acc != null) {
            acc.deposit(amount);
        } else {
            System.out.println("❌ Account not found!");
        }
    }

    public void withdraw(String accountNumber, double amount) {
        Account acc = accounts.get(accountNumber);
        if (acc != null) {
            acc.withdraw(amount);
        } else {
            System.out.println("❌ Account not found!");
        }
    }

    public void checkBalance(String accountNumber) {
        Account acc = accounts.get(accountNumber);
        if (acc != null) {
            System.out.println("💰 Balance for " + acc.getAccountHolder() + ": ₹" + acc.getBalance());
        } else {
            System.out.println("❌ Account not found!");
        }
    }

    public void displayAllAccounts() {
        if (accounts.isEmpty()) {
            System.out.println("⚠️ No accounts to display.");
        } else {
            System.out.println("\n--- All Accounts ---");
            for (Account acc : accounts.values()) {
                System.out.println(acc);
            }
        }
    }
}
