package model;

public enum Role {
    ADMIN("Administrator", "ADM"),
    FACULTY("Faculty", "FAC"),
    STUDENT("Student", "STU");

    private final String displayName;
    private final String idPrefix;

    Role(String displayName, String idPrefix) {
        this.displayName = displayName;
        this.idPrefix = idPrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public static Role fromString(String str) {
        if (str == null) return STUDENT;
        String upper = str.trim().toUpperCase();
        for (Role r : values()) {
            if (r.name().equals(upper) || r.displayName.equalsIgnoreCase(upper)) {
                return r;
            }
        }
        return STUDENT;
    }
}
