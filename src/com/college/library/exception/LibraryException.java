package com.college.library.exception;

/** Base checked exception for every business-rule or storage failure in the system. */
public class LibraryException extends Exception {
    public LibraryException(String message) { super(message); }
    public LibraryException(String message, Throwable cause) { super(message, cause); }
}
