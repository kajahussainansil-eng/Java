package util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility methods for date and time parsing and formatting.
 */
public final class DateUtils {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    public static final DateTimeFormatter DISPLAY_DATETIME = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    public static final DateTimeFormatter STORAGE_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {}

    public static String todayString() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String nowString() {
        return LocalDateTime.now().format(STORAGE_DATETIME);
    }

    public static String formatDisplayDate(String yyyyMmDd) {
        if (yyyyMmDd == null || yyyyMmDd.trim().isEmpty()) return "-";
        try {
            LocalDate d = LocalDate.parse(yyyyMmDd.trim(), DATE_FORMATTER);
            return d.format(DISPLAY_DATE);
        } catch (DateTimeParseException e) {
            return yyyyMmDd;
        }
    }

    public static String formatDisplayDateTime(String dtStr) {
        if (dtStr == null || dtStr.trim().isEmpty()) return "-";
        try {
            LocalDateTime dt = LocalDateTime.parse(dtStr.trim(), STORAGE_DATETIME);
            return dt.format(DISPLAY_DATETIME);
        } catch (DateTimeParseException e) {
            return dtStr;
        }
    }

    public static boolean isValidDate(String yyyyMmDd) {
        if (yyyyMmDd == null || yyyyMmDd.trim().isEmpty()) return false;
        try {
            LocalDate.parse(yyyyMmDd.trim(), DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static LocalDate parseDate(String yyyyMmDd) {
        if (yyyyMmDd == null || yyyyMmDd.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(yyyyMmDd.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
