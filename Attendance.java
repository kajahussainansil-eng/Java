package model;

import java.io.Serializable;

public class Attendance implements Serializable {
    private static final long serialVersionUID = 1L;

    private String attendanceId;
    private String studentId;
    private String courseCode;
    private String date; // YYYY-MM-DD
    private AttendanceStatus status;
    private String remarks;

    public Attendance() {
        this.status = AttendanceStatus.PRESENT;
        this.remarks = "";
    }

    public Attendance(String attendanceId, String studentId, String courseCode,
                      String date, AttendanceStatus status, String remarks) {
        this.attendanceId = attendanceId;
        this.studentId = studentId;
        this.courseCode = courseCode != null ? courseCode.trim().toUpperCase() : "";
        this.date = date;
        this.status = status != null ? status : AttendanceStatus.PRESENT;
        this.remarks = remarks != null ? remarks : "";
    }

    public String getAttendanceId() { return attendanceId; }
    public void setAttendanceId(String attendanceId) { this.attendanceId = attendanceId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode != null ? courseCode.trim().toUpperCase() : ""; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @Override
    public String toString() {
        return date + " | " + courseCode + " | " + studentId + " : " + status;
    }
}
