package com.bank;

import com.bank.dao.Database;
import com.bank.service.Bank;

import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Bank bank = new Bank();

        // ✅ Ensure table exists in DB
        Database.createTableIfNotExists();

        // ✅ Load existing accounts from DB
        //List<Account> existingAccounts = bank.getAllAccountsFromDB();
        //if (!existingAccounts.isEmpty()) {
          //  System.out.println("📂 Previous accounts loaded from DB:");
            //existingAccounts.forEach(System.out::println);
        //}

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
                        // STEP 1: Account Holder Name
                        String holderName;
                        while (true) {
                            System.out.print("Enter Account Holder Name: ");
                            holderName = sc.nextLine();
                            if (bank.isValidName(holderName)) break;
                            System.out.println("❌ Invalid name. Only alphabets allowed.");
                        }

// STEP 2: Phone Number
                        String phone;
                        while (true) {
                            System.out.print("Enter Phone Number: ");
                            phone = sc.nextLine();
                            if (bank.isValidPhone(phone)) break;
                            System.out.println("❌ Invalid phone number. Must be exactly 10 digits.");
                        }

// STEP 3: Initial Deposit
                        double initDeposit;
                        while (true) {
                            try {
                                System.out.print("Enter Initial Deposit: ");
                                initDeposit = sc.nextDouble();
                                sc.nextLine(); // consume newline
                                if (bank.isPositive(initDeposit)) break;
                                System.out.println("❌ Invalid amount. Must be greater than 0.");
                            } catch (Exception e) {
                                System.out.println("❌ Invalid input. Enter numbers only.");
                                sc.nextLine(); // clear invalid input
                            }
                        }

// STEP 4: Create account
                        bank.createAccount(holderName, phone, initDeposit);


                    }
                    case 2 -> {
                        String accNo;
                        while (true) {
                            System.out.print("Enter Account Number: ");
                            accNo = sc.nextLine();
                            if (bank.accountExists(accNo)) break;
                            System.out.println("❌ Account not found. Please enter a valid account number.");
                        }

                        double depositAmt;
                        while (true) {
                            try {
                                System.out.print("Enter Amount to Deposit: ");
                                depositAmt = sc.nextDouble();
                                sc.nextLine();
                                if (bank.isPositive(depositAmt)) break;
                                System.out.println("❌ Invalid amount. Must be greater than 0.");
                            } catch (Exception e) {
                                System.out.println("❌ Invalid input. Enter numbers only.");
                                sc.nextLine(); // clear invalid input
                            }
                        }

                        bank.deposit(accNo, depositAmt);

                    }
                    case 3 -> {
                        String accNo;
                        while (true) {
                            System.out.print("Enter Account Number: ");
                            accNo = sc.nextLine();
                            if (bank.accountExists(accNo)) break;
                            System.out.println("❌ Account not found. Please enter a valid account number.");
                        }

                        double withdrawAmt;
                        while (true) {
                            try {
                                System.out.print("Enter Amount to Withdraw: ");
                                withdrawAmt = sc.nextDouble();
                                sc.nextLine();
                                if (bank.isPositive(withdrawAmt)) break;
                                System.out.println("❌ Invalid amount. Must be greater than 0.");
                            } catch (Exception e) {
                                System.out.println("❌ Invalid input. Enter numbers only.");
                                sc.nextLine();
                            }
                        }

                        bank.withdraw(accNo, withdrawAmt);

                    }
                    case 4 -> {
                        String accNo;
                        while (true) {
                            System.out.print("Enter Account Number: ");
                            accNo = sc.nextLine();
                            if (bank.accountExists(accNo)) break;
                            System.out.println("❌ Account not found. Please enter a valid account number.");
                        }

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
