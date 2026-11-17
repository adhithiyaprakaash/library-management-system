package com.college.library.storage;

import com.college.library.model.Book;
import java.io.IOException;
import java.nio.file.Path;

public class BookRepository extends CsvRepository<Book> {
    public BookRepository(Path file) throws IOException { super(file); }

    @Override protected String idOf(Book b) { return b.getId(); }

    @Override protected String[] toFields(Book b) {
        return new String[] { b.getId(), b.getTitle(), b.getAuthor(), b.getCategory(),
                String.valueOf(b.getTotalCopies()), String.valueOf(b.getAvailableCopies()) };
    }

    @Override protected Book fromFields(String[] f) {
        return new Book(f[0], f[1], f[2], f[3], Integer.parseInt(f[4]), Integer.parseInt(f[5]));
    }
}
