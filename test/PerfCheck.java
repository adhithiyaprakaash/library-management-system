import com.college.library.model.*;
import com.college.library.service.*;
import com.college.library.storage.*;
import com.college.library.util.SearchField;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class PerfCheck {
    interface Op { void run() throws Exception; }
    static long median(Op op, int runs) throws Exception {
        long[] t = new long[runs];
        for (int i = 0; i < runs; i++) { long s = System.nanoTime(); op.run(); t[i] = (System.nanoTime() - s) / 1000; }
        Arrays.sort(t); return t[runs / 2];
    }
    public static void main(String[] a) throws Exception {
        Path dir = Files.createTempDirectory("lms-perf");
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= 10000; i++)
            lines.add(String.format("B%05d|%s Programming Volume %d|Author %d|Computer Science|5|5", i, (i % 7 == 0 ? "Java" : "Python"), i, i % 500));
        Files.write(dir.resolve("books.csv"), lines);
        List<String> ml = new ArrayList<>();
        for (int i = 1; i <= 500; i++) ml.add(String.format("M%05d|Reader %d|r%d@college.edu|FACULTY|2026-01-01", i, i, i));
        Files.write(dir.resolve("members.csv"), ml);
        List<String> ll = new ArrayList<>();
        for (int i = 1; i <= 5000; i++) {
            String ret = i % 5 == 0 ? "" : "2026-02-20";
            ll.add(String.format("L%04d|B%05d|M%05d|2026-02-01|2026-02-15|%s|%d", i, i, (i % 500) + 1, ret, ret.isEmpty() ? 0 : 6));
        }
        Files.write(dir.resolve("loans.csv"), ll);

        long load = median(() -> new BookRepository(dir.resolve("books.csv")), 5);
        BookRepository br = new BookRepository(dir.resolve("books.csv"));
        LibraryService svc = new LibraryService(br, new MemberRepository(dir.resolve("members.csv")),
                new LoanRepository(dir.resolve("loans.csv")), new StandardFinePolicy());
        for (int i = 0; i < 20; i++) svc.searchBooks("java", SearchField.ANY);   // warm-up
        long search = median(() -> svc.searchBooks("java", SearchField.ANY), 15);
        int[] n = {20000};
        long add = median(() -> svc.addBook("B" + (n[0]++), "Perf Book", "Perf Author", "Misc", "2"), 5);
        int[] m = {1};
        long issue = median(() -> svc.issueBook("B00001", String.format("M%05d", 2 + (m[0]++ % 4)), LocalDate.of(2026, 11, 2)), 5);
        ReportService rs = new ReportService(svc);
        long export = median(() -> rs.export(dir.resolve("rep"), LocalDate.of(2026, 11, 16)), 5);
        System.out.printf("load_10000_books_ms=%.1f%nsearch_10000_ms=%.2f%nadd_book_ms=%.1f%nissue_book_ms=%.1f%nexport_5000_loans_ms=%.1f%n",
                load / 1000.0, search / 1000.0, add / 1000.0, issue / 1000.0, export / 1000.0);
        System.out.println("java=" + System.getProperty("java.version") + " cpus=" + Runtime.getRuntime().availableProcessors());
    }
}
