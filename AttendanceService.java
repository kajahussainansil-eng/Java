package service;

import model.Attendance;
import model.AttendanceStatus;
import model.Course;
import model.Student;
import model.User;
import security.AccessControl;
import storage.WebDataManager;
import util.DateUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AttendanceService {

    private final WebDataManager dataManager;

    public AttendanceService(WebDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public List<Attendance> getAllAttendance() {
        return new ArrayList<>(dataManager.getAttendances());
    }

    public synchronized void recordAttendance(User actor, String studentId, String courseCode,
                                             String date, AttendanceStatus status, String remarks) throws Exception {
        Course course = dataManager.getCourses().get(courseCode.toUpperCase());
        if (course == null) throw new IllegalArgumentException("Course " + courseCode + " does not exist.");

        Student student = dataManager.getStudents().get(studentId.toUpperCase());
        if (student == null) throw new IllegalArgumentException("Student " + studentId + " does not exist.");

        if (!AccessControl.canRecordAttendance(actor, course)) {
            throw new SecurityException("Permission denied: You cannot record attendance for course " + courseCode);
        }

        if (!DateUtils.isValidDate(date)) {
            throw new IllegalArgumentException("Invalid date format. Expected yyyy-MM-dd.");
        }

        Attendance existing = null;
        for (Attendance a : dataManager.getAttendances()) {
            if (a.getStudentId().equalsIgnoreCase(studentId) &&
                a.getCourseCode().equalsIgnoreCase(courseCode) &&
                a.getDate().equals(date)) {
                existing = a;
                break;
            }
        }

        if (existing != null) {
            existing.setStatus(status);
            existing.setRemarks(remarks != null ? remarks : "");
        } else {
            String id = dataManager.generateNextAttendanceId();
            Attendance att = new Attendance(id, studentId.toUpperCase(), courseCode.toUpperCase(),
                    date, status, remarks != null ? remarks : "");
            dataManager.getAttendances().add(att);
        }

        dataManager.saveAttendance();
        dataManager.logAudit(actor.getUserId(), "ATTENDANCE_RECORD",
                "Recorded attendance for " + studentId + " in " + courseCode + ": " + status);
    }

    public synchronized void recordBatchAttendance(User actor, String courseCode, String date,
                                                  Map<String, AttendanceStatus> attendanceMap,
                                                  Map<String, String> remarksMap) throws Exception {
        Course course = dataManager.getCourses().get(courseCode.toUpperCase());
        if (course == null) throw new IllegalArgumentException("Course not found.");

        if (!AccessControl.canRecordAttendance(actor, course)) {
            throw new SecurityException("Permission denied for course " + courseCode);
        }

        for (Map.Entry<String, AttendanceStatus> entry : attendanceMap.entrySet()) {
            String studentId = entry.getKey();
            AttendanceStatus status = entry.getValue();
            String remarks = remarksMap != null ? remarksMap.getOrDefault(studentId, "") : "";
            recordAttendance(actor, studentId, courseCode, date, status, remarks);
        }
    }

    public List<Attendance> getAttendanceForCourse(String courseCode) {
        String code = courseCode.toUpperCase();
        return dataManager.getAttendances().stream()
                .filter(a -> a.getCourseCode().equalsIgnoreCase(code))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
    }

    public List<Attendance> getAttendanceForStudent(String studentId) {
        String sid = studentId.toUpperCase();
        return dataManager.getAttendances().stream()
                .filter(a -> a.getStudentId().equalsIgnoreCase(sid))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
    }

    public double calculateStudentCoursePercentage(String studentId, String courseCode) {
        List<Attendance> records = dataManager.getAttendances().stream()
                .filter(a -> a.getStudentId().equalsIgnoreCase(studentId) &&
                             a.getCourseCode().equalsIgnoreCase(courseCode))
                .collect(Collectors.toList());

        if (records.isEmpty()) return 100.0;

        long attended = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();

        return (double) attended / records.size() * 100.0;
    }

    public double calculateStudentOverallPercentage(String studentId) {
        List<Attendance> records = getAttendanceForStudent(studentId);
        if (records.isEmpty()) return 100.0;

        long attended = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();

        return (double) attended / records.size() * 100.0;
    }

    public double calculateCampusOverallPercentage() {
        List<Attendance> all = dataManager.getAttendances();
        if (all.isEmpty()) return 0.0;
        long attended = all.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();
        return (double) attended / all.size() * 100.0;
    }

    public Map<String, Double> getStudentCourseBreakdown(String studentId) {
        Map<String, Double> map = new HashMap<>();
        List<Attendance> records = getAttendanceForStudent(studentId);
        Set<String> courses = records.stream().map(Attendance::getCourseCode).collect(Collectors.toSet());
        for (String c : courses) {
            map.put(c, calculateStudentCoursePercentage(studentId, c));
        }
        return map;
    }
}
