package com.college.library.model;

import com.college.library.exception.ValidationException;

public class Book extends LibraryItem {
    private String author;
    private String category;
    private int totalCopies;
    private int availableCopies;

    public Book(String id, String title, String author, String category, int totalCopies) {
        this(id, title, author, category, totalCopies, totalCopies);
    }

    public Book(String id, String title, String author, String category, int total, int available) {
        super(id, title);
        this.author = author;
        this.category = category;
        this.totalCopies = total;
        this.availableCopies = available;
    }

    @Override public String getItemType() { return "Book"; }

    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public void setAuthor(String author) { this.author = author; }
    public void setCategory(String category) { this.category = category; }

    public boolean isAvailable() { return availableCopies > 0; }

    /** Copies are changed only through these guarded methods, never by direct field access. */
    public void issueCopy() { availableCopies--; }
    public void returnCopy() { availableCopies++; }

    public void changeTotalCopies(int newTotal) throws ValidationException {
        int onLoan = totalCopies - availableCopies;
        if (newTotal < onLoan) {
            throw new ValidationException("Total copies cannot be less than the " + onLoan + " copies on loan");
        }
        availableCopies = newTotal - onLoan;
        totalCopies = newTotal;
    }
}
