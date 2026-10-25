package util;

import java.util.regex.Pattern;

/**
 * Validation helpers for inputs across forms.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9\\s\\-\\(\\)]{7,20}$"
    );

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._-]{3,30}$"
    );

    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile(
            "^[A-Za-z]{2,5}[0-9]{3,4}[A-Za-z]?$"
    );

    private ValidationUtils() {}

    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    public static boolean isValidCourseCode(String code) {
        if (code == null) return false;
        return COURSE_CODE_PATTERN.matcher(code.trim().toUpperCase()).matches();
    }

    public static boolean isValidPassword(String password) {
        // Minimum 6 characters for academic project security
        return password != null && password.length() >= 6;
    }
}
