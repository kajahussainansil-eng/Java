package model;

import java.io.Serializable;

public class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    private String courseCode;
    private String courseName;
    private String department;
    private int semester;
    private int credits;
    private String assignedFacultyId;
    private String status; // "ACTIVE", "ARCHIVED"

    public Course() {
        this.semester = 1;
        this.credits = 3;
        this.status = "ACTIVE";
        this.assignedFacultyId = "";
    }

    public Course(String courseCode, String courseName, String department,
                  int semester, int credits, String assignedFacultyId, String status) {
        this.courseCode = courseCode != null ? courseCode.trim().toUpperCase() : "";
        this.courseName = courseName;
        this.department = department;
        this.semester = semester;
        this.credits = credits;
        this.assignedFacultyId = assignedFacultyId != null ? assignedFacultyId : "";
        this.status = (status != null && !status.isEmpty()) ? status : "ACTIVE";
    }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode != null ? courseCode.trim().toUpperCase() : ""; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public String getAssignedFacultyId() { return assignedFacultyId; }
    public void setAssignedFacultyId(String assignedFacultyId) { this.assignedFacultyId = assignedFacultyId != null ? assignedFacultyId : ""; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return courseCode + " - " + courseName;
    }
}
