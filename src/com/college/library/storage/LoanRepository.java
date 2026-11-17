package com.college.library.storage;

import com.college.library.model.Loan;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class LoanRepository extends CsvRepository<Loan> {
    public LoanRepository(Path file) throws IOException { super(file); }

    @Override protected String idOf(Loan l) { return l.getLoanId(); }

    @Override protected String[] toFields(Loan l) {
        return new String[] { l.getLoanId(), l.getBookId(), l.getMemberId(), l.getIssueDate().toString(),
                l.getDueDate().toString(), l.getReturnDate() == null ? "" : l.getReturnDate().toString(),
                String.valueOf(l.getFine()) };
    }

    @Override protected Loan fromFields(String[] f) {
        LocalDate returned = f[5].isEmpty() ? null : LocalDate.parse(f[5]);
        return new Loan(f[0], f[1], f[2], LocalDate.parse(f[3]), LocalDate.parse(f[4]), returned, Integer.parseInt(f[6]));
    }

    public List<Loan> activeLoansOfMember(String memberId) {
        return findAll().stream().filter(l -> l.isActive() && l.getMemberId().equals(memberId)).collect(Collectors.toList());
    }

    public List<Loan> activeLoansOfBook(String bookId) {
        return findAll().stream().filter(l -> l.isActive() && l.getBookId().equals(bookId)).collect(Collectors.toList());
    }
}
