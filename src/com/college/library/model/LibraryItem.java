package com.college.library.model;

/** Abstract parent of everything the library can lend. */
public abstract class LibraryItem {
    private final String id;
    private String title;

    protected LibraryItem(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    /** Each concrete item type reports its own label. */
    public abstract String getItemType();

    @Override
    public String toString() { return String.format("%s [%s] %s", getItemType(), id, title); }
}
