package com.college.library.model;

import java.time.LocalDate;

public class Loan {
    public static final String ID_PREFIX = "L";

    private final String loanId;
    private final String bookId;
    private final String memberId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;   // null while the book is still out
    private int fine;

    public Loan(String loanId, String bookId, String memberId, LocalDate issueDate, LocalDate dueDate) {
        this(loanId, bookId, memberId, issueDate, dueDate, null, 0);
    }

    public Loan(String loanId, String bookId, String memberId, LocalDate issueDate,
                LocalDate dueDate, LocalDate returnDate, int fine) {
        this.loanId = loanId;
        this.bookId = bookId;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fine = fine;
    }

    public String getLoanId() { return loanId; }
    public String getBookId() { return bookId; }
    public String getMemberId() { return memberId; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public int getFine() { return fine; }
    public boolean isActive() { return returnDate == null; }

    public void close(LocalDate returned, int fineAmount) { this.returnDate = returned; this.fine = fineAmount; }
    public void reopen() { this.returnDate = null; this.fine = 0; }

    /** Extracts the numeric part of an id such as L0007 so the counter can resume after a restart. */
    public static int sequenceOf(String loanId) {
        try { return Integer.parseInt(loanId.substring(ID_PREFIX.length())); }
        catch (RuntimeException e) { return 0; }
    }
}
