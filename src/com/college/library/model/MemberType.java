package com.college.library.model;

/** Member categories with their own loan limit and loan period. */
public enum MemberType {
    STUDENT(3, 14),
    FACULTY(5, 30);

    private final int maxLoans;
    private final int loanDays;

    MemberType(int maxLoans, int loanDays) {
        this.maxLoans = maxLoans;
        this.loanDays = loanDays;
    }

    public int getMaxLoans() { return maxLoans; }
    public int getLoanDays() { return loanDays; }
}
