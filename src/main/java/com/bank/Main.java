package com.bank;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Bank bank = new Bank();

        // ✅ Ensure table exists in DB
        Database.createTableIfNotExists();

        // ✅ Load existing accounts from DB
        List<Account> existingAccounts = bank.getAllAccountsFromDB();
        if (!existingAccounts.isEmpty()) {
            System.out.println("📂 Previous accounts loaded from DB:");
            existingAccounts.forEach(System.out::println);
        }

        System.out.println("🏦 Welcome to Banking Simulator 🏦");

        while (true) {
            try {
                System.out.println("\nChoose an option:");
                System.out.println("1️⃣ Create Account");
                System.out.println("2️⃣ Deposit Money");
                System.out.println("3️⃣ Withdraw Money");
                System.out.println("4️⃣ Check Balance");
                System.out.println("5️⃣ Display All Accounts");
                System.out.println("6️⃣ Exit");
                System.out.print("Enter your choice: ");

                int choice = sc.nextInt();
                sc.nextLine(); // consume newline

                switch (choice) {
                    case 1 -> {
                        System.out.print("Enter Account Number: ");
                        String accNo = sc.nextLine();

                        System.out.print("Enter Account Holder Name: ");
                        String holder = sc.nextLine();

                        System.out.print("Enter Initial Deposit: ");
                        double initBal = sc.nextDouble();
                        sc.nextLine();

                        bank.createAccount(accNo, holder, initBal);
                    }
                    case 2 -> {
                        System.out.print("Enter Account Number: ");
                        String accNo = sc.nextLine();

                        System.out.print("Enter Amount to Deposit: ");
                        double depositAmt = sc.nextDouble();
                        sc.nextLine();

                        bank.deposit(accNo, depositAmt);
                    }
                    case 3 -> {
                        System.out.print("Enter Account Number: ");
                        String accNo = sc.nextLine();

                        System.out.print("Enter Amount to Withdraw: ");
                        double withdrawAmt = sc.nextDouble();
                        sc.nextLine();

                        bank.withdraw(accNo, withdrawAmt);
                    }
                    case 4 -> {
                        System.out.print("Enter Account Number: ");
                        String accNo = sc.nextLine();
                        bank.checkBalance(accNo);
                    }
                    case 5 -> bank.displayAllAccounts();
                    case 6 -> {
                        System.out.println("👋 Thank you for using Banking Simulator. Goodbye!");
                        sc.close();
                        System.exit(0);
                    }
                    default -> System.out.println("❌ Invalid choice. Please try again.");
                }

            } catch (InputMismatchException e) {
                System.out.println("⚠️ Invalid input. Please enter valid numbers where required.");
                sc.nextLine(); // clear invalid input
            } catch (Exception e) {
                System.out.println("⚠️ An unexpected error occurred: " + e.getMessage());
            }
        }
    }
}
