package test;

import com.sun.net.httpserver.HttpServer;
import model.*;
import security.PasswordHasher;
import service.*;
import storage.WebDataManager;
import web.ApiHandler;
import web.JsonUtils;
import web.SessionManager;
import web.StaticFileHandler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WebIntegrationTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("   CAMPUSFLOW MOBILE WEB APPLICATION - INTEGRATION TEST SUITE   ");
        System.out.println("=================================================================");

        long startTime = System.currentTimeMillis();

        testJsonUtils();
        testPasswordHasher();
        testDataManagerAndSeedData();
        testSessionManager();
        testAuthService();
        testStudentCrud();
        testAttendanceAndReports();
        testComplaintsDesk();
        testNoticeAndEvents();
        testHttpServerAndEndpoints();

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("=================================================================");
        System.out.printf("TEST SUMMARY: %d/%d assertions passed successfully in %d ms.\n", testsPassed, testsRun, duration);
        System.out.println("=================================================================");

        if (testsPassed != testsRun) {
            System.err.println("Some tests failed!");
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.println("  [FAIL] " + testName);
        }
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        testsRun++;
        boolean match = Objects.equals(expected, actual);
        if (match) {
            testsPassed++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.println("  [FAIL] " + testName + " - expected: " + expected + ", got: " + actual);
        }
    }

    private static void testJsonUtils() {
        System.out.println("\n--- Testing JsonUtils Parser & Serializer ---");
        Map<String, Object> map = new HashMap<>();
        map.put("name", "CampusFlow");
        map.put("active", true);
        map.put("port", 8080);
        map.put("tags", Arrays.asList("mobile", "pwa", "cyber-navy"));

        String json = JsonUtils.toJson(map);
        assertTrue("JsonUtils serializes object to non-empty string", json != null && json.contains("CampusFlow"));
        assertTrue("JsonUtils includes boolean and number", json.contains("true") && json.contains("8080"));

        Map<String, Object> parsed = JsonUtils.parseJsonObject(json);
        assertEquals("Parsed JSON preserves string property", "CampusFlow", parsed.get("name"));
        assertEquals("Parsed JSON preserves boolean", Boolean.TRUE, parsed.get("active"));
        assertEquals("Parsed JSON preserves number", 8080L, ((Number) parsed.get("port")).longValue());
    }

    private static void testPasswordHasher() {
        System.out.println("\n--- Testing PBKDF2 PasswordHasher ---");
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword("Admin@123", salt);
        assertTrue("PasswordHasher generates hex hash", hash != null && hash.length() >= 32);
        assertTrue("PasswordHasher verifies correct password", PasswordHasher.verifyPassword("Admin@123", salt, hash));
        assertTrue("PasswordHasher rejects wrong password", !PasswordHasher.verifyPassword("WrongPassword", salt, hash));
    }

    private static void testDataManagerAndSeedData() {
        System.out.println("\n--- Testing WebDataManager & CSV Persistence ---");
        WebDataManager dm = WebDataManager.getInstance();
        assertTrue("Data manager loads seed users", dm.getUsers().size() >= 5);
        assertTrue("Admin user exists in users table", dm.findUserByUsername("admin").isPresent());
        assertTrue("Alex Chen student exists", dm.findUserByUsername("alex.chen").isPresent());
        assertTrue("Robert Vance faculty exists", dm.findUserByUsername("robert.vance").isPresent());
        assertTrue("Seed courses loaded", dm.getCourses().size() >= 4);
        assertTrue("Seed students loaded", dm.getStudents().size() >= 5);
        assertTrue("Seed complaints loaded", dm.getComplaints().size() >= 3);
        assertTrue("Seed notices loaded", dm.getNotices().size() >= 3);
        assertTrue("Seed events loaded", dm.getEvents().size() >= 2);
    }

    private static void testSessionManager() {
        System.out.println("\n--- Testing SessionManager In-Memory Security Tokens ---");
        SessionManager sm = SessionManager.getInstance();
        User admin = WebDataManager.getInstance().findUserByUsername("admin").get();
        String token = sm.createSession(admin);
        assertTrue("Session manager generates valid token string", token != null && token.length() >= 32);

        User sessionUser = sm.getUserByToken(token);
        assertTrue("Session manager retrieves active session user", sessionUser != null);
        assertEquals("Session username matches", "admin", sessionUser.getUsername());
        assertEquals("Session role matches ADMIN", Role.ADMIN, sessionUser.getRole());

        sm.invalidateToken(token);
        assertTrue("Invalidated token returns null", sm.getUserByToken(token) == null);
    }

    private static void testAuthService() {
        System.out.println("\n--- Testing WebAuthService ---");
        WebDataManager dm = WebDataManager.getInstance();
        WebAuthService auth = new WebAuthService(dm);

        try {
            User admin = auth.login("admin", "Admin@123");
            assertTrue("Admin authenticates successfully", admin != null && admin.getRole() == Role.ADMIN);
        } catch (Exception e) {
            assertTrue("Admin login threw exception: " + e.getMessage(), false);
        }

        try {
            User faculty = auth.login("robert.vance", "Faculty@123");
            assertTrue("Faculty authenticates successfully", faculty != null && faculty.getRole() == Role.FACULTY);
        } catch (Exception e) {
            assertTrue("Faculty login threw exception: " + e.getMessage(), false);
        }

        try {
            User student = auth.login("alex.chen", "Student@123");
            assertTrue("Student authenticates successfully", student != null && student.getRole() == Role.STUDENT);
        } catch (Exception e) {
            assertTrue("Student login threw exception: " + e.getMessage(), false);
        }

        try {
            auth.login("admin", "BadPassword!");
            assertTrue("Bad credentials should have failed", false);
        } catch (Exception e) {
            assertTrue("Bad credentials correctly rejected with exception", true);
        }
    }

    private static void testStudentCrud() {
        System.out.println("\n--- Testing StudentService Operations ---");
        WebDataManager dm = WebDataManager.getInstance();
        StudentService ss = new StudentService(dm);

        try {
            String testEmail = "test_" + System.currentTimeMillis() + "@campusflow.edu";
            Student newStudent = ss.createStudent("Integration Test Student", "Computer Science", 3, testEmail, "+1-555-8888", "ACTIVE", false, null);
            assertTrue("Student created successfully", newStudent != null && newStudent.getStudentId() != null);

            Student fetched = ss.getStudent(newStudent.getStudentId());
            assertTrue("Student fetched by ID", fetched != null && fetched.getEmail().equals(testEmail));

            fetched.setFullName("Updated Test Student");
            ss.updateStudent(fetched);
            Student updated = ss.getStudent(newStudent.getStudentId());
            assertEquals("Student name updated", "Updated Test Student", updated.getFullName());

            ss.deactivateStudent(newStudent.getStudentId());
            Student deactivated = ss.getStudent(newStudent.getStudentId());
            assertEquals("Student deactivated", "INACTIVE", deactivated.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Student CRUD threw exception: " + e.getMessage(), false);
        }
    }

    private static void testAttendanceAndReports() {
        System.out.println("\n--- Testing Attendance & Low-Attendance Alert Reports ---");
        WebDataManager dm = WebDataManager.getInstance();
        AttendanceService as = new AttendanceService(dm);
        ReportService rs = new ReportService(dm, as);
        User admin = dm.findUserByUsername("admin").get();

        Student alex = dm.getStudents().get("STU001");
        if (alex == null) {
            alex = dm.getStudents().values().iterator().next();
        }
        assertTrue("Student found for attendance check", alex != null);

        // Record attendance
        try {
            as.recordAttendance(admin, alex.getStudentId(), "CS301", "2026-10-25", AttendanceStatus.PRESENT, "Mobile Attendance Check");
            assertTrue("Recorded attendance session", true);
        } catch (Exception e) {
            assertTrue("Recording attendance threw: " + e.getMessage(), false);
        }

        double alexPct = as.calculateStudentOverallPercentage(alex.getStudentId());
        assertTrue("Attendance percentage calculated", alexPct > 0.0 && alexPct <= 100.0);

        List<ReportService.LowAttendanceRecord> alerts = rs.generateLowAttendanceReport(75.0);
        assertTrue("Low-attendance report generated without exception", alerts != null);

        double campusAvg = as.calculateCampusOverallPercentage();
        assertTrue("Campus average calculated", campusAvg >= 0.0 && campusAvg <= 100.0);
    }

    private static void testComplaintsDesk() {
        System.out.println("\n--- Testing Complaints Desk Workflow ---");
        WebDataManager dm = WebDataManager.getInstance();
        ComplaintService cs = new ComplaintService(dm);
        User studentUser = dm.findUserByUsername("alex.chen").get();
        User adminUser = dm.findUserByUsername("admin").get();

        try {
            Complaint c = cs.submitComplaint(studentUser, "Wi-Fi Drops in Lab 3", "IT_SERVICES", "Signal fluctuates during afternoon coding labs.");
            assertTrue("Complaint filed successfully", c != null && "PENDING".equalsIgnoreCase(c.getStatus()));

            cs.updateComplaintStatus(adminUser, c.getComplaintId(), "IN_PROGRESS", "Network Eng", "Assigned to network engineer.");
            Complaint updated = dm.getComplaints().get(c.getComplaintId());
            assertEquals("Complaint moved to IN_PROGRESS", "IN_PROGRESS", updated.getStatus());

            cs.updateComplaintStatus(adminUser, c.getComplaintId(), "RESOLVED", "Network Eng", "Access point antenna replaced.");
            Complaint resolved = dm.getComplaints().get(c.getComplaintId());
            assertEquals("Complaint marked RESOLVED", "RESOLVED", resolved.getStatus());
            assertTrue("Resolution notes present", resolved.getResolutionNotes().contains("Access point"));
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Complaints desk threw exception: " + e.getMessage(), false);
        }
    }

    private static void testNoticeAndEvents() {
        System.out.println("\n--- Testing Notices & Campus Events ---");
        WebDataManager dm = WebDataManager.getInstance();
        NoticeService ns = new NoticeService(dm);
        EventService es = new EventService(dm);
        User admin = dm.findUserByUsername("admin").get();

        try {
            Notice n = ns.createNotice(admin, "Final Examination Schedule", "ACADEMIC", "Exams commence Dec 15.");
            assertTrue("Notice posted", n != null && n.getNoticeId() != null);
            assertTrue("Active notices list includes new notice", ns.getActiveNotices().stream().anyMatch(x -> x.getNoticeId().equals(n.getNoticeId())));
        } catch (Exception e) {
            assertTrue("Notice posting threw: " + e.getMessage(), false);
        }

        List<CampusEvent> events = es.getAllEvents();
        assertTrue("Events list populated", !events.isEmpty());
    }

    private static void testHttpServerAndEndpoints() {
        System.out.println("\n--- Testing Embedded HTTP Server REST & Static Endpoints ---");
        int testPort = 18080;
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", testPort), 0);
            server.createContext("/api", new ApiHandler());
            server.createContext("/", new StaticFileHandler());
            server.start();

            // 1. Test Static files
            HttpResponse resHtml = httpGet("http://127.0.0.1:" + testPort + "/index.html", null);
            assertEquals("GET /index.html returns 200 OK", 200, resHtml.statusCode);
            assertTrue("HTML contains CampusFlow title", resHtml.body.contains("CampusFlow"));

            HttpResponse resCss = httpGet("http://127.0.0.1:" + testPort + "/styles.css", null);
            assertEquals("GET /styles.css returns 200 OK", 200, resCss.statusCode);
            assertTrue("CSS contains Cyber Navy color variables", resCss.body.contains("--bg-dark: #0B1020"));

            HttpResponse resJs = httpGet("http://127.0.0.1:" + testPort + "/app.js", null);
            assertEquals("GET /app.js returns 200 OK", 200, resJs.statusCode);
            assertTrue("JS contains serviceWorker registration", resJs.body.contains("serviceWorker"));

            HttpResponse resManifest = httpGet("http://127.0.0.1:" + testPort + "/manifest.json", null);
            assertEquals("GET /manifest.json returns 200 OK", 200, resManifest.statusCode);

            // 2. Test Login API endpoint
            Map<String, Object> loginPayload = new HashMap<>();
            loginPayload.put("username", "admin");
            loginPayload.put("password", "Admin@123");
            HttpResponse resLogin = httpPost("http://127.0.0.1:" + testPort + "/api/auth/login", JsonUtils.toJson(loginPayload), null);
            assertEquals("POST /api/auth/login returns 200 OK", 200, resLogin.statusCode);
            Map<String, Object> loginResp = JsonUtils.parseJsonObject(resLogin.body);
            assertTrue("Login returns success flag", Boolean.TRUE.equals(loginResp.get("success")));
            String token = (String) loginResp.get("token");
            assertTrue("Login returns authentication bearer token", token != null && !token.isEmpty());

            // 3. Test Dashboard API endpoint with Bearer token
            HttpResponse resDash = httpGet("http://127.0.0.1:" + testPort + "/api/dashboard", token);
            assertEquals("GET /api/dashboard returns 200 OK", 200, resDash.statusCode);
            assertTrue("Dashboard returns admin metrics", resDash.body.contains("totalStudents"));

            // 4. Test Students API endpoint
            HttpResponse resStudents = httpGet("http://127.0.0.1:" + testPort + "/api/students", token);
            assertEquals("GET /api/students returns 200 OK", 200, resStudents.statusCode);
            assertTrue("Students list contains Alex Chen", resStudents.body.contains("Alex Chen"));

            // 5. Test Low-attendance report endpoint
            HttpResponse resAlerts = httpGet("http://127.0.0.1:" + testPort + "/api/reports/low-attendance", token);
            assertEquals("GET /api/reports/low-attendance returns 200 OK", 200, resAlerts.statusCode);

            // 6. Test CSV Export endpoint
            HttpResponse resCsv = httpGet("http://127.0.0.1:" + testPort + "/api/export/csv?entity=students", token);
            assertEquals("GET /api/export/csv returns 200 OK", 200, resCsv.statusCode);
            assertTrue("CSV content has student data", resCsv.body.contains("studentId") || resCsv.body.contains("fullName"));

            // 7. Test Backup endpoint
            HttpResponse resBackup = httpPost("http://127.0.0.1:" + testPort + "/api/backup", "{}", token);
            assertEquals("POST /api/backup returns 200 OK", 200, resBackup.statusCode);
            assertTrue("Backup response contains backupPath", resBackup.body.contains("backupPath"));

            // 8. Test Desktop Sync endpoint
            HttpResponse resSync = httpPost("http://127.0.0.1:" + testPort + "/api/sync/desktop", "{}", token);
            assertEquals("POST /api/sync/desktop returns 200 OK", 200, resSync.statusCode);
            assertTrue("Desktop sync indicates success", resSync.body.contains("success"));

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Server test encountered exception: " + e.getMessage(), false);
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    private static class HttpResponse {
        int statusCode;
        String body;
        HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    private static HttpResponse httpGet(String urlStr, String token) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
        String body = "";
        if (is != null) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new HttpResponse(code, body);
    }

    private static HttpResponse httpPost(String urlStr, String jsonBody, String token) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
        String body = "";
        if (is != null) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new HttpResponse(code, body);
    }
}
