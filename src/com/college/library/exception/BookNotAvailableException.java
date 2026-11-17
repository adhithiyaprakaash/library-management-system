package com.college.library.exception;

/** Thrown when every copy of a title is already on loan. */
public class BookNotAvailableException extends LibraryException {
    public BookNotAvailableException(String bookId, String title) {
        super("No copy of '" + title + "' (" + bookId + ") is currently available");
    }
}
