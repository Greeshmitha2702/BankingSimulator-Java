package com.bank.report;

import com.bank.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

    public static void generatePDFReport(Account account, List<String[]> transactions) {
        String folderPath = "reports";
        File folder = new File(folderPath);
        if (!folder.exists()) folder.mkdirs();

        String fileName = folderPath + "/report_" + account.getAccountNumber() + ".pdf";

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Bank Account Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Account details
            document.add(new Paragraph("Account Holder: " + account.getAccountHolder()));
            document.add(new Paragraph("Account Number: " + account.getAccountNumber()));
            document.add(new Paragraph("Phone: " + account.getPhone()));
            document.add(new Paragraph("Current Balance: ₹" + account.getBalance()));
            document.add(new Paragraph("\n"));

            // Table header
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("Date");
            table.addCell("Type");
            table.addCell("Amount");
            table.addCell("Target Account");

            double totalDeposits = 0, totalWithdrawals = 0, totalTransfers = 0;

            for (String[] tx : transactions) {
                table.addCell(tx[0]);
                table.addCell(tx[1]);
                table.addCell("₹" + tx[2]);
                table.addCell(tx[3] == null ? "-" : tx[3]);

                String type = tx[1].toLowerCase();
                double amt = Double.parseDouble(tx[2]);
                if (type.equals("deposit")) totalDeposits += amt;
                else if (type.equals("withdraw")) totalWithdrawals += amt;
                else if (type.equals("transfer")) totalTransfers += amt;
            }

            document.add(table);
            document.add(new Paragraph("\nSummary:\n"));
            document.add(new Paragraph("Total Deposits: ₹" + totalDeposits));
            document.add(new Paragraph("Total Withdrawals: ₹" + totalWithdrawals));
            document.add(new Paragraph("Total Transfers: ₹" + totalTransfers));

            document.close();
            logger.info("✅ PDF report generated successfully at {}", fileName);
            System.out.println("📄 PDF report generated successfully: " + fileName);

        } catch (Exception e) {
            logger.error("❌ Error generating PDF report for {}", account.getAccountNumber(), e);
            System.out.println("❌ Failed to generate report: " + e.getMessage());
        }
    }
}
