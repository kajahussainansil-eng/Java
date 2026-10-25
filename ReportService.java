package service;

import model.*;
import storage.WebDataManager;
import util.DateUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {

    private final WebDataManager dataManager;
    private final AttendanceService attendanceService;

    public static class LowAttendanceRecord {
        public final String studentId;
        public final String studentName;
        public final String department;
        public final String courseCode;
        public final double percentage;
        public final int attendedSessions;
        public final int totalSessions;

        public LowAttendanceRecord(String studentId, String studentName, String department,
                                   String courseCode, double percentage, int attendedSessions, int totalSessions) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.department = department;
            this.courseCode = courseCode;
            this.percentage = percentage;
            this.attendedSessions = attendedSessions;
            this.totalSessions = totalSessions;
        }
    }

    public ReportService(WebDataManager dataManager, AttendanceService attendanceService) {
        this.dataManager = dataManager;
        this.attendanceService = attendanceService;
    }

    public Map<String, Integer> getStudentsByDepartment() {
        Map<String, Integer> map = new TreeMap<>();
        for (Student s : dataManager.getStudents().values()) {
            map.put(s.getDepartment(), map.getOrDefault(s.getDepartment(), 0) + 1);
        }
        return map;
    }

    public Map<String, Integer> getComplaintsByStatus() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("PENDING", 0);
        map.put("IN_PROGRESS", 0);
        map.put("RESOLVED", 0);
        map.put("REJECTED", 0);

        for (Complaint c : dataManager.getComplaints().values()) {
            String s = c.getStatus().toUpperCase();
            map.put(s, map.getOrDefault(s, 0) + 1);
        }
        return map;
    }

    public List<LowAttendanceRecord> generateLowAttendanceReport(double thresholdPct) {
        List<LowAttendanceRecord> list = new ArrayList<>();

        for (Student s : dataManager.getStudents().values()) {
            if (!s.isActive()) continue;
            for (Course c : dataManager.getCourses().values()) {
                List<Attendance> records = dataManager.getAttendances().stream()
                        .filter(a -> a.getStudentId().equalsIgnoreCase(s.getStudentId()) &&
                                     a.getCourseCode().equalsIgnoreCase(c.getCourseCode()))
                        .collect(Collectors.toList());

                if (records.size() >= 2) {
                    int attended = (int) records.stream()
                            .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                            .count();
                    double pct = (double) attended / records.size() * 100.0;
                    if (pct < thresholdPct) {
                        list.add(new LowAttendanceRecord(s.getStudentId(), s.getFullName(),
                                s.getDepartment(), c.getCourseCode(), pct, attended, records.size()));
                    }
                }
            }
        }

        list.sort(Comparator.comparingDouble(r -> r.percentage));
        return list;
    }

    public void exportLowAttendanceToCsv(File file, double thresholdPct) throws IOException {
        List<LowAttendanceRecord> records = generateLowAttendanceReport(thresholdPct);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Student ID,Student Name,Department,Course Code,Attendance %,Attended Classes,Total Classes");
            for (LowAttendanceRecord r : records) {
                pw.printf("%s,\"%s\",\"%s\",%s,%.1f%%,%d,%d%n",
                        r.studentId, r.studentName, r.department, r.courseCode, r.percentage, r.attendedSessions, r.totalSessions);
            }
        }
    }

    public void exportCampusSummaryReport(File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("===============================================================================");
            pw.println("                     CAMPUSFLOW EXECUTIVE SUMMARY REPORT                      ");
            pw.println("                      SMART CAMPUS MANAGEMENT SYSTEM                          ");
            pw.println("===============================================================================");
            pw.println("Generated Date: " + DateUtils.nowString());
            pw.println();

            pw.println("-------------------------------------------------------------------------------");
            pw.println("1. INSTITUTIONAL METRICS SUMMARY");
            pw.println("-------------------------------------------------------------------------------");
            pw.printf("Total Enrolled Students:  %d%n", dataManager.getStudents().size());
            pw.printf("Total Faculty Members:    %d%n", dataManager.getFaculties().size());
            pw.printf("Total Active Courses:     %d%n", dataManager.getCourses().size());
            pw.printf("Total Attendance Records: %d%n", dataManager.getAttendances().size());
            pw.printf("Campus-Wide Attendance:   %.2f%%%n", attendanceService.calculateCampusOverallPercentage());
            pw.printf("Pending Student Complaints:%d%n", dataManager.getComplaints().values().stream()
                    .filter(c -> !"RESOLVED".equalsIgnoreCase(c.getStatus())).count());
            pw.printf("Total Notices Published:  %d%n", dataManager.getNotices().size());
            pw.printf("Scheduled Campus Events:  %d%n", dataManager.getEvents().size());
            pw.println();

            pw.println("-------------------------------------------------------------------------------");
            pw.println("2. STUDENTS ENROLLMENT BY DEPARTMENT");
            pw.println("-------------------------------------------------------------------------------");
            Map<String, Integer> deptMap = getStudentsByDepartment();
            for (Map.Entry<String, Integer> e : deptMap.entrySet()) {
                pw.printf(" - %-35s : %d students%n", e.getKey(), e.getValue());
            }
            pw.println();

            pw.println("-------------------------------------------------------------------------------");
            pw.println("3. GRIEVANCES AND COMPLAINTS BREAKDOWN");
            pw.println("-------------------------------------------------------------------------------");
            Map<String, Integer> compMap = getComplaintsByStatus();
            for (Map.Entry<String, Integer> e : compMap.entrySet()) {
                pw.printf(" - %-20s : %d%n", e.getKey(), e.getValue());
            }
            pw.println();

            pw.println("-------------------------------------------------------------------------------");
            pw.println("4. DEFICIT ATTENDANCE ALERTS (< 75.0% THRESHOLD)");
            pw.println("-------------------------------------------------------------------------------");
            List<LowAttendanceRecord> low = generateLowAttendanceReport(75.0);
            if (low.isEmpty()) {
                pw.println("No students currently falling below the 75% attendance threshold.");
            } else {
                pw.printf("%-10s | %-22s | %-8s | %-12s | %s%n", "ID", "Student Name", "Course", "Attendance", "Classes");
                pw.println("-------------------------------------------------------------------------------");
                for (LowAttendanceRecord r : low) {
                    pw.printf("%-10s | %-22s | %-8s | %6.1f%%     | %d / %d%n",
                            r.studentId, r.studentName, r.courseCode, r.percentage, r.attendedSessions, r.totalSessions);
                }
            }
            pw.println();
            pw.println("===============================================================================");
            pw.println("                       END OF CAMPUSFLOW SYSTEM REPORT                         ");
            pw.println("===============================================================================");
        }
    }
}
