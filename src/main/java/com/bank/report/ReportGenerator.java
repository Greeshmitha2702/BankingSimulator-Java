package com.bank.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

    public static void generateCSVReport(String accountNumber, String holder, double balance, List<String[]> transactions) {
        String fileName = "report_" + accountNumber + ".csv";

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.append("Account Holder,").append(holder).append("\n");
            writer.append("Account Number,").append(accountNumber).append("\n");
            writer.append("Current Balance,₹").append(String.valueOf(balance)).append("\n\n");

            writer.append("Date,Type,Amount,Target Account\n");

            double totalDeposits = 0, totalWithdrawals = 0, totalTransfers = 0;

            for (String[] tx : transactions) {
                writer.append(String.join(",", tx)).append("\n");

                String type = tx[1];
                double amt = Double.parseDouble(tx[2]);
                if (type.equalsIgnoreCase("deposit")) totalDeposits += amt;
                else if (type.equalsIgnoreCase("withdraw")) totalWithdrawals += amt;
                else if (type.equalsIgnoreCase("transfer")) totalTransfers += amt;
            }

            writer.append("\nSummary,,,\n");
            writer.append("Total Deposits,₹").append(String.valueOf(totalDeposits)).append("\n");
            writer.append("Total Withdrawals,₹").append(String.valueOf(totalWithdrawals)).append("\n");
            writer.append("Total Transfers,₹").append(String.valueOf(totalTransfers)).append("\n");

            logger.info("CSV report generated successfully for account {}", accountNumber);
        } catch (IOException e) {
            logger.error("Error writing CSV report for {}", accountNumber, e);
        }
    }
}
