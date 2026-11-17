package com.college.library.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StandardFinePolicy implements FinePolicy {
    public static final int RATE_PER_DAY = 2;   // rupees

    @Override
    public int calculateFine(LocalDate dueDate, LocalDate returnDate) {
        long late = ChronoUnit.DAYS.between(dueDate, returnDate) - graceDays();
        return late > 0 ? (int) late * RATE_PER_DAY : 0;
    }
}
