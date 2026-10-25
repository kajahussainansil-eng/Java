package model;

public enum AttendanceStatus {
    PRESENT("Present"),
    ABSENT("Absent"),
    LATE("Late"),
    EXCUSED("Excused");

    private final String label;

    AttendanceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AttendanceStatus fromString(String str) {
        if (str == null) return PRESENT;
        String upper = str.trim().toUpperCase();
        for (AttendanceStatus s : values()) {
            if (s.name().equals(upper) || s.label.equalsIgnoreCase(upper)) {
                return s;
            }
        }
        return PRESENT;
    }
}
