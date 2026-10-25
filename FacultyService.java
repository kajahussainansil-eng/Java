package service;

import model.Faculty;
import model.Role;
import model.User;
import model.UserStatus;
import security.PasswordHasher;
import storage.WebDataManager;
import util.DateUtils;
import util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FacultyService {

    private final WebDataManager dataManager;

    public FacultyService(WebDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public List<Faculty> getAllFaculty() {
        return new ArrayList<>(dataManager.getFaculties().values());
    }

    public Faculty getFaculty(String facultyId) {
        if (facultyId == null) return null;
        return dataManager.getFaculties().get(facultyId.toUpperCase());
    }

    public synchronized Faculty createFaculty(String fullName, String department, String designation,
                                              String email, String phone, String status,
                                              boolean createPortalAccount, String tempPassword) throws Exception {
        if (!ValidationUtils.isNotEmpty(fullName)) throw new IllegalArgumentException("Full name is required.");
        if (!ValidationUtils.isNotEmpty(department)) throw new IllegalArgumentException("Department is required.");
        if (!ValidationUtils.isNotEmpty(designation)) throw new IllegalArgumentException("Designation is required.");
        if (!ValidationUtils.isValidEmail(email)) throw new IllegalArgumentException("Valid email address is required.");
        if (!ValidationUtils.isValidPhone(phone)) throw new IllegalArgumentException("Valid phone number is required.");

        for (Faculty f : dataManager.getFaculties().values()) {
            if (f.getEmail().equalsIgnoreCase(email.trim())) {
                throw new IllegalArgumentException("A faculty member with this email address already exists.");
            }
        }

        String facultyId = dataManager.generateNextFacultyId();
        Faculty faculty = new Faculty(facultyId, fullName.trim(), department.trim(),
                designation.trim(), email.trim(), phone.trim(), status != null ? status : "ACTIVE");

        dataManager.getFaculties().put(facultyId, faculty);
        dataManager.saveFaculty();

        if (createPortalAccount) {
            String username = facultyId.toLowerCase();
            String pwd = (tempPassword != null && tempPassword.length() >= 6) ? tempPassword : "Faculty@123";
            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hashPassword(pwd, salt);

            User u = new User(facultyId, username, hash, salt, fullName.trim(),
                    Role.FACULTY, UserStatus.ACTIVE, true, DateUtils.nowString(), "");
            dataManager.getUsers().put(facultyId, u);
            dataManager.saveUsers();
        }

        dataManager.logAudit("ADMIN", "CREATE_FACULTY", "Created faculty " + facultyId + " - " + fullName);
        return faculty;
    }

    public synchronized void updateFaculty(Faculty faculty) throws Exception {
        if (faculty == null || faculty.getFacultyId() == null) throw new IllegalArgumentException("Invalid faculty record.");
        if (!ValidationUtils.isNotEmpty(faculty.getFullName())) throw new IllegalArgumentException("Full name is required.");
        if (!ValidationUtils.isValidEmail(faculty.getEmail())) throw new IllegalArgumentException("Valid email is required.");

        dataManager.getFaculties().put(faculty.getFacultyId(), faculty);
        dataManager.saveFaculty();

        User u = dataManager.getUsers().get(faculty.getFacultyId());
        if (u != null) {
            u.setFullName(faculty.getFullName());
            u.setStatus("ACTIVE".equalsIgnoreCase(faculty.getStatus()) ? UserStatus.ACTIVE : UserStatus.INACTIVE);
            dataManager.saveUsers();
        }

        dataManager.logAudit("ADMIN", "UPDATE_FACULTY", "Updated faculty " + faculty.getFacultyId());
    }

    public synchronized void deactivateFaculty(String facultyId) throws Exception {
        Faculty f = dataManager.getFaculties().get(facultyId);
        if (f == null) throw new IllegalArgumentException("Faculty member not found.");

        f.setStatus("INACTIVE");
        dataManager.saveFaculty();

        User u = dataManager.getUsers().get(facultyId);
        if (u != null) {
            u.setStatus(UserStatus.INACTIVE);
            dataManager.saveUsers();
        }

        dataManager.logAudit("ADMIN", "DEACTIVATE_FACULTY", "Deactivated faculty " + facultyId);
    }

    public synchronized void activateFaculty(String facultyId) throws Exception {
        Faculty f = dataManager.getFaculties().get(facultyId);
        if (f == null) throw new IllegalArgumentException("Faculty member not found.");

        f.setStatus("ACTIVE");
        dataManager.saveFaculty();

        User u = dataManager.getUsers().get(facultyId);
        if (u != null) {
            u.setStatus(UserStatus.ACTIVE);
            dataManager.saveUsers();
        }

        dataManager.logAudit("ADMIN", "ACTIVATE_FACULTY", "Activated faculty " + facultyId);
    }

    public List<Faculty> searchFaculty(String query, String deptFilter) {
        String q = query != null ? query.trim().toLowerCase() : "";
        String dept = deptFilter != null ? deptFilter.trim() : "All";

        return dataManager.getFaculties().values().stream()
                .filter(f -> {
                    boolean matchesQuery = q.isEmpty() ||
                            f.getFacultyId().toLowerCase().contains(q) ||
                            f.getFullName().toLowerCase().contains(q) ||
                            f.getEmail().toLowerCase().contains(q) ||
                            f.getDesignation().toLowerCase().contains(q);

                    boolean matchesDept = dept.equalsIgnoreCase("All") ||
                            f.getDepartment().equalsIgnoreCase(dept);

                    return matchesQuery && matchesDept;
                })
                .sorted((a, b) -> a.getFacultyId().compareTo(b.getFacultyId()))
                .collect(Collectors.toList());
    }
}
