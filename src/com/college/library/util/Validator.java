package com.college.library.util;

import com.college.library.exception.ValidationException;
import com.college.library.model.MemberType;
import java.util.regex.Pattern;

/** Central place for every input rule, so the same checks apply to the UI and to the service layer. */
public final class Validator {
    private static final Pattern BOOK_ID = Pattern.compile("^B\\d{3,5}$");
    private static final Pattern MEMBER_ID = Pattern.compile("^M\\d{3,5}$");
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,59}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    public static final int MIN_COPIES = 1;
    public static final int MAX_COPIES = 99;

    private Validator() { }

    public static String bookId(String raw) throws ValidationException {
        String id = clean(raw).toUpperCase();
        if (!BOOK_ID.matcher(id).matches())
            throw new ValidationException("Invalid book ID '" + raw + "': use the format B followed by 3 to 5 digits (e.g. B101)");
        return id;
    }

    public static String memberId(String raw) throws ValidationException {
        String id = clean(raw).toUpperCase();
        if (!MEMBER_ID.matcher(id).matches())
            throw new ValidationException("Invalid member ID '" + raw + "': use the format M followed by 3 to 5 digits (e.g. M001)");
        return id;
    }

    /** Free text such as a title or author: not blank, at most 100 characters and without the '|' separator. */
    public static String text(String raw, String field) throws ValidationException {
        String value = clean(raw);
        if (value.isEmpty()) throw new ValidationException(field + " must not be empty");
        if (value.length() > 100) throw new ValidationException(field + " must not exceed 100 characters");
        if (value.contains("|")) throw new ValidationException(field + " must not contain the '|' character");
        return value;
    }

    public static String name(String raw) throws ValidationException {
        String value = clean(raw);
        if (!NAME.matcher(value).matches())
            throw new ValidationException("Invalid name '" + raw + "': letters, spaces, dots, hyphens and apostrophes only");
        return value;
    }

    public static String email(String raw) throws ValidationException {
        String value = clean(raw);
        if (!EMAIL.matcher(value).matches()) throw new ValidationException("Invalid e-mail address '" + raw + "'");
        return value;
    }

    public static int copies(String raw) throws ValidationException {
        try {
            int n = Integer.parseInt(clean(raw));
            if (n < MIN_COPIES || n > MAX_COPIES)
                throw new ValidationException("Copies must be between " + MIN_COPIES + " and " + MAX_COPIES);
            return n;
        } catch (NumberFormatException e) {
            throw new ValidationException("Copies must be a whole number, but received '" + raw + "'");
        }
    }

    public static MemberType memberType(String raw) throws ValidationException {
        try { return MemberType.valueOf(clean(raw).toUpperCase()); }
        catch (IllegalArgumentException e) { throw new ValidationException("Member type must be STUDENT or FACULTY"); }
    }

    private static String clean(String raw) { return raw == null ? "" : raw.trim(); }
}
