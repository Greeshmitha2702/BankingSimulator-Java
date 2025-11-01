package com.bank.app;

import com.bank.dao.Database;
import com.bank.service.AuthService;
import com.bank.service.Bank;
import com.bank.service.TransactionService;

import java.io.Console;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Bank bank = new Bank();
        AuthService auth = new AuthService();

        // ✅ Ensure tables exist
        Database.createTableIfNotExists();

        System.out.println("🏦 Welcome to Banking Simulator 🏦");

        String username = "";

        // ------------------------
        // 🧩 LOGIN / REGISTER FLOW + MAIN BANKING MENU
        // ------------------------
        while (true) {
            System.out.println("\n1️⃣ Login");
            System.out.println("2️⃣ Register (New User)");
            System.out.println("3️⃣ Create Bank Account");
            System.out.println("4️⃣ Exit");
            System.out.println("5️⃣ Forgot Password");
            System.out.print("Enter your choice: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> {
                    System.out.print("👤 Enter username: ");
                    username = sc.nextLine();

                    if (!auth.userExists(username)) {
                        System.out.println("❌ Invalid username!");
                        break;
                    }

                    String password = readPassword("🔑 Enter password: ");
                    boolean isLoggedIn = auth.loginUser(username, password);

                    if (isLoggedIn) {
                        // ✅ Banking menu after login
                        bankingMenu(sc, bank, auth, username);
                    }
                }

                case "2" -> {
                    System.out.print("👤 Choose a username: ");
                    username = sc.nextLine();

                    System.out.print("🏦 Enter your Account Number to link: ");
                    String accountNumber = sc.nextLine();

                    if (!bank.accountExists(accountNumber)) {
                        System.out.println("❌ Invalid Account Number. Please create a bank account first.");
                        break;
                    }

                    String password = readPassword("🔑 Enter password: ");
                    String confirmPassword = readPassword("🔑 Confirm password: ");

                    if (!password.equals(confirmPassword)) {
                        System.out.println("❌ Passwords do not match. Try again!");
                        break;
                    }

                    boolean registered = auth.registerUser(username, password, accountNumber);
                    if (registered) {
                        System.out.println("✅ Registration successful! You can now log in.");
                    }
                }

                case "3" -> {
                    createBankAccount(sc, bank);
                }

                case "4" -> {
                    System.out.println("👋 Thank you for using the Banking Simulator. Goodbye!");
                    return;
                }

                case "5" -> {
                    // Forgot password BEFORE login
                    System.out.print("Enter your bank account number to reset password: ");
                    String accNo = sc.nextLine();
                    boolean ok = auth.resetPasswordAndEmail(accNo);
                    if (ok) {
                        System.out.println("📧 A temporary password has been emailed to your account's registered email (if available).");
                        System.out.println("🔑 Use it to login and then change your password.");
                    } else {
                        System.out.println("❌ Could not reset password. Make sure the account number is correct and has a registered email.");
                    }
                }

                default -> System.out.println("❌ Invalid choice. Please try again.");
            }
        }
    }

    // ------------------------
    // 💰 Banking Menu
    // ------------------------
    private static void bankingMenu(Scanner sc, Bank bank, AuthService auth, String username) {
        while (true) {
            try {
                System.out.println("\nChoose an option:");
                System.out.println("1️⃣ Deposit Money");
                System.out.println("2️⃣ Withdraw Money");
                System.out.println("3️⃣ Check Balance");
                System.out.println("4️⃣ Display All Accounts");
                System.out.println("5️⃣ View Transaction History");
                System.out.println("6️⃣ Transfer Money");
                System.out.println("7️⃣ Logout");
                System.out.println("8️⃣ Generate Account Report");
                System.out.println("9️⃣ Set Alert Threshold");
                System.out.println("🔟 Update Account Details");
                System.out.println("1️⃣1️⃣ Delete Account");
                System.out.print("Enter your choice: ");

                int choice = sc.nextInt();
                sc.nextLine(); // consume newline

                switch (choice) {
                    case 1 -> { // Deposit
                        String accNo = auth.getLinkedAccount(username); // use linked account
                        System.out.println("💳 Your Account Number: " + accNo);
                        System.out.print("Enter Amount to Deposit: ");
                        double depositAmt = sc.nextDouble();
                        sc.nextLine();
                        bank.deposit(accNo, depositAmt);
                    }

                    case 2 -> { // Withdraw (now asks for login password inside Bank.withdraw)
                        String accNo = auth.getLinkedAccount(username);
                        System.out.println("💳 Your Account Number: " + accNo);
                        System.out.print("Enter Amount to Withdraw: ");
                        double withdrawAmt = sc.nextDouble();
                        sc.nextLine();
                        bank.withdraw(accNo, withdrawAmt, username);
                    }

                    case 3 -> { // Check Balance
                        String accNo = auth.getLinkedAccount(username);
                        bank.checkBalance(accNo);
                    }

                    case 4 -> bank.displayAllAccounts();

                    case 5 -> { // View Transaction History
                        String accNo = auth.getLinkedAccount(username); // use logged-in account
                        System.out.println("💳 Your Account Number: " + accNo);
                        TransactionService ts = new TransactionService();
                        var transactions = ts.getTransactions(accNo);
                        if (transactions.isEmpty())
                            System.out.println("⚠️ No transactions yet.");
                        else {
                            System.out.println("--- Transaction History ---");
                            transactions.forEach(System.out::println);
                        }
                    }

                    case 6 -> { // Transfer
                        String fromAcc = auth.getLinkedAccount(username); // logged-in account
                        System.out.println("💳 Your Account Number: " + fromAcc);
                        String toAcc;

                        while (true) {
                            System.out.print("Enter Target Account Number: ");
                            toAcc = sc.nextLine();

                            if (fromAcc.equals(toAcc)) {
                                System.out.println("❌ Cannot transfer to the same account. Please choose another account.");
                                continue;
                            }

                            if (!bank.accountExists(toAcc)) {
                                System.out.println("❌ Target account not found. Please enter a valid account number.");
                                continue;
                            }

                            break; // valid target account
                        }

                        System.out.print("Enter Amount: ");
                        double amt = sc.nextDouble();
                        sc.nextLine();
                        bank.transfer(fromAcc, toAcc, amt);
                    }

                    case 7 -> {
                        System.out.println("👋 Logged out successfully. Goodbye " + username + "!");
                        return;
                    }
                    case 8 -> {
                        System.out.println("📊 Generating your account report...");
                        String accNo = auth.getLinkedAccount(username);
                        bank.generateReport(accNo);
                    }
                    case 9 -> {
                        String accNo = auth.getLinkedAccount(username);
                        System.out.print("Enter new alert threshold amount (₹): ");
                        double threshold = sc.nextDouble();
                        sc.nextLine(); // consume newline
                        bank.setAlertThreshold(accNo, threshold);
                    }
                    case 10 -> {
                        String accNo = auth.getLinkedAccount(username); // use linked account
                        System.out.println("💳 Your Account Number: " + accNo);
                        bank.updateAccountDetails(accNo,username);
                    }
                    case 11 -> {
                        String accNo = auth.getLinkedAccount(username);
                        boolean deleted = bank.deleteAccount(accNo, username);
                        if (deleted) {
                            System.out.println("🚪 You have been logged out as your account is deleted.");
                            username = null;
                            accNo = null;
                            return; // or break from loop
                        }

                    }
                    default -> System.out.println("❌ Invalid choice. Try again.");
                }

            } catch (InputMismatchException e) {
                System.out.println("⚠️ Invalid input. Please enter valid numbers where required.");
                sc.nextLine();
            } catch (Exception e) {
                System.out.println("⚠️ Unexpected error: " + e.getMessage());
            }
        }
    }

    // ------------------------
    // 🏦 Bank Account Creation
    // ------------------------
    private static void createBankAccount(Scanner sc, Bank bank) {
        System.out.println("\n🏦 Creating a new bank account...");
        String holderName;
        while (true) {
            System.out.print("Enter Account Holder Name: ");
            holderName = sc.nextLine();
            if (bank.isValidName(holderName)) break;
            System.out.println("❌ Invalid name. Only alphabets allowed.");
        }

        String phone;
        while (true) {
            System.out.print("Enter Phone Number: ");
            phone = sc.nextLine();
            if (bank.isValidPhone(phone)) break;
            System.out.println("❌ Invalid phone number. Must be exactly 10 digits.");
        }

        // Email input + validation
        String email;
        while (true) {
            System.out.print("Enter Email: ");
            email = sc.nextLine().trim();
            if (email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                break;
            } else {
                System.out.println("❌ Invalid email. Please enter a valid email address (contains '@' and domain).");
            }
        }

        double initDeposit;
        while (true) {
            try {
                System.out.print("Enter Initial Deposit: ");
                initDeposit = Double.parseDouble(sc.nextLine());
                if (bank.isPositive(initDeposit)) break;
                System.out.println("❌ Invalid amount. Must be greater than 0.");
            } catch (Exception e) {
                System.out.println("❌ Invalid input. Enter numbers only.");
            }
        }

        bank.createAccount(holderName, phone, initDeposit, email);
    }

    // ------------------------
    // ✅ PASSWORD METHODS
    // ------------------------
    private static String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword(prompt);
            return new String(passwordChars);
        } else {
            System.out.print(prompt);
            return new java.util.Scanner(System.in).nextLine();
        }
    }
}
