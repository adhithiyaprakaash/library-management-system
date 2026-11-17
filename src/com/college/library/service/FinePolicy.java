package com.college.library.service;

import java.time.LocalDate;

/** Strategy interface: a different fine rule can be plugged in without changing LibraryService. */
public interface FinePolicy {
    int calculateFine(LocalDate dueDate, LocalDate returnDate);

    /** Default method: number of free days after the due date. */
    default int graceDays() { return 0; }
}
