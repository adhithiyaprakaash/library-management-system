package com.college.library.exception;

/** Wraps an IOException so that the UI never depends on file-handling details. */
public class StorageException extends LibraryException {
    public StorageException(String message, Throwable cause) { super(message, cause); }
}
