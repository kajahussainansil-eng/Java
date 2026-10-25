package model;

import java.io.Serializable;

public class Faculty implements Serializable {
    private static final long serialVersionUID = 1L;

    private String facultyId;
    private String fullName;
    private String department;
    private String designation;
    private String email;
    private String phone;
    private String status; // "ACTIVE", "INACTIVE"

    public Faculty() {
        this.status = "ACTIVE";
    }

    public Faculty(String facultyId, String fullName, String department,
                   String designation, String email, String phone, String status) {
        this.facultyId = facultyId;
        this.fullName = fullName;
        this.department = department;
        this.designation = designation;
        this.email = email;
        this.phone = phone;
        this.status = (status != null && !status.isEmpty()) ? status : "ACTIVE";
    }

    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return fullName + " (" + designation + ", " + facultyId + ")";
    }
}
