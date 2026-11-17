package com.college.library.util;

import com.college.library.model.Book;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SearchUtil {
    private SearchUtil() { }

    /** Case-insensitive partial match. Pattern.quote makes characters such as '+' or '(' literal. */
    public static List<Book> search(List<Book> all, String query, SearchField field) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return all.stream().sorted(byTitle()).collect(Collectors.toList());
        }
        Pattern p = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);   // compiled once per search
        return all.stream()
                .filter(b -> matches(p, b, field))
                .sorted(byTitle())
                .collect(Collectors.toList());
    }

    private static boolean matches(Pattern p, Book b, SearchField field) {
        switch (field) {
            case TITLE:  return p.matcher(b.getTitle()).find();
            case AUTHOR: return p.matcher(b.getAuthor()).find();
            case ID:     return p.matcher(b.getId()).find();
            default:     return p.matcher(b.getTitle()).find() || p.matcher(b.getAuthor()).find()
                                || p.matcher(b.getId()).find();
        }
    }

    private static Comparator<Book> byTitle() { return Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER); }
}
