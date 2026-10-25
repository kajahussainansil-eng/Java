package model;

import java.io.Serializable;

public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String fullName;
    private String department;
    private int semester;
    private String email;
    private String phone;
    private String enrollmentDate;
    private String status; // "ACTIVE", "INACTIVE", "GRADUATED"

    public Student() {
        this.status = "ACTIVE";
        this.semester = 1;
    }

    public Student(String studentId, String fullName, String department, int semester,
                   String email, String phone, String enrollmentDate, String status) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.department = department;
        this.semester = semester;
        this.email = email;
        this.phone = phone;
        this.enrollmentDate = enrollmentDate;
        this.status = (status != null && !status.isEmpty()) ? status : "ACTIVE";
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(String enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return fullName + " (" + studentId + ")";
    }
}
