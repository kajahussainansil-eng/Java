package security;

import model.Course;
import model.Role;
import model.User;

/**
 * Enforces role-based permissions at the application logic layer.
 */
public final class AccessControl {

    private AccessControl() {}

    public static void requireLogin(User user) {
        if (user == null || !user.isActive()) {
            throw new SecurityException("Authentication required to perform this action.");
        }
    }

    public static void requireRole(User user, Role role) {
        requireLogin(user);
        if (user.getRole() != role) {
            throw new SecurityException("Access denied: Requires role " + role.getDisplayName() + ".");
        }
    }

    public static void requireAdmin(User user) {
        requireRole(user, Role.ADMIN);
    }

    public static boolean canManageUsers(User user) {
        return user != null && user.isActive() && user.getRole() == Role.ADMIN;
    }

    public static boolean canManageStudents(User user) {
        return user != null && user.isActive() && user.getRole() == Role.ADMIN;
    }

    public static boolean canManageFaculty(User user) {
        return user != null && user.isActive() && user.getRole() == Role.ADMIN;
    }

    public static boolean canManageCourses(User user) {
        return user != null && user.isActive() && user.getRole() == Role.ADMIN;
    }

    public static boolean canRecordAttendance(User user, Course course) {
        if (user == null || !user.isActive()) return false;
        if (user.getRole() == Role.ADMIN) return true;
        if (user.getRole() == Role.FACULTY && course != null) {
            return user.getUserId().equalsIgnoreCase(course.getAssignedFacultyId());
        }
        return false;
    }

    public static boolean canPublishNotices(User user) {
        return user != null && user.isActive() &&
                (user.getRole() == Role.ADMIN || user.getRole() == Role.FACULTY);
    }

    public static boolean canManageEvents(User user) {
        return user != null && user.isActive() &&
                (user.getRole() == Role.ADMIN || user.getRole() == Role.FACULTY);
    }

    public static boolean canReviewComplaints(User user) {
        return user != null && user.isActive() && user.getRole() == Role.ADMIN;
    }

    public static boolean canSubmitComplaints(User user) {
        return user != null && user.isActive() && user.getRole() == Role.STUDENT;
    }

    public static boolean canViewReports(User user) {
        return user != null && user.isActive() &&
                (user.getRole() == Role.ADMIN || user.getRole() == Role.FACULTY);
    }

    public static boolean canBackupData(User user) {
        return user != null && user.isActive() && user.getRole() == Role.ADMIN;
    }
}
