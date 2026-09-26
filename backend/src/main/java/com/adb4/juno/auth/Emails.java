package com.adb4.juno.auth;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Emails {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Emails() {
    }

    public static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String email) {
        return email != null && email.length() <= 320 && EMAIL.matcher(email.strip()).matches();
    }
}
