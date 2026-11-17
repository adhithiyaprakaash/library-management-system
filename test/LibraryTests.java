import com.college.library.exception.*;
import com.college.library.model.*;
import com.college.library.service.*;
import com.college.library.storage.*;
import com.college.library.util.SearchField;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free test runner. Writes one tab-separated line per test case to test-results.tsv. */
public class LibraryTests {
    static final LocalDate D = LocalDate.of(2026, 11, 2);
    static final List<String> rows = new ArrayList<>();
    static int passed, failed;

    static class Fx {
        Path dir; BookRepository books; MemberRepository members; LoanRepository loans; LibraryService svc;
        Fx() throws IOException { dir = Files.createTempDirectory("lms-test"); load(); }
        void load() throws IOException {
            books = new BookRepository(dir.resolve("books.csv"));
            members = new MemberRepository(dir.resolve("members.csv"));
            loans = new LoanRepository(dir.resolve("loans.csv"));
            svc = new LibraryService(books, members, loans, new StandardFinePolicy());
        }
        Fx seed() throws Exception {
            svc.addBook("B001", "Data Structures in Java", "Robert Lafore", "Computer Science", "5");
            svc.addBook("B002", "Java: The Complete Reference", "Herbert Schildt", "Programming", "3");
            svc.addBook("B003", "Database System Concepts", "Silberschatz", "Database", "1");
            svc.addMember("M001", "Anitha Kumar", "anitha@college.edu", "STUDENT", D);
            svc.addMember("M002", "Rahul Menon", "rahul@college.edu", "STUDENT", D);
            svc.addMember("M003", "Dr. Suresh Babu", "suresh@college.edu", "FACULTY", D);
            return this;
        }
    }

    interface Body { String[] run(Fx fx) throws Exception; }   // {actual, "ok"|"fail"}
    interface Thrower { void run() throws Exception; }

    static String[] ok(String actual) { return new String[] { actual, "ok" }; }
    static String[] check(boolean cond, String actual) { return new String[] { actual, cond ? "ok" : "fail" }; }

    static String[] expect(Class<? extends Exception> type, Thrower t) {
        try { t.run(); return new String[] { "No exception thrown", "fail" }; }
        catch (Exception e) {
            String text = e.getClass().getSimpleName() + ": " + e.getMessage();
            return new String[] { text, type.isInstance(e) ? "ok" : "fail" };
        }
    }

    static void tc(String id, String scenario, String input, String expected, Body body) {
        String[] r;
        try { r = body.run(new Fx()); }
        catch (Throwable t) { r = new String[] { "Unexpected " + t, "fail" }; }
        boolean pass = r[1].equals("ok");
        if (pass) passed++; else failed++;
        rows.add(String.join("\t", id, scenario, input, expected, r[0].replace('\t', ' '), pass ? "Pass" : "FAIL"));
        System.out.println(id + " " + (pass ? "PASS" : "FAIL") + "  " + scenario + "  ->  " + r[0]);
    }

    public static void main(String[] args) throws Exception {
        tc("TC-01", "Add a valid book", "B001, 'Data Structures in Java', 5 copies",
           "Book saved with 5 total and 5 available copies", fx -> {
            Book b = fx.svc.addBook("B001", "Data Structures in Java", "Robert Lafore", "Computer Science", "5");
            return check(b.getTotalCopies() == 5 && b.getAvailableCopies() == 5 && fx.books.existsById("B001"),
                    "Saved: " + b.getId() + ", total " + b.getTotalCopies() + ", available " + b.getAvailableCopies());
        });
        tc("TC-02", "Add a duplicate book ID", "B001 added a second time",
           "DuplicateRecordException; no second record", fx -> {
            fx.seed();
            String[] r = expect(DuplicateRecordException.class, () -> fx.svc.addBook("B001", "Another", "X Y", "Misc", "2"));
            return check(r[1].equals("ok") && fx.svc.listBooks().size() == 3, r[0]);
        });
        tc("TC-03", "Invalid book ID format", "ID = X12", "ValidationException about the ID format", fx ->
            expect(ValidationException.class, () -> fx.svc.addBook("X12", "Title", "Author", "Cat", "2")));
        tc("TC-04", "Copies below the minimum (boundary)", "Copies = 0", "ValidationException (range 1 to 99)", fx ->
            expect(ValidationException.class, () -> fx.svc.addBook("B010", "Title", "Author", "Cat", "0")));
        tc("TC-05", "Non-numeric copies", "Copies = five", "ValidationException; NumberFormatException handled", fx ->
            expect(ValidationException.class, () -> fx.svc.addBook("B010", "Title", "Author", "Cat", "five")));
        tc("TC-06", "Copies at the upper boundary", "Copies = 99, then 100",
           "99 accepted, 100 rejected", fx -> {
            fx.svc.addBook("B011", "Large Stock", "Author One", "Cat", "99");
            String[] r = expect(ValidationException.class, () -> fx.svc.addBook("B012", "Too Large", "Author Two", "Cat", "100"));
            return check(r[1].equals("ok") && fx.books.existsById("B011") && !fx.books.existsById("B012"),
                    "99 accepted; 100 -> " + r[0]);
        });
        tc("TC-07", "Update author and total copies", "B001: author = 'R. Lafore', total = 8",
           "Author changed; available copies recalculated to 8", fx -> {
            fx.seed();
            Book b = fx.svc.updateBook("B001", "", "R. Lafore", "", "8");
            return check(b.getAuthor().equals("R. Lafore") && b.getTotalCopies() == 8 && b.getAvailableCopies() == 8,
                    "Author " + b.getAuthor() + ", total " + b.getTotalCopies() + ", available " + b.getAvailableCopies());
        });
        tc("TC-08", "Reduce total copies below copies on loan", "2 copies on loan, new total = 1",
           "ValidationException; record unchanged", fx -> {
            fx.seed();
            fx.svc.issueBook("B001", "M001", D);
            fx.svc.issueBook("B001", "M002", D);
            String[] r = expect(ValidationException.class, () -> fx.svc.updateBook("B001", "", "", "", "1"));
            return check(r[1].equals("ok") && fx.svc.getBook("B001").getTotalCopies() == 5, r[0]);
        });
        tc("TC-09", "Delete a book with no active loans", "Delete B003", "Book removed from catalogue", fx -> {
            fx.seed();
            fx.svc.deleteBook("B003");
            return check(!fx.books.existsById("B003") && fx.svc.listBooks().size() == 2,
                    "B003 deleted; " + fx.svc.listBooks().size() + " titles remain");
        });
        tc("TC-10", "Delete a book that is on loan", "Delete B001 while issued", "OperationNotAllowedException", fx -> {
            fx.seed();
            fx.svc.issueBook("B001", "M001", D);
            return expect(OperationNotAllowedException.class, () -> fx.svc.deleteBook("B001"));
        });
        tc("TC-11", "Register a valid member", "M004, 'Divya Raj', divya@college.edu, STUDENT",
           "Member saved with type STUDENT", fx -> {
            Member m = fx.svc.addMember("M004", "Divya Raj", "divya@college.edu", "student", D);
            return check(m.getType() == MemberType.STUDENT && fx.members.existsById("M004"),
                    "Saved: " + m.getId() + ", " + m.getName() + ", " + m.getType());
        });
        tc("TC-12", "Register a member with a duplicate e-mail", "anitha@college.edu used twice",
           "DuplicateRecordException", fx -> {
            fx.seed();
            return expect(DuplicateRecordException.class, () -> fx.svc.addMember("M009", "Other Person", "anitha@college.edu", "STUDENT", D));
        });
        tc("TC-13", "Register a member with an invalid e-mail", "e-mail = anitha@", "ValidationException", fx ->
            expect(ValidationException.class, () -> fx.svc.addMember("M005", "Anitha Kumar", "anitha@", "STUDENT", D)));
        tc("TC-14", "Search by title (partial, any case)", "Title contains 'JAVA'",
           "Two titles found, sorted by title", fx -> {
            fx.seed();
            List<Book> r = fx.svc.searchBooks("JAVA", SearchField.TITLE);
            return check(r.size() == 2 && r.get(0).getId().equals("B001"),
                    r.size() + " found: " + r.get(0).getTitle() + "; " + r.get(1).getTitle());
        });
        tc("TC-15", "Search by author", "Author contains 'schildt'", "B002 found", fx -> {
            fx.seed();
            List<Book> r = fx.svc.searchBooks("schildt", SearchField.AUTHOR);
            return check(r.size() == 1 && r.get(0).getId().equals("B002"), r.size() + " found: " + r.get(0).getId());
        });
        tc("TC-16", "Search by book ID", "ID contains 'B003'", "Database System Concepts found", fx -> {
            fx.seed();
            List<Book> r = fx.svc.searchBooks("B003", SearchField.ID);
            return check(r.size() == 1, r.size() + " found: " + r.get(0).getTitle());
        });
        tc("TC-17", "Search for a nonexistent book", "Title contains 'Quantum Cooking'",
           "Empty result, no exception", fx -> {
            fx.seed();
            List<Book> r = fx.svc.searchBooks("Quantum Cooking", SearchField.TITLE);
            return check(r.isEmpty(), r.size() + " results; no exception");
        });
        tc("TC-18", "Search text with regex special characters", "Search text = 'C++ ('",
           "Treated as literal text; no PatternSyntaxException", fx -> {
            fx.seed();
            List<Book> r = fx.svc.searchBooks("C++ (", SearchField.ANY);
            return check(r.isEmpty(), r.size() + " results; no exception");
        });
        tc("TC-19", "Issue an available book to a student", "B001 to M001 on 2 Nov 2026",
           "Loan L0001 created, due 16 Nov 2026, copies 5 -> 4", fx -> {
            fx.seed();
            Loan l = fx.svc.issueBook("B001", "M001", D);
            return check(l.getLoanId().equals("L0001") && l.getDueDate().equals(D.plusDays(14))
                    && fx.svc.getBook("B001").getAvailableCopies() == 4,
                    "Loan " + l.getLoanId() + ", due " + l.getDueDate() + ", available " + fx.svc.getBook("B001").getAvailableCopies());
        });
        tc("TC-20", "Issue a book with no copies left", "B003 (1 copy) to M001, then to M002",
           "BookNotAvailableException for the second request", fx -> {
            fx.seed();
            fx.svc.issueBook("B003", "M001", D);
            return expect(BookNotAvailableException.class, () -> fx.svc.issueBook("B003", "M002", D));
        });
        tc("TC-21", "Student exceeds the 3-loan limit", "4th issue to M001", "LimitExceededException", fx -> {
            fx.seed();
            fx.svc.addBook("B004", "Operating Systems", "Galvin", "Systems", "2");
            fx.svc.issueBook("B001", "M001", D);
            fx.svc.issueBook("B002", "M001", D);
            fx.svc.issueBook("B003", "M001", D);
            return expect(LimitExceededException.class, () -> fx.svc.issueBook("B004", "M001", D));
        });
        tc("TC-22", "Issue to an unregistered member", "Member M099", "RecordNotFoundException", fx -> {
            fx.seed();
            return expect(RecordNotFoundException.class, () -> fx.svc.issueBook("B001", "M099", D));
        });
        tc("TC-23", "Faculty loan period", "B001 to faculty M003", "Due date is 30 days after issue", fx -> {
            fx.seed();
            Loan l = fx.svc.issueBook("B001", "M003", D);
            return check(l.getDueDate().equals(D.plusDays(30)), "Due " + l.getDueDate());
        });
        tc("TC-24", "Return a book on the due date", "Loan L0001 returned on 16 Nov 2026",
           "Fine = Rs. 0; available copies restored", fx -> {
            fx.seed();
            Loan l = fx.svc.issueBook("B001", "M001", D);
            fx.svc.returnBook(l.getLoanId(), D.plusDays(14));
            return check(l.getFine() == 0 && fx.svc.getBook("B001").getAvailableCopies() == 5,
                    "Fine Rs. " + l.getFine() + ", available " + fx.svc.getBook("B001").getAvailableCopies());
        });
        tc("TC-25", "Return a book 5 days late", "Loan L0001 returned on 21 Nov 2026", "Fine = Rs. 10", fx -> {
            fx.seed();
            Loan l = fx.svc.issueBook("B001", "M001", D);
            fx.svc.returnBook(l.getLoanId(), D.plusDays(19));
            return check(l.getFine() == 10, "Fine Rs. " + l.getFine());
        });
        tc("TC-26", "Return the same loan twice", "L0001 returned again", "OperationNotAllowedException", fx -> {
            fx.seed();
            Loan l = fx.svc.issueBook("B001", "M001", D);
            fx.svc.returnBook(l.getLoanId(), D.plusDays(3));
            return expect(OperationNotAllowedException.class, () -> fx.svc.returnBook(l.getLoanId(), D.plusDays(4)));
        });
        tc("TC-27", "Return date earlier than issue date", "Returned 1 day before issue", "ValidationException", fx -> {
            fx.seed();
            Loan l = fx.svc.issueBook("B001", "M001", D);
            return expect(ValidationException.class, () -> fx.svc.returnBook(l.getLoanId(), D.minusDays(1)));
        });
        tc("TC-28", "Records survive an application restart", "Add, issue, then reload all files",
           "Same books, members and loans after reload", fx -> {
            fx.seed();
            fx.svc.issueBook("B001", "M001", D);
            fx.load();
            return check(fx.svc.listBooks().size() == 3 && fx.svc.listMembers().size() == 3
                    && fx.svc.activeLoans().size() == 1 && fx.svc.getBook("B001").getAvailableCopies() == 4,
                    fx.svc.listBooks().size() + " books, " + fx.svc.listMembers().size() + " members, "
                    + fx.svc.activeLoans().size() + " active loan; B001 available = " + fx.svc.getBook("B001").getAvailableCopies());
        });
        tc("TC-29", "Damaged line in the data file", "books.csv contains 'garbage' line",
           "Bad line skipped; valid records still load", fx -> {
            fx.seed();
            Files.writeString(fx.dir.resolve("books.csv"),
                    Files.readString(fx.dir.resolve("books.csv")) + "this is not a valid record\n");
            fx.load();
            return check(fx.books.getSkippedLines() == 1 && fx.svc.listBooks().size() == 3,
                    fx.svc.listBooks().size() + " books loaded, " + fx.books.getSkippedLines() + " line skipped");
        });
        tc("TC-30", "Concurrent issue of the last copy", "8 threads request the only copy, 20 rounds",
           "Exactly one success per round", fx -> {
            fx.svc.addBook("B050", "Single Copy Title", "Some Author", "Misc", "1");
            for (int i = 1; i <= 8; i++) fx.svc.addMember(String.format("M%03d", 100 + i), "Reader " + (char) ('A' + i),
                    "reader" + i + "@college.edu", "FACULTY", D);
            int perfectRounds = 0;
            for (int round = 0; round < 20; round++) {
                ExecutorService pool = Executors.newFixedThreadPool(8);
                CountDownLatch go = new CountDownLatch(1);
                AtomicInteger success = new AtomicInteger();
                List<Future<?>> fs = new ArrayList<>();
                for (int i = 1; i <= 8; i++) {
                    final String member = String.format("M%03d", 100 + i);
                    fs.add(pool.submit(() -> {
                        try { go.await(); fx.svc.issueBook("B050", member, D); success.incrementAndGet(); }
                        catch (BookNotAvailableException expected) { }
                        catch (Exception other) { throw new RuntimeException(other); }
                    }));
                }
                go.countDown();
                for (Future<?> f : fs) f.get();
                pool.shutdown();
                if (success.get() == 1 && fx.svc.getBook("B050").getAvailableCopies() == 0) perfectRounds++;
                for (Loan l : fx.svc.activeLoans()) fx.svc.returnBook(l.getLoanId(), D);
            }
            return check(perfectRounds == 20, perfectRounds + " of 20 rounds: exactly one thread succeeded");
        });
        tc("TC-31", "Overdue loan detection", "Loan due 16 Nov 2026, checked on 20 Nov 2026",
           "Loan listed as overdue; accrued fine Rs. 8", fx -> {
            fx.seed();
            Loan l = fx.svc.issueBook("B001", "M001", D);
            List<Loan> od = fx.svc.findOverdue(D.plusDays(18));
            return check(od.size() == 1 && fx.svc.accruedFine(l, D.plusDays(18)) == 8,
                    od.size() + " overdue loan; accrued fine Rs. " + fx.svc.accruedFine(l, D.plusDays(18)));
        });
        tc("TC-32", "Export the library report to a file", "Reports > Export",
           "File created and contains the inventory summary", fx -> {
            fx.seed();
            fx.svc.issueBook("B001", "M001", D);
            Path p = new ReportService(fx.svc).export(fx.dir.resolve("reports"), D);
            String text = Files.readString(p);
            return check(Files.exists(p) && text.contains("Titles: 3 | Total copies: 9 | Available: 8 | On loan: 1"),
                    p.getFileName() + " written; first summary line: Titles: 3 | Total copies: 9 | Available: 8 | On loan: 1");
        });
        tc("TC-33", "Data folder becomes unavailable while saving", "Folder deleted, then add a book",
           "StorageException; memory and file stay consistent", fx -> {
            fx.seed();
            for (Path p : Files.list(fx.dir).toArray(Path[]::new)) Files.delete(p);
            Files.delete(fx.dir);
            String[] r = expect(StorageException.class, () -> fx.svc.addBook("B090", "Ghost Book", "No One", "Misc", "1"));
            return check(r[1].equals("ok") && !fx.books.existsById("B090"), r[0].substring(0, Math.min(r[0].length(), 60)) + "...");
        });

        tc("TC-34", "Title containing the file separator", "Title = 'A|B'", "ValidationException; nothing stored", fx -> {
            String[] r = expect(ValidationException.class, () -> fx.svc.addBook("B060", "A|B", "Author", "Cat", "1"));
            return check(r[1].equals("ok") && fx.svc.listBooks().isEmpty(), r[0]);
        });
        tc("TC-35", "Update a member", "M001: new name and type FACULTY", "Details changed and saved to the file", fx -> {
            fx.seed();
            fx.svc.updateMember("M001", "Anitha K. Raj", "", "FACULTY");
            fx.load();
            Member m = fx.svc.getMember("M001");
            return check(m.getName().equals("Anitha K. Raj") && m.getType() == MemberType.FACULTY,
                    "After reload: " + m.getName() + ", " + m.getType());
        });
        tc("TC-36", "Delete a member who holds a book", "Delete M001 while a book is issued", "OperationNotAllowedException", fx -> {
            fx.seed();
            fx.svc.issueBook("B001", "M001", D);
            return expect(OperationNotAllowedException.class, () -> fx.svc.deleteMember("M001"));
        });
        tc("TC-37", "Check availability", "B003 (1 copy) before and after issue", "1 of 1, then 0 of 1 copies available", fx -> {
            fx.seed();
            String before = fx.svc.availability("B003");
            fx.svc.issueBook("B003", "M001", D);
            String after = fx.svc.availability("B003");
            return check(before.endsWith("1 of 1 copies available") && after.endsWith("0 of 1 copies available"),
                    before.substring(before.indexOf(':') + 2) + "; then " + after.substring(after.indexOf(':') + 2));
        });
        tc("TC-38", "Background overdue monitor thread", "Loan overdue; monitor started with 50 ms interval",
           "Monitor thread reports 1 overdue loan within one second", fx -> {
            fx.seed();
            fx.svc.issueBook("B001", "M001", LocalDate.now().minusDays(30));
            OverdueMonitor mon = new OverdueMonitor(fx.svc, 50);
            Thread t = mon.start();
            long end = System.currentTimeMillis() + 1000;
            while (mon.getOverdueCount() == 0 && System.currentTimeMillis() < end) Thread.sleep(10);
            boolean daemon = t.isDaemon();
            t.interrupt();
            return check(mon.getOverdueCount() == 1 && daemon, "Count = " + mon.getOverdueCount() + "; daemon thread = " + daemon);
        });

        Files.write(Paths.get("test-results.tsv"), rows);
        System.out.println("\nTotal " + (passed + failed) + ", passed " + passed + ", failed " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
