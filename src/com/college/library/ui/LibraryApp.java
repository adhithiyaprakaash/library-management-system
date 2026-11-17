package com.college.library.ui;

import com.college.library.exception.LibraryException;
import com.college.library.model.*;
import com.college.library.service.*;
import com.college.library.storage.*;
import com.college.library.util.SearchField;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Console user interface: reads input, calls the service layer and prints results or error messages. */
public class LibraryApp {
    private final Scanner in = new Scanner(System.in);
    private final LibraryService service;
    private final ReportService reports;
    private final OverdueMonitor monitor;

    public LibraryApp(LibraryService service, OverdueMonitor monitor) {
        this.service = service;
        this.reports = new ReportService(service);
        this.monitor = monitor;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "data");
        LibraryService service = new LibraryService(new BookRepository(dir.resolve("books.csv")),
                new MemberRepository(dir.resolve("members.csv")), new LoanRepository(dir.resolve("loans.csv")),
                new StandardFinePolicy());
        OverdueMonitor monitor = new OverdueMonitor(service, 30_000);
        monitor.scan();
        monitor.start();
        new LibraryApp(service, monitor).run();
    }

    private void run() {
        try {
            while (true) {
                System.out.println();
                System.out.println("===== LIBRARY MANAGEMENT SYSTEM =====");
                System.out.println("Overdue loans right now: " + monitor.getOverdueCount());
                System.out.println("1. Book management      2. Member management    3. Search books");
                System.out.println("4. Issue book           5. Return book          6. Check availability");
                System.out.println("7. Reports              0. Exit");
                String choice = ask("Enter choice");
                if (choice.equals("0")) { System.out.println("Goodbye."); return; }
                try {
                    switch (choice) {
                        case "1": bookMenu(); break;
                        case "2": memberMenu(); break;
                        case "3": search(); break;
                        case "4": issue(); break;
                        case "5": giveBack(); break;
                        case "6": System.out.println(service.availability(ask("Book ID"))); break;
                        case "7": reportMenu(); break;
                        default: System.out.println("Please choose a number from 0 to 7.");
                    }
                } catch (LibraryException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (NoSuchElementException endOfInput) {
            System.out.println("Input closed. Goodbye.");
        }
    }

    private void bookMenu() throws LibraryException {
        String c = ask("1 Add  2 Update  3 Delete  4 List all");
        switch (c) {
            case "1":
                Book b = service.addBook(ask("Book ID"), ask("Title"), ask("Author"), ask("Category"), ask("Copies"));
                System.out.println("Book added: " + b + " (" + b.getTotalCopies() + (b.getTotalCopies() == 1 ? " copy)" : " copies)"));
                break;
            case "2":
                String id = ask("Book ID");
                Book u = service.updateBook(id, ask("New title (blank = keep)"), ask("New author (blank = keep)"),
                        ask("New category (blank = keep)"), ask("New total copies (blank = keep)"));
                System.out.println("Book updated: " + u);
                break;
            case "3": service.deleteBook(ask("Book ID")); System.out.println("Book deleted."); break;
            case "4": printBooks(service.listBooks()); break;
            default: System.out.println("Unknown option.");
        }
    }

    private void memberMenu() throws LibraryException {
        String c = ask("1 Register  2 Update  3 Delete  4 List all");
        switch (c) {
            case "1":
                Member m = service.addMember(ask("Member ID"), ask("Name"), ask("E-mail"),
                        ask("Type (STUDENT/FACULTY)"), LocalDate.now());
                System.out.println("Member registered: " + m.getName() + " (" + m.getId() + ", " + m.getType() + ")");
                break;
            case "2":
                Member u = service.updateMember(ask("Member ID"), ask("New name (blank = keep)"),
                        ask("New e-mail (blank = keep)"), ask("New type (blank = keep)"));
                System.out.println("Member updated: " + u.getName());
                break;
            case "3": service.deleteMember(ask("Member ID")); System.out.println("Member deleted."); break;
            case "4":
                System.out.printf("%-6s %-22s %-28s %s%n", "ID", "Name", "E-mail", "Type");
                for (Member x : service.listMembers())
                    System.out.printf("%-6s %-22s %-28s %s%n", x.getId(), x.getName(), x.getEmail(), x.getType());
                break;
            default: System.out.println("Unknown option.");
        }
    }

    private void search() {
        String f = ask("Search by 1 Title  2 Author  3 Book ID  4 Any");
        SearchField field = f.equals("1") ? SearchField.TITLE : f.equals("2") ? SearchField.AUTHOR
                : f.equals("3") ? SearchField.ID : SearchField.ANY;
        List<Book> found = service.searchBooks(ask("Search text"), field);
        if (found.isEmpty()) System.out.println("No matching books found."); else printBooks(found);
    }

    private void issue() throws LibraryException {
        Loan l = service.issueBook(ask("Book ID"), ask("Member ID"));
        System.out.println("Issued. Loan " + l.getLoanId() + ", due on " + l.getDueDate());
    }

    private void giveBack() throws LibraryException {
        Loan l = service.returnBook(ask("Loan ID"));
        System.out.println("Returned. Fine: Rs. " + l.getFine());
    }

    private void reportMenu() throws LibraryException {
        String c = ask("1 Inventory  2 Issued books  3 Fines  4 Member history  5 Export to file");
        LocalDate today = LocalDate.now();
        switch (c) {
            case "1": System.out.println(reports.inventorySummary()); break;
            case "2": System.out.print(reports.issuedBooksReport(today)); break;
            case "3": System.out.println(reports.finesReport()); break;
            case "4": System.out.print(reports.memberHistory(ask("Member ID"))); break;
            case "5": System.out.println("Report written to " + reports.export(Paths.get("reports"), today)); break;
            default: System.out.println("Unknown option.");
        }
    }

    private void printBooks(List<Book> books) {
        System.out.printf("%-6s %-32s %-22s %-17s %s%n", "ID", "Title", "Author", "Category", "Avail/Total");
        for (Book b : books)
            System.out.printf("%-6s %-32s %-22s %-17s %d/%d%n", b.getId(), b.getTitle(), b.getAuthor(),
                    b.getCategory(), b.getAvailableCopies(), b.getTotalCopies());
    }

    private String ask(String label) {
        System.out.print(label + ": ");
        return in.nextLine().trim();
    }
}
