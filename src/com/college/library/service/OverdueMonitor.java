package com.college.library.service;

import java.time.LocalDate;

/** Background task that periodically counts overdue loans so the menu can show an up-to-date warning. */
public class OverdueMonitor implements Runnable {
    private final LibraryService service;
    private final long intervalMillis;
    private volatile int overdueCount;

    public OverdueMonitor(LibraryService service, long intervalMillis) {
        this.service = service;
        this.intervalMillis = intervalMillis;
    }

    public void scan() { overdueCount = service.findOverdue(LocalDate.now()).size(); }

    public int getOverdueCount() { return overdueCount; }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            scan();
            try { Thread.sleep(intervalMillis); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public Thread start() {
        Thread t = new Thread(this, "overdue-monitor");
        t.setDaemon(true);      // never keeps the JVM alive after the menu exits
        t.start();
        return t;
    }
}
