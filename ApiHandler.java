package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.*;
import security.AccessControl;
import service.*;
import storage.WebDataManager;
import util.DateUtils;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles all REST API requests for CampusFlow Mobile Web.
 */
public class ApiHandler implements HttpHandler {

    private final WebDataManager dataManager;
    private final WebAuthService authService;
    private final StudentService studentService;
    private final FacultyService facultyService;
    private final CourseService courseService;
    private final AttendanceService attendanceService;
    private final NoticeService noticeService;
    private final EventService eventService;
    private final ComplaintService complaintService;
    private final ReportService reportService;
    private final SessionManager sessionManager;

    public ApiHandler() {
        this.dataManager = WebDataManager.getInstance();
        this.sessionManager = SessionManager.getInstance();
        this.authService = new WebAuthService(dataManager);
        this.studentService = new StudentService(dataManager);
        this.facultyService = new FacultyService(dataManager);
        this.courseService = new CourseService(dataManager);
        this.attendanceService = new AttendanceService(dataManager);
        this.noticeService = new NoticeService(dataManager);
        this.eventService = new EventService(dataManager);
        this.complaintService = new ComplaintService(dataManager);
        this.reportService = new ReportService(dataManager, attendanceService);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        // CORS Headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            dispatch(exchange, method, path);
        } catch (SecurityException se) {
            sendError(exchange, 403, se.getMessage());
        } catch (IllegalArgumentException iae) {
            sendError(exchange, 400, iae.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            sendError(exchange, 500, "Internal error: " + ex.getMessage());
        }
    }

    private void dispatch(HttpExchange exchange, String method, String path) throws Exception {
        // Public Auth Endpoints
        if ("/api/auth/login".equals(path) && "POST".equals(method)) {
            handleLogin(exchange);
            return;
        }

        // Require Valid Session for all other /api/ endpoints
        User user = authenticate(exchange);
        if (user == null) {
            sendError(exchange, 401, "Authentication required. Please log in.");
            return;
        }

        if ("/api/auth/me".equals(path) && "GET".equals(method)) {
            sendJson(exchange, 200, userToSafeMap(user));
        } else if ("/api/auth/logout".equals(path) && "POST".equals(method)) {
            String token = extractToken(exchange);
            sessionManager.invalidateToken(token);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            sendJson(exchange, 200, resp);
        } else if ("/api/auth/change-password".equals(path) && "POST".equals(method)) {
            handleChangePassword(exchange, user);
        } else if ("/api/auth/reset-password".equals(path) && "POST".equals(method)) {
            handleAdminResetPassword(exchange, user);
        } else if ("/api/dashboard".equals(path) && "GET".equals(method)) {
            handleDashboard(exchange, user);
        } else if (path.startsWith("/api/students")) {
            handleStudents(exchange, method, path, user);
        } else if (path.startsWith("/api/faculty")) {
            handleFaculty(exchange, method, path, user);
        } else if (path.startsWith("/api/courses")) {
            handleCourses(exchange, method, path, user);
        } else if (path.startsWith("/api/attendance")) {
            handleAttendance(exchange, method, path, user);
        } else if (path.startsWith("/api/notices")) {
            handleNotices(exchange, method, path, user);
        } else if (path.startsWith("/api/events")) {
            handleEvents(exchange, method, path, user);
        } else if (path.startsWith("/api/complaints")) {
            handleComplaints(exchange, method, path, user);
        } else if (path.startsWith("/api/reports") || path.startsWith("/api/export")) {
            handleReports(exchange, method, path, user);
        } else if (path.startsWith("/api/settings") || path.equals("/api/backup")) {
            handleSettings(exchange, method, path, user);
        } else if (path.startsWith("/api/sync")) {
            handleDesktopSync(exchange, method, path, user);
        } else {
            sendError(exchange, 404, "Endpoint not found: " + path);
        }
    }

    // -------------------------------------------------------------
    // AUTHENTICATION & SESSIONS
    // -------------------------------------------------------------

    private User authenticate(HttpExchange exchange) {
        String token = extractToken(exchange);
        if (token == null) return null;
        return sessionManager.getUserByToken(token);
    }

    private String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && "cf_session".equals(parts[0])) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    private void handleLogin(HttpExchange exchange) throws Exception {
        Map<String, Object> body = readJsonBody(exchange);
        String username = (String) body.get("username");
        String password = (String) body.get("password");

        User u = authService.login(username, password);
        String token = sessionManager.createSession(u);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("token", token);
        resp.put("user", userToSafeMap(u));
        resp.put("message", "Welcome back, " + u.getFullName());

        sendJson(exchange, 200, resp);
    }

    private void handleChangePassword(HttpExchange exchange, User user) throws Exception {
        Map<String, Object> body = readJsonBody(exchange);
        String oldP = (String) body.get("oldPassword");
        String newP = (String) body.get("newPassword");

        authService.changePassword(user.getUserId(), oldP, newP);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Password changed successfully.");
        sendJson(exchange, 200, resp);
    }

    private void handleAdminResetPassword(HttpExchange exchange, User user) throws Exception {
        AccessControl.requireAdmin(user);
        Map<String, Object> body = readJsonBody(exchange);
        String targetId = (String) body.get("targetUserId");
        String tempP = (String) body.get("tempPassword");

        authService.resetPasswordByAdmin(user.getUserId(), targetId, tempP);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Password reset successfully for " + targetId);
        sendJson(exchange, 200, resp);
    }

    // -------------------------------------------------------------
    // DASHBOARD
    // -------------------------------------------------------------

    private void handleDashboard(HttpExchange exchange, User user) throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", user.getRole().name());
        data.put("fullName", user.getFullName());
        data.put("userId", user.getUserId());

        if (user.getRole() == Role.ADMIN) {
            data.put("totalStudents", dataManager.getStudents().size());
            data.put("totalFaculty", dataManager.getFaculties().size());
            data.put("totalCourses", dataManager.getCourses().size());
            data.put("campusAttendance", attendanceService.calculateCampusOverallPercentage());
            data.put("pendingComplaints", complaintService.countPendingComplaints());
            data.put("upcomingEventsCount", eventService.getUpcomingEvents(10).size());
            data.put("deptDistribution", reportService.getStudentsByDepartment());
            data.put("complaintStatusDistribution", reportService.getComplaintsByStatus());
        } else if (user.getRole() == Role.FACULTY) {
            List<Course> myCourses = courseService.getCoursesByFaculty(user.getUserId());
            data.put("assignedCoursesCount", myCourses.size());
            data.put("assignedCourses", myCourses);

            int totalFacultySessions = 0;
            int totalAttended = 0;
            for (Course c : myCourses) {
                List<Attendance> list = attendanceService.getAttendanceForCourse(c.getCourseCode());
                totalFacultySessions += list.size();
                totalAttended += list.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE).count();
            }
            double facPct = totalFacultySessions > 0 ? (double) totalAttended / totalFacultySessions * 100.0 : 100.0;
            data.put("facultyAttendancePct", facPct);
            data.put("activeNoticesCount", noticeService.getActiveNotices().size());
            data.put("upcomingEventsCount", eventService.getUpcomingEvents(5).size());
        } else {
            // Student
            String sId = user.getUserId();
            Student student = studentService.getStudent(sId);
            double overallAtt = attendanceService.calculateStudentOverallPercentage(sId);
            data.put("studentProfile", student);
            data.put("attendancePercentage", overallAtt);
            data.put("isEligibleForExams", overallAtt >= 75.0);
            data.put("myComplaintsCount", complaintService.getComplaintsForStudent(sId).size());
            data.put("courseAttendance", attendanceService.getStudentCourseBreakdown(sId));
        }

        data.put("recentNotices", noticeService.getRecentNotices(4));
        data.put("upcomingEvents", eventService.getUpcomingEvents(4));

        sendJson(exchange, 200, data);
    }

    // -------------------------------------------------------------
    // STUDENTS
    // -------------------------------------------------------------

    private void handleStudents(HttpExchange exchange, String method, String path, User user) throws Exception {
        if ("GET".equals(method)) {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            String q = query.getOrDefault("q", "");
            String dept = query.getOrDefault("dept", "All");
            String status = query.getOrDefault("status", "All");

            List<Student> list = studentService.searchStudents(q, dept, status);
            sendJson(exchange, 200, list);
        } else if ("POST".equals(method)) {
            AccessControl.requireAdmin(user);
            Map<String, Object> body = readJsonBody(exchange);
            String name = (String) body.get("fullName");
            String dept = (String) body.get("department");
            int sem = parseNumber(body.get("semester"), 1);
            String email = (String) body.get("email");
            String phone = (String) body.get("phone");
            boolean createAcc = parseBoolean(body.get("createPortalAccount"), true);
            String tempPwd = (String) body.getOrDefault("tempPassword", "Student@123");

            Student s = studentService.createStudent(name, dept, sem, email, phone, "ACTIVE", createAcc, tempPwd);
            sendJson(exchange, 201, s);
        } else if ("PUT".equals(method)) {
            AccessControl.requireAdmin(user);
            Map<String, Object> body = readJsonBody(exchange);
            String sId = (String) body.get("studentId");
            Student s = studentService.getStudent(sId);
            if (s == null) throw new IllegalArgumentException("Student not found.");

            if (body.containsKey("fullName")) s.setFullName((String) body.get("fullName"));
            if (body.containsKey("department")) s.setDepartment((String) body.get("department"));
            if (body.containsKey("semester")) s.setSemester(parseNumber(body.get("semester"), s.getSemester()));
            if (body.containsKey("email")) s.setEmail((String) body.get("email"));
            if (body.containsKey("phone")) s.setPhone((String) body.get("phone"));
            if (body.containsKey("status")) s.setStatus((String) body.get("status"));

            studentService.updateStudent(s);
            sendJson(exchange, 200, s);
        } else if ("DELETE".equals(method)) {
            AccessControl.requireAdmin(user);
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            String sId = q.get("id");
            if (sId == null && path.lastIndexOf('/') > 13) {
                sId = path.substring(path.lastIndexOf('/') + 1);
            }
            studentService.deactivateStudent(sId);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Student " + sId + " deactivated.");
            sendJson(exchange, 200, resp);
        }
    }

    // -------------------------------------------------------------
    // FACULTY
    // -------------------------------------------------------------

    private void handleFaculty(HttpExchange exchange, String method, String path, User user) throws Exception {
        if ("GET".equals(method)) {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            String q = query.getOrDefault("q", "");
            String dept = query.getOrDefault("dept", "All");

            List<Faculty> list = facultyService.searchFaculty(q, dept);
            sendJson(exchange, 200, list);
        } else if ("POST".equals(method)) {
            AccessControl.requireAdmin(user);
            Map<String, Object> body = readJsonBody(exchange);
            String name = (String) body.get("fullName");
            String dept = (String) body.get("department");
            String desig = (String) body.get("designation");
            String email = (String) body.get("email");
            String phone = (String) body.get("phone");
            boolean createAcc = parseBoolean(body.get("createPortalAccount"), true);
            String tempPwd = (String) body.getOrDefault("tempPassword", "Faculty@123");

            Faculty f = facultyService.createFaculty(name, dept, desig, email, phone, "ACTIVE", createAcc, tempPwd);
            sendJson(exchange, 201, f);
        } else if ("PUT".equals(method)) {
            AccessControl.requireAdmin(user);
            Map<String, Object> body = readJsonBody(exchange);
            String fId = (String) body.get("facultyId");
            Faculty f = facultyService.getFaculty(fId);
            if (f == null) throw new IllegalArgumentException("Faculty not found.");

            if (body.containsKey("fullName")) f.setFullName((String) body.get("fullName"));
            if (body.containsKey("department")) f.setDepartment((String) body.get("department"));
            if (body.containsKey("designation")) f.setDesignation((String) body.get("designation"));
            if (body.containsKey("email")) f.setEmail((String) body.get("email"));
            if (body.containsKey("phone")) f.setPhone((String) body.get("phone"));
            if (body.containsKey("status")) f.setStatus((String) body.get("status"));

            facultyService.updateFaculty(f);
            sendJson(exchange, 200, f);
        } else if ("DELETE".equals(method)) {
            AccessControl.requireAdmin(user);
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            String fId = q.get("id");
            facultyService.deactivateFaculty(fId);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            sendJson(exchange, 200, resp);
        }
    }

    // -------------------------------------------------------------
    // COURSES
    // -------------------------------------------------------------

    private void handleCourses(HttpExchange exchange, String method, String path, User user) throws Exception {
        if ("GET".equals(method)) {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            String q = query.getOrDefault("q", "");
            String dept = query.getOrDefault("dept", "All");
            List<Course> list = courseService.searchCourses(q, dept);
            sendJson(exchange, 200, list);
        } else if ("POST".equals(method)) {
            AccessControl.requireAdmin(user);
            Map<String, Object> body = readJsonBody(exchange);
            String code = (String) body.get("courseCode");
            String name = (String) body.get("courseName");
            String dept = (String) body.get("department");
            int sem = parseNumber(body.get("semester"), 1);
            int cred = parseNumber(body.get("credits"), 3);
            String facId = (String) body.getOrDefault("assignedFacultyId", "");

            Course c = new Course(code, name, dept, sem, cred, facId, "ACTIVE");
            courseService.createCourse(c);
            sendJson(exchange, 201, c);
        } else if ("PUT".equals(method)) {
            AccessControl.requireAdmin(user);
            Map<String, Object> body = readJsonBody(exchange);
            String code = (String) body.get("courseCode");
            Course c = courseService.getCourse(code);
            if (c == null) throw new IllegalArgumentException("Course not found.");

            if (body.containsKey("courseName")) c.setCourseName((String) body.get("courseName"));
            if (body.containsKey("department")) c.setDepartment((String) body.get("department"));
            if (body.containsKey("semester")) c.setSemester(parseNumber(body.get("semester"), c.getSemester()));
            if (body.containsKey("credits")) c.setCredits(parseNumber(body.get("credits"), c.getCredits()));
            if (body.containsKey("assignedFacultyId")) c.setAssignedFacultyId((String) body.get("assignedFacultyId"));
            if (body.containsKey("status")) c.setStatus((String) body.get("status"));

            courseService.updateCourse(c);
            sendJson(exchange, 200, c);
        } else if ("DELETE".equals(method)) {
            AccessControl.requireAdmin(user);
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            String code = q.get("code");
            courseService.archiveCourse(code);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            sendJson(exchange, 200, resp);
        }
    }

    // -------------------------------------------------------------
    // ATTENDANCE
    // -------------------------------------------------------------

    private void handleAttendance(HttpExchange exchange, String method, String path, User user) throws Exception {
        if ("/api/attendance/roster".equals(path) && "GET".equals(method)) {
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            String courseCode = q.getOrDefault("courseCode", q.get("courseId"));
            String date = q.getOrDefault("date", DateUtils.todayString());

            Course course = courseCode != null ? courseService.getCourse(courseCode) : null;
            List<Student> students = studentService.getAllStudents();
            if (course != null) {
                students = students.stream()
                        .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                        .filter(s -> s.getDepartment().equalsIgnoreCase(course.getDepartment()) || true)
                        .collect(Collectors.toList());
            }

            List<Attendance> existing = (courseCode != null) ? attendanceService.getAttendanceForCourse(courseCode) : Collections.emptyList();
            Map<String, AttendanceStatus> dateMap = new HashMap<>();
            for (Attendance a : existing) {
                if (a.getDate().equals(date)) {
                    dateMap.put(a.getStudentId(), a.getStatus());
                }
            }

            List<Map<String, Object>> roster = new ArrayList<>();
            for (Student s : students) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("studentId", s.getStudentId());
                item.put("studentName", s.getFullName());
                item.put("rollNumber", s.getStudentId());
                item.put("department", s.getDepartment());
                AttendanceStatus st = dateMap.getOrDefault(s.getStudentId(), AttendanceStatus.PRESENT);
                item.put("currentStatus", st.name());
                roster.add(item);
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("courseCode", courseCode);
            resp.put("date", date);
            resp.put("students", roster);
            sendJson(exchange, 200, resp);
        } else if ("/api/attendance/batch".equals(path) && "POST".equals(method)) {
            Map<String, Object> body = readJsonBody(exchange);
            String courseCode = (String) body.getOrDefault("courseCode", body.get("courseId"));
            String date = (String) body.get("date");
            Object recordsObj = body.get("records");
            Map<?, ?> remarksMap = (Map<?, ?>) body.get("remarks");

            Map<String, AttendanceStatus> attMap = new HashMap<>();
            if (recordsObj instanceof Map) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) recordsObj).entrySet()) {
                    attMap.put(String.valueOf(e.getKey()), AttendanceStatus.fromString(String.valueOf(e.getValue())));
                }
            } else if (recordsObj instanceof List) {
                for (Object item : (List<?>) recordsObj) {
                    if (item instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) item;
                        String sid = String.valueOf(m.get("studentId"));
                        String st = String.valueOf(m.get("status"));
                        attMap.put(sid, AttendanceStatus.fromString(st));
                    }
                }
            }

            Map<String, String> remMap = new HashMap<>();
            if (remarksMap != null) {
                for (Map.Entry<?, ?> e : remarksMap.entrySet()) {
                    remMap.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }

            attendanceService.recordBatchAttendance(user, courseCode, date, attMap, remMap);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("recordedCount", attMap.size());
            resp.put("message", "Attendance saved successfully for " + date);
            sendJson(exchange, 200, resp);
        } else if ("/api/attendance/summary".equals(path) && "GET".equals(method)) {
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            String courseCode = q.get("courseCode");
            Course course = courseService.getCourse(courseCode);

            Map<String, Object> res = new LinkedHashMap<>();
            if (course != null) {
                List<Attendance> all = attendanceService.getAttendanceForCourse(courseCode);
                Set<String> dates = all.stream().map(Attendance::getDate).collect(Collectors.toSet());
                res.put("sessionsHeld", dates.size());
                long attended = all.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE).count();
                double pct = all.isEmpty() ? 100.0 : (double) attended / all.size() * 100.0;
                res.put("classAveragePct", pct);
            }
            sendJson(exchange, 200, res);
        } else if ("GET".equals(method)) {
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            String courseCode = q.get("courseCode");
            String date = q.get("date");
            String sId = q.get("studentId");

            if (user.getRole() == Role.STUDENT) {
                sId = user.getUserId(); // Force student to their own
            }

            List<Attendance> list;
            if (courseCode != null && !courseCode.isEmpty()) {
                list = attendanceService.getAttendanceForCourse(courseCode);
                if (date != null && !date.isEmpty()) {
                    list = list.stream().filter(a -> a.getDate().equals(date)).collect(Collectors.toList());
                }
            } else if (sId != null && !sId.isEmpty()) {
                list = attendanceService.getAttendanceForStudent(sId);
            } else {
                list = attendanceService.getAllAttendance();
            }

            sendJson(exchange, 200, list);
        }
    }

    // -------------------------------------------------------------
    // NOTICES
    // -------------------------------------------------------------

    private void handleNotices(HttpExchange exchange, String method, String path, User user) throws Exception {
        if ("/api/notices/archive".equals(path) && "POST".equals(method)) {
            Map<String, Object> body = readJsonBody(exchange);
            String id = (String) body.get("noticeId");
            noticeService.archiveNotice(user, id);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            sendJson(exchange, 200, resp);
        } else if ("GET".equals(method)) {
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            String query = q.getOrDefault("q", "");
            String cat = q.getOrDefault("category", "All");
            List<Notice> list = noticeService.searchNotices(query, cat);
            sendJson(exchange, 200, list);
        } else if ("POST".equals(method)) {
            Map<String, Object> body = readJsonBody(exchange);
            String title = (String) body.get("title");
            String category = (String) body.get("category");
            String desc = (String) body.get("description");

            Notice n = noticeService.createNotice(user, title, category, desc);
            sendJson(exchange, 201, n);
        }
    }

    // -------------------------------------------------------------
    // EVENTS
    // -------------------------------------------------------------

    private void handleEvents(HttpExchange exchange, String method, String path, User user) throws Exception {
        if ("/api/events/cancel".equals(path) && "POST".equals(method)) {
            Map<String, Object> body = readJsonBody(exchange);
            String id = (String) body.get("eventId");
            eventService.cancelEvent(user, id);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            sendJson(exchange, 200, resp);
        } else if ("GET".equals(method)) {
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            String query = q.getOrDefault("q", "");
            String status = q.getOrDefault("status", "All");
            List<CampusEvent> list = eventService.searchEvents(query, status);
            sendJson(exchange, 200, list);
        } else if ("POST".equals(method)) {
            Map<String, Object> body = readJsonBody(exchange);
            String name = (String) body.get("eventName");
            String desc = (String) body.get("description");
            String dateTime = (String) body.get("dateTime");
            String venue = (String) body.get("venue");
            String coord = (String) body.getOrDefault("coordinator", user.getFullName());

            CampusEvent ev = eventService.createEvent(user, name, desc, dateTime, venue, coord);
            sendJson(exchange, 201, ev);
        }
    }

    // -------------------------------------------------------------
    // COMPLAINTS
    // -------------------------------------------------------------

    private void handleComplaints(HttpExchange exchange, String method, String path, User user) throws Exception {
        if ("/api/complaints/review".equals(path) && "POST".equals(method)) {
            AccessControl.requireAdmin(user);
            Map<String, Object> body = readJsonBody(exchange);
            String cId = (String) body.get("complaintId");
            String status = (String) body.get("status");
            String handler = (String) body.get("assignedHandler");
            String notes = (String) body.get("resolutionNotes");

            complaintService.updateComplaintStatus(user, cId, status, handler, notes);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Grievance status updated to " + status);
            sendJson(exchange, 200, resp);
        } else if ("GET".equals(method)) {
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            String query = q.getOrDefault("q", "");
            String st = q.getOrDefault("status", "All");
            String cat = q.getOrDefault("category", "All");

            List<Complaint> list = complaintService.searchComplaints(user, query, st, cat);
            sendJson(exchange, 200, list);
        } else if ("POST".equals(method)) {
            Map<String, Object> body = readJsonBody(exchange);
            String sub = (String) body.get("subject");
            String cat = (String) body.get("category");
            String desc = (String) body.get("description");

            Complaint c = complaintService.submitComplaint(user, sub, cat, desc);
            sendJson(exchange, 201, c);
        }
    }

    // -------------------------------------------------------------
    // REPORTS & AUDITS
    // -------------------------------------------------------------

    private void handleReports(HttpExchange exchange, String method, String path, User user) throws Exception {
        if ("/api/reports/analytics".equals(path) && "GET".equals(method)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("deptBreakdown", reportService.getStudentsByDepartment());
            data.put("complaintBreakdown", reportService.getComplaintsByStatus());
            data.put("campusAttendance", attendanceService.calculateCampusOverallPercentage());
            sendJson(exchange, 200, data);
        } else if ("/api/reports/low-attendance".equals(path) && "GET".equals(method)) {
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            double t = 75.0;
            try {
                if (q.containsKey("threshold")) t = Double.parseDouble(q.get("threshold"));
            } catch (Exception ignored) {}
            List<ReportService.LowAttendanceRecord> list = reportService.generateLowAttendanceReport(t);
            sendJson(exchange, 200, list);
        } else if (("/api/reports/export-csv".equals(path) || "/api/export/csv".equals(path)) && "GET".equals(method)) {
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            String entity = q.getOrDefault("entity", "");
            if ("students".equalsIgnoreCase(entity)) {
                Path csvP = dataManager.getDataDir().resolve("students.csv");
                if (Files.exists(csvP)) {
                    byte[] bytes = Files.readAllBytes(csvP);
                    exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
                    exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"students.csv\"");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    return;
                }
            } else if ("attendance".equalsIgnoreCase(entity)) {
                Path csvP = dataManager.getDataDir().resolve("attendance.csv");
                if (Files.exists(csvP)) {
                    byte[] bytes = Files.readAllBytes(csvP);
                    exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
                    exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"attendance.csv\"");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    return;
                }
            }

            double t = 75.0;
            try { if (q.containsKey("threshold")) t = Double.parseDouble(q.get("threshold")); } catch (Exception ignored) {}

            File tempF = File.createTempFile("campus_low_att_", ".csv");
            reportService.exportLowAttendanceToCsv(tempF, t);

            exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"low_attendance_report.csv\"");
            byte[] bytes = Files.readAllBytes(tempF.toPath());
            tempF.delete();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        } else if ("/api/reports/export-summary".equals(path) && "GET".equals(method)) {
            File tempF = File.createTempFile("campus_summary_", ".txt");
            reportService.exportCampusSummaryReport(tempF);

            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"campusflow_executive_report.txt\"");
            byte[] bytes = Files.readAllBytes(tempF.toPath());
            tempF.delete();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        } else {
            sendError(exchange, 404, "Endpoint not found: " + path);
        }
    }

    // -------------------------------------------------------------
    // SETTINGS & BACKUPS
    // -------------------------------------------------------------

    private void handleSettings(HttpExchange exchange, String method, String path, User user) throws Exception {
        AccessControl.requireAdmin(user);

        if ("/api/settings/users".equals(path) && "GET".equals(method)) {
            List<Map<String, Object>> safeUsers = dataManager.getUsers().values().stream()
                    .map(this::userToSafeMap)
                    .sorted((a, b) -> String.valueOf(a.get("userId")).compareTo(String.valueOf(b.get("userId"))))
                    .collect(Collectors.toList());
            sendJson(exchange, 200, safeUsers);
        } else if ("/api/settings/toggle-status".equals(path) && "POST".equals(method)) {
            Map<String, Object> body = readJsonBody(exchange);
            String targetId = (String) body.get("userId");
            if (user.getUserId().equalsIgnoreCase(targetId)) {
                throw new IllegalArgumentException("Cannot toggle status of own administrative account.");
            }
            User target = dataManager.getUsers().get(targetId);
            if (target == null) throw new IllegalArgumentException("User not found.");

            target.setStatus(target.getStatus() == UserStatus.ACTIVE ? UserStatus.INACTIVE : UserStatus.ACTIVE);
            dataManager.saveUsers();

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("status", target.getStatus().name());
            sendJson(exchange, 200, resp);
        } else if (("/api/settings/backup".equals(path) || "/api/backup".equals(path)) && "POST".equals(method)) {
            String pathStr = dataManager.createBackup();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("backupPath", pathStr);
            resp.put("backupName", Paths.get(pathStr).getFileName().toString());
            sendJson(exchange, 200, resp);
        } else if ("/api/settings/backups".equals(path) && "GET".equals(method)) {
            Path backupDir = dataManager.getDataDir().resolve("backups");
            List<String> backupNames = new ArrayList<>();
            if (Files.exists(backupDir)) {
                try (var stream = Files.list(backupDir)) {
                    stream.filter(Files::isDirectory).map(p -> p.getFileName().toString()).forEach(backupNames::add);
                }
            }
            sendJson(exchange, 200, backupNames);
        } else if ("/api/settings/restore".equals(path) && "POST".equals(method)) {
            Map<String, Object> body = readJsonBody(exchange);
            String name = (String) body.get("backupName");
            Path targetBackup = dataManager.getDataDir().resolve("backups").resolve(name);
            boolean ok = dataManager.restoreBackup(targetBackup);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", ok);
            sendJson(exchange, ok ? 200 : 400, resp);
        } else {
            sendError(exchange, 404, "Endpoint not found: " + path);
        }
    }

    // -------------------------------------------------------------
    // DESKTOP DATA SYNCHRONIZATION
    // -------------------------------------------------------------

    private void handleDesktopSync(HttpExchange exchange, String method, String path, User user) throws Exception {
        AccessControl.requireAdmin(user);
        Path desktopDataDir = Paths.get("../campus_data").normalize().toAbsolutePath();
        if (!Files.exists(desktopDataDir)) {
            desktopDataDir = Paths.get("campus_data").toAbsolutePath();
        }

        if ("/api/sync/desktop-status".equals(path) && "GET".equals(method)) {
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("desktopPath", desktopDataDir.toString());
            status.put("desktopExists", Files.exists(desktopDataDir));
            status.put("webRecordCount", dataManager.getStudents().size());
            sendJson(exchange, 200, status);
        } else if ("/api/sync/desktop-import".equals(path) && "POST".equals(method)) {
            if (!Files.exists(desktopDataDir)) {
                throw new IllegalArgumentException("Desktop campus_data directory not found.");
            }
            // Copy files from desktop directory to web directory
            File[] files = desktopDataDir.toFile().listFiles((dir, name) -> name.endsWith(".csv"));
            if (files != null) {
                for (File f : files) {
                    Files.copy(f.toPath(), dataManager.getDataDir().resolve(f.getName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
            dataManager.loadAllData();
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Imported records from Desktop application successfully.");
            sendJson(exchange, 200, resp);
        } else if (("/api/sync/desktop-export".equals(path) || "/api/sync/desktop".equals(path)) && "POST".equals(method)) {
            if (!Files.exists(desktopDataDir)) {
                Files.createDirectories(desktopDataDir);
            }
            File[] files = dataManager.getDataDir().toFile().listFiles((dir, name) -> name.endsWith(".csv"));
            if (files != null) {
                for (File f : files) {
                    Files.copy(f.toPath(), desktopDataDir.resolve(f.getName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Exported records to Desktop application storage successfully.");
            sendJson(exchange, 200, resp);
        } else {
            sendError(exchange, 404, "Endpoint not found: " + path);
        }
    }

    // -------------------------------------------------------------
    // HELPERS & JSON RESPONSES
    // -------------------------------------------------------------

    private Map<String, Object> userToSafeMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.getUserId());
        m.put("username", u.getUsername());
        m.put("fullName", u.getFullName());
        m.put("role", u.getRole().name());
        m.put("roleDisplayName", u.getRole().getDisplayName());
        m.put("status", u.getStatus().name());
        m.put("mustChangePassword", u.isMustChangePassword());
        m.put("createdAt", u.getCreatedAt());
        m.put("lastLogin", u.getLastLogin());
        return m;
    }

    private Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        String body = new String(baos.toByteArray(), StandardCharsets.UTF_8).trim();
        return JsonUtils.parseJsonObject(body);
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                try {
                    map.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8"));
                } catch (Exception ignored) {}
            } else if (pair.length == 1) {
                map.put(pair[0], "");
            }
        }
        return map;
    }

    private int parseNumber(Object obj, int defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private boolean parseBoolean(Object obj, boolean defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Boolean) return (Boolean) obj;
        return Boolean.parseBoolean(obj.toString().trim());
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = JsonUtils.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> err = new HashMap<>();
        err.put("error", true);
        err.put("message", message != null ? message : "An error occurred");
        sendJson(exchange, statusCode, err);
    }
}
