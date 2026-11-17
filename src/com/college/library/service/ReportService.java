package com.college.library.service;

import com.college.library.exception.LibraryException;
import com.college.library.exception.StorageException;
import com.college.library.model.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final LibraryService service;

    public ReportService(LibraryService service) { this.service = service; }

    public String inventorySummary() {
        List<Book> books = service.listBooks();
        int titles = books.size();
        int copies = books.stream().mapToInt(Book::getTotalCopies).sum();
        int available = books.stream().mapToInt(Book::getAvailableCopies).sum();
        return String.format("Titles: %d | Total copies: %d | Available: %d | On loan: %d",
                titles, copies, available, copies - available);
    }

    public String issuedBooksReport(LocalDate today) {
        StringBuilder sb = new StringBuilder(String.format("%-7s %-6s %-6s %-12s %-12s %s%n",
                "Loan", "Book", "Member", "Issued", "Due", "Status"));
        for (Loan l : service.activeLoans()) {
            String status = l.getDueDate().isBefore(today)
                    ? "OVERDUE, fine so far Rs. " + service.accruedFine(l, today) : "On time";
            sb.append(String.format("%-7s %-6s %-6s %-12s %-12s %s%n", l.getLoanId(), l.getBookId(),
                    l.getMemberId(), l.getIssueDate(), l.getDueDate(), status));
        }
        if (service.activeLoans().isEmpty()) sb.append("No books are currently issued.").append(System.lineSeparator());
        return sb.toString();
    }

    public String finesReport() {
        int total = service.listLoans().stream().mapToInt(Loan::getFine).sum();
        long charged = service.listLoans().stream().filter(l -> l.getFine() > 0).count();
        return String.format("Fines charged on returned books: Rs. %d across %d loans", total, charged);
    }

    public String memberHistory(String memberId) throws LibraryException {
        Member m = service.getMember(memberId);
        StringBuilder sb = new StringBuilder("History of " + m.getName() + " (" + m.getId() + ")" + System.lineSeparator());
        service.listLoans().stream().filter(l -> l.getMemberId().equals(m.getId())).forEach(l ->
                sb.append(String.format("  %s  %s  issued %s, due %s, returned %s, fine Rs. %d%n", l.getLoanId(),
                        l.getBookId(), l.getIssueDate(), l.getDueDate(),
                        l.getReturnDate() == null ? "-" : l.getReturnDate().toString(), l.getFine())));
        return sb.toString();
    }

    /** Writes the full report to reports/library_report_YYYY-MM-DD.txt and returns the file path. */
    public Path export(Path folder, LocalDate today) throws StorageException {
        Path target = folder.resolve("library_report_" + today + ".txt");
        try {
            Files.createDirectories(folder);
            try (BufferedWriter out = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                out.write("LIBRARY MANAGEMENT SYSTEM - REPORT FOR " + today); out.newLine(); out.newLine();
                out.write(inventorySummary()); out.newLine(); out.newLine();
                out.write(issuedBooksReport(today)); out.newLine();
                out.write(finesReport()); out.newLine();
            }
        } catch (IOException e) {
            throw new StorageException("Report could not be written: " + e.getMessage(), e);
        }
        return target;
    }
}
