package model;

public enum UserStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static UserStatus fromString(String str) {
        if (str == null) return ACTIVE;
        String upper = str.trim().toUpperCase();
        for (UserStatus s : values()) {
            if (s.name().equals(upper) || s.label.equalsIgnoreCase(upper)) {
                return s;
            }
        }
        return ACTIVE;
    }
}
