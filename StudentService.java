package service;

import model.Role;
import model.Student;
import model.User;
import model.UserStatus;
import security.PasswordHasher;
import storage.WebDataManager;
import util.DateUtils;
import util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StudentService {

    private final WebDataManager dataManager;

    public StudentService(WebDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public List<Student> getAllStudents() {
        return new ArrayList<>(dataManager.getStudents().values());
    }

    public Student getStudent(String studentId) {
        if (studentId == null) return null;
        return dataManager.getStudents().get(studentId.toUpperCase());
    }

    public synchronized Student createStudent(String fullName, String department, int semester,
                                              String email, String phone, String status,
                                              boolean createPortalAccount, String tempPassword) throws Exception {
        if (!ValidationUtils.isNotEmpty(fullName)) throw new IllegalArgumentException("Full name is required.");
        if (!ValidationUtils.isNotEmpty(department)) throw new IllegalArgumentException("Department is required.");
        if (semester < 1 || semester > 8) throw new IllegalArgumentException("Semester must be between 1 and 8.");
        if (!ValidationUtils.isValidEmail(email)) throw new IllegalArgumentException("Valid email address is required.");
        if (!ValidationUtils.isValidPhone(phone)) throw new IllegalArgumentException("Valid phone number is required.");

        for (Student s : dataManager.getStudents().values()) {
            if (s.getEmail().equalsIgnoreCase(email.trim())) {
                throw new IllegalArgumentException("A student with this email address already exists.");
            }
        }

        String studentId = dataManager.generateNextStudentId();
        Student student = new Student(studentId, fullName.trim(), department.trim(), semester,
                email.trim(), phone.trim(), DateUtils.todayString(), status != null ? status : "ACTIVE");

        dataManager.getStudents().put(studentId, student);
        dataManager.saveStudents();

        if (createPortalAccount) {
            String username = studentId.toLowerCase();
            String pwd = (tempPassword != null && tempPassword.length() >= 6) ? tempPassword : "Student@123";
            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hashPassword(pwd, salt);

            User u = new User(studentId, username, hash, salt, fullName.trim(),
                    Role.STUDENT, UserStatus.ACTIVE, true, DateUtils.nowString(), "");
            dataManager.getUsers().put(studentId, u);
            dataManager.saveUsers();
        }

        dataManager.logAudit("ADMIN", "CREATE_STUDENT", "Created student " + studentId + " - " + fullName);
        return student;
    }

    public synchronized void updateStudent(Student student) throws Exception {
        if (student == null || student.getStudentId() == null) throw new IllegalArgumentException("Invalid student object.");
        if (!ValidationUtils.isNotEmpty(student.getFullName())) throw new IllegalArgumentException("Full name is required.");
        if (!ValidationUtils.isValidEmail(student.getEmail())) throw new IllegalArgumentException("Valid email is required.");

        dataManager.getStudents().put(student.getStudentId(), student);
        dataManager.saveStudents();

        User u = dataManager.getUsers().get(student.getStudentId());
        if (u != null) {
            u.setFullName(student.getFullName());
            u.setStatus("ACTIVE".equalsIgnoreCase(student.getStatus()) ? UserStatus.ACTIVE : UserStatus.INACTIVE);
            dataManager.saveUsers();
        }

        dataManager.logAudit("ADMIN", "UPDATE_STUDENT", "Updated student " + student.getStudentId());
    }

    public synchronized void deactivateStudent(String studentId) throws Exception {
        Student s = dataManager.getStudents().get(studentId);
        if (s == null) throw new IllegalArgumentException("Student not found.");

        s.setStatus("INACTIVE");
        dataManager.saveStudents();

        User u = dataManager.getUsers().get(studentId);
        if (u != null) {
            u.setStatus(UserStatus.INACTIVE);
            dataManager.saveUsers();
        }

        dataManager.logAudit("ADMIN", "DEACTIVATE_STUDENT", "Deactivated student " + studentId);
    }

    public synchronized void activateStudent(String studentId) throws Exception {
        Student s = dataManager.getStudents().get(studentId);
        if (s == null) throw new IllegalArgumentException("Student not found.");

        s.setStatus("ACTIVE");
        dataManager.saveStudents();

        User u = dataManager.getUsers().get(studentId);
        if (u != null) {
            u.setStatus(UserStatus.ACTIVE);
            dataManager.saveUsers();
        }

        dataManager.logAudit("ADMIN", "ACTIVATE_STUDENT", "Activated student " + studentId);
    }

    public List<Student> searchStudents(String query, String deptFilter, String statusFilter) {
        String q = query != null ? query.trim().toLowerCase() : "";
        String dept = deptFilter != null ? deptFilter.trim() : "All";
        String status = statusFilter != null ? statusFilter.trim() : "All";

        return dataManager.getStudents().values().stream()
                .filter(s -> {
                    boolean matchesQuery = q.isEmpty() ||
                            s.getStudentId().toLowerCase().contains(q) ||
                            s.getFullName().toLowerCase().contains(q) ||
                            s.getEmail().toLowerCase().contains(q) ||
                            s.getDepartment().toLowerCase().contains(q);

                    boolean matchesDept = dept.equalsIgnoreCase("All") ||
                            s.getDepartment().equalsIgnoreCase(dept);

                    boolean matchesStatus = status.equalsIgnoreCase("All") ||
                            s.getStatus().equalsIgnoreCase(status);

                    return matchesQuery && matchesDept && matchesStatus;
                })
                .sorted((a, b) -> a.getStudentId().compareTo(b.getStudentId()))
                .collect(Collectors.toList());
    }
}
