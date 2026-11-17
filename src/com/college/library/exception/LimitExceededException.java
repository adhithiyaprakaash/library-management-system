package com.college.library.exception;

/** Thrown when a member already holds the maximum number of active loans. */
public class LimitExceededException extends LibraryException {
    public LimitExceededException(String memberId, int limit) {
        super("Member " + memberId + " has reached the limit of " + limit + " active loans");
    }
}
