package com.bank.model;

public class Account {
    private final String accountNumber;
    private final String accountHolder;
    private double balance;
    private String phone;


    public Account(String accountNumber, String accountHolder, String phone, double balance) {
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.phone = phone;
        this.balance = balance;
    }


    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            System.out.println("✅ Deposited ₹" + amount + " successfully!");
        } else {
            System.out.println("❌ Enter a valid amount to deposit.");
        }
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            System.out.println("❌ Withdrawal amount must be greater than zero.");
        } else if (amount > balance) {
            System.out.println("❌ Insufficient balance!");
        } else {
            balance -= amount;
            System.out.println("✅ Withdrew ₹" + amount + " successfully!");
        }
    }

    public double getBalance() {
        return balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
    public String getPhone() {
        return phone;
    }


    public String getAccountHolder() {
        return accountHolder;
    }

    @Override
    public String toString() {
        return "Account{" +
                "Account No='" + accountNumber + '\'' +
                ", Holder='" + accountHolder + '\'' +
                ", Phone='" + phone + '\'' +
                ", Balance=" + balance +
                '}';
    }

}
