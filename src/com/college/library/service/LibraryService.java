package com.college.library.service;

import com.college.library.exception.*;
import com.college.library.model.*;
import com.college.library.storage.*;
import com.college.library.util.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business-rule layer. Every public method is synchronized on the service object, so two threads can never
 * interleave a check ("is a copy available?") with the update that depends on it.
 */
public class LibraryService {
    @FunctionalInterface
    private interface IoAction { void run() throws IOException; }

    private final BookRepository books;
    private final MemberRepository members;
    private final LoanRepository loans;
    private final FinePolicy finePolicy;
    private int loanSequence;

    public LibraryService(BookRepository books, MemberRepository members, LoanRepository loans, FinePolicy finePolicy) {
        this.books = books;
        this.members = members;
        this.loans = loans;
        this.finePolicy = finePolicy;
        this.loanSequence = loans.findAll().stream().mapToInt(l -> Loan.sequenceOf(l.getLoanId())).max().orElse(0);
    }

    // ---------------------------------------------------------------- books
    public synchronized Book addBook(String id, String title, String author, String category, String copies)
            throws LibraryException {
        String bookId = Validator.bookId(id);
        String t = Validator.text(title, "Title");
        String a = Validator.text(author, "Author");
        String c = Validator.text(category, "Category");
        int n = Validator.copies(copies);
        if (books.existsById(bookId)) throw new DuplicateRecordException("Book ID " + bookId + " already exists");
        Book book = new Book(bookId, t, a, c, n);
        persist(() -> books.save(book));
        return book;
    }

    /** A blank argument means "keep the current value". */
    public synchronized Book updateBook(String id, String title, String author, String category, String total)
            throws LibraryException {
        Book book = getBook(id);
        if (!title.isBlank()) book.setTitle(Validator.text(title, "Title"));
        if (!author.isBlank()) book.setAuthor(Validator.text(author, "Author"));
        if (!category.isBlank()) book.setCategory(Validator.text(category, "Category"));
        if (!total.isBlank()) book.changeTotalCopies(Validator.copies(total));
        persist(() -> books.save(book));
        return book;
    }

    public synchronized void deleteBook(String id) throws LibraryException {
        Book book = getBook(id);
        if (!loans.activeLoansOfBook(book.getId()).isEmpty())
            throw new OperationNotAllowedException("Book " + book.getId() + " has copies on loan and cannot be deleted");
        persist(() -> books.deleteById(book.getId()));
    }

    public synchronized Book getBook(String id) throws LibraryException {
        String bookId = Validator.bookId(id);
        return books.findById(bookId).orElseThrow(() -> new RecordNotFoundException("Book " + bookId + " not found"));
    }

    public synchronized List<Book> listBooks() {
        return books.findAll().stream().sorted(Comparator.comparing(Book::getId)).collect(Collectors.toList());
    }

    public synchronized List<Book> searchBooks(String query, SearchField field) {
        return SearchUtil.search(books.findAll(), query, field);
    }

    public synchronized String availability(String id) throws LibraryException {
        Book b = getBook(id);
        return String.format("%s (%s): %d of %d copies available", b.getTitle(), b.getId(),
                b.getAvailableCopies(), b.getTotalCopies());
    }

    // -------------------------------------------------------------- members
    public synchronized Member addMember(String id, String name, String email, String type, LocalDate joined)
            throws LibraryException {
        String memberId = Validator.memberId(id);
        String n = Validator.name(name);
        String e = Validator.email(email);
        MemberType t = Validator.memberType(type);
        if (members.existsById(memberId)) throw new DuplicateRecordException("Member ID " + memberId + " already exists");
        boolean emailUsed = members.findAll().stream().anyMatch(m -> m.getEmail().equalsIgnoreCase(e));
        if (emailUsed) throw new DuplicateRecordException("E-mail " + e + " is already registered");
        Member member = new Member(memberId, n, e, t, joined);
        persist(() -> members.save(member));
        return member;
    }

    public synchronized Member updateMember(String id, String name, String email, String type) throws LibraryException {
        Member m = getMember(id);
        if (!name.isBlank()) m.setName(Validator.name(name));
        if (!email.isBlank()) m.setEmail(Validator.email(email));
        if (!type.isBlank()) m.setType(Validator.memberType(type));
        persist(() -> members.save(m));
        return m;
    }

    public synchronized void deleteMember(String id) throws LibraryException {
        Member m = getMember(id);
        if (!loans.activeLoansOfMember(m.getId()).isEmpty())
            throw new OperationNotAllowedException("Member " + m.getId() + " still holds issued books");
        persist(() -> members.deleteById(m.getId()));
    }

    public synchronized Member getMember(String id) throws LibraryException {
        String memberId = Validator.memberId(id);
        return members.findById(memberId).orElseThrow(() -> new RecordNotFoundException("Member " + memberId + " is not registered"));
    }

    public synchronized List<Member> listMembers() {
        return members.findAll().stream().sorted(Comparator.comparing(Member::getId)).collect(Collectors.toList());
    }

    // ---------------------------------------------------------- issue/return
    public synchronized Loan issueBook(String bookId, String memberId) throws LibraryException {
        return issueBook(bookId, memberId, LocalDate.now());
    }

    public synchronized Loan issueBook(String bookId, String memberId, LocalDate issueDate) throws LibraryException {
        Member member = getMember(memberId);
        Book book = getBook(bookId);
        if (!book.isAvailable()) throw new BookNotAvailableException(book.getId(), book.getTitle());
        int active = loans.activeLoansOfMember(member.getId()).size();
        if (active >= member.getType().getMaxLoans())
            throw new LimitExceededException(member.getId(), member.getType().getMaxLoans());

        Loan loan = new Loan(nextLoanId(), book.getId(), member.getId(), issueDate,
                issueDate.plusDays(member.getType().getLoanDays()));
        book.issueCopy();
        try {
            persist(() -> loans.save(loan));
            persist(() -> books.save(book));
        } catch (LibraryException failure) {          // undo the in-memory change if either file write failed
            book.returnCopy();
            try { loans.deleteById(loan.getLoanId()); } catch (IOException ignored) { }
            throw failure;
        }
        return loan;
    }

    public synchronized Loan returnBook(String loanId, LocalDate returnDate) throws LibraryException {
        String id = loanId == null ? "" : loanId.trim().toUpperCase();
        Loan loan = loans.findById(id).orElseThrow(() -> new RecordNotFoundException("Loan " + id + " not found"));
        if (!loan.isActive())
            throw new OperationNotAllowedException("Loan " + id + " was already returned on " + loan.getReturnDate());
        if (returnDate.isBefore(loan.getIssueDate()))
            throw new ValidationException("Return date cannot be earlier than the issue date " + loan.getIssueDate());
        Book book = books.findById(loan.getBookId())
                .orElseThrow(() -> new RecordNotFoundException("Book " + loan.getBookId() + " is missing from the catalogue"));

        loan.close(returnDate, finePolicy.calculateFine(loan.getDueDate(), returnDate));
        book.returnCopy();
        try {
            persist(() -> loans.save(loan));
            persist(() -> books.save(book));
        } catch (LibraryException failure) {
            loan.reopen();
            book.issueCopy();
            throw failure;
        }
        return loan;
    }

    public synchronized Loan returnBook(String loanId) throws LibraryException {
        return returnBook(loanId, LocalDate.now());
    }

    // ---------------------------------------------------------------- queries
    public synchronized List<Loan> listLoans() {
        return loans.findAll().stream().sorted(Comparator.comparing(Loan::getLoanId)).collect(Collectors.toList());
    }

    public synchronized List<Loan> activeLoans() {
        return listLoans().stream().filter(Loan::isActive).collect(Collectors.toList());
    }

    public synchronized List<Loan> findOverdue(LocalDate today) {
        return activeLoans().stream().filter(l -> l.getDueDate().isBefore(today)).collect(Collectors.toList());
    }

    public int accruedFine(Loan loan, LocalDate today) { return finePolicy.calculateFine(loan.getDueDate(), today); }

    // ---------------------------------------------------------------- helpers
    private String nextLoanId() { return String.format("%s%04d", Loan.ID_PREFIX, ++loanSequence); }

    private void persist(IoAction action) throws StorageException {
        try { action.run(); }
        catch (IOException e) { throw new StorageException("Could not write to the data files: " + e.getMessage(), e); }
    }
}
