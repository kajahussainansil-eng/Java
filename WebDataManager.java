package storage;

import model.*;
import security.PasswordHasher;
import util.DateUtils;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data management and persistence layer for CampusFlow Web.
 * Operates in web_data/ directory with RFC-4180 atomic CSV persistence.
 */
public class WebDataManager {

    private static WebDataManager instance;

    private final Path dataDir;
    private final File usersFile;
    private final File studentsFile;
    private final File facultyFile;
    private final File coursesFile;
    private final File attendanceFile;
    private final File noticesFile;
    private final File eventsFile;
    private final File complaintsFile;
    private final File auditFile;

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Student> students = new ConcurrentHashMap<>();
    private final Map<String, Faculty> faculties = new ConcurrentHashMap<>();
    private final Map<String, Course> courses = new ConcurrentHashMap<>();
    private final List<Attendance> attendances = new CopyOnWriteArrayList<>();
    private final Map<String, Notice> notices = new ConcurrentHashMap<>();
    private final Map<String, CampusEvent> events = new ConcurrentHashMap<>();
    private final Map<String, Complaint> complaints = new ConcurrentHashMap<>();
    private final List<AuditLog> auditLogs = new CopyOnWriteArrayList<>();

    private int maxAdminSeq = 1000;
    private int maxFacultySeq = 1000;
    private int maxStudentSeq = 1000;
    private int maxNoticeSeq = 1000;
    private int maxEventSeq = 1000;
    private int maxComplaintSeq = 1000;
    private int maxAttendanceSeq = 10000;

    private WebDataManager() {
        String envDir = System.getenv("DATA_DIR");
        if (envDir != null && !envDir.trim().isEmpty()) {
            this.dataDir = Paths.get(envDir.trim()).toAbsolutePath();
        } else {
            this.dataDir = Paths.get("web_data").toAbsolutePath();
        }
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            System.err.println("Failed to create web_data directory: " + e.getMessage());
        }

        this.usersFile = dataDir.resolve("users.csv").toFile();
        this.studentsFile = dataDir.resolve("students.csv").toFile();
        this.facultyFile = dataDir.resolve("faculty.csv").toFile();
        this.coursesFile = dataDir.resolve("courses.csv").toFile();
        this.attendanceFile = dataDir.resolve("attendance.csv").toFile();
        this.noticesFile = dataDir.resolve("notices.csv").toFile();
        this.eventsFile = dataDir.resolve("events.csv").toFile();
        this.complaintsFile = dataDir.resolve("complaints.csv").toFile();
        this.auditFile = dataDir.resolve("audit.csv").toFile();

        loadAllData();

        // If newly created and empty, seed initial realistic dataset
        if (users.isEmpty()) {
            seedSampleDataset();
        }
    }

    public static synchronized WebDataManager getInstance() {
        if (instance == null) {
            instance = new WebDataManager();
        }
        return instance;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public synchronized void loadAllData() {
        loadUsers();
        loadStudents();
        loadFaculty();
        loadCourses();
        loadAttendance();
        loadNotices();
        loadEvents();
        loadComplaints();
        loadAuditLogs();
        calculateMaxSequences();
    }

    private void loadUsers() {
        users.clear();
        if (!usersFile.exists()) return;
        try {
            List<List<String>> rows = CsvStorage.readCsv(usersFile);
            boolean first = true;
            for (List<String> row : rows) {
                if (first) { first = false; continue; }
                if (row.size() < 8) continue;
                String userId = row.get(0);
                String username = row.get(1);
                String hash = row.get(2);
                String salt = row.get(3);
                String fullName = row.get(4);
                Role role = Role.fromString(row.get(5));
                UserStatus status = UserStatus.fromString(row.get(6));
                boolean mustChange = Boolean.parseBoolean(row.get(7));
                String created = row.size() > 8 ? row.get(8) : "";
                String lastLogin = row.size() > 9 ? row.get(9) : "";

                User u = new User(userId, username, hash, salt, fullName, role, status, mustChange, created, lastLogin);
                users.put(userId, u);
            }
        } catch (Exception e) {
            System.err.println("Warning: Error loading web users: " + e.getMessage());
        }
    }

    private void loadStudents() {
        students.clear();
        if (!studentsFile.exists()) return;
        try {
            List<List<String>> rows = CsvStorage.readCsv(studentsFile);
            boolean first = true;
            for (List<String> row : rows) {
                if (first) { first = false; continue; }
                if (row.size() < 7) continue;
                String id = row.get(0);
                String name = row.get(1);
                String dept = row.get(2);
                int sem = 1;
                try { sem = Integer.parseInt(row.get(3)); } catch (Exception ignored) {}
                String email = row.get(4);
                String phone = row.get(5);
                String date = row.get(6);
                String status = row.size() > 7 ? row.get(7) : "ACTIVE";

                Student s = new Student(id, name, dept, sem, email, phone, date, status);
                students.put(id, s);
            }
        } catch (Exception e) {
            System.err.println("Warning: Error loading web students: " + e.getMessage());
        }
    }

    private void loadFaculty() {
        faculties.clear();
        if (!facultyFile.exists()) return;
        try {
            List<List<String>> rows = CsvStorage.readCsv(facultyFile);
            boolean first = true;
            for (List<String> row : rows) {
                if (first) { first = false; continue; }
                if (row.size() < 6) continue;
                String id = row.get(0);
                String name = row.get(1);
                String dept = row.get(2);
                String desig = row.get(3);
                String email = row.get(4);
                String phone = row.get(5);
                String status = row.size() > 6 ? row.get(6) : "ACTIVE";

                Faculty f = new Faculty(id, name, dept, desig, email, phone, status);
                faculties.put(id, f);
            }
        } catch (Exception e) {
            System.err.println("Warning: Error loading web faculty: " + e.getMessage());
        }
    }

    private void loadCourses() {
        courses.clear();
        if (!coursesFile.exists()) return;
        try {
            List<List<String>> rows = CsvStorage.readCsv(coursesFile);
            boolean first = true;
            for (List<String> row : rows) {
                if (first) { first = false; continue; }
                if (row.size() < 6) continue;
                String code = row.get(0).toUpperCase();
                String name = row.get(1);
                String dept = row.get(2);
                int sem = 1;
                int credits = 3;
                try { sem = Integer.parseInt(row.get(3)); } catch (Exception ignored) {}
                try { credits = Integer.parseInt(row.get(4)); } catch (Exception ignored) {}
                String facId = row.get(5);
                String status = row.size() > 6 ? row.get(6) : "ACTIVE";

                Course c = new Course(code, name, dept, sem, credits, facId, status);
                courses.put(code, c);
            }
        } catch (Exception e) {
            System.err.println("Warning: Error loading web courses: " + e.getMessage());
        }
    }

    private void loadAttendance() {
        attendances.clear();
        if (!attendanceFile.exists()) return;
        try {
            List<List<String>> rows = CsvStorage.readCsv(attendanceFile);
            boolean first = true;
            for (List<String> row : rows) {
                if (first) { first = false; continue; }
                if (row.size() < 5) continue;
                String id = row.get(0);
                String sId = row.get(1);
                String cCode = row.get(2).toUpperCase();
                String date = row.get(3);
                AttendanceStatus status = AttendanceStatus.fromString(row.get(4));
                String remarks = row.size() > 5 ? row.get(5) : "";

                Attendance a = new Attendance(id, sId, cCode, date, status, remarks);
                attendances.add(a);
            }
        } catch (Exception e) {
            System.err.println("Warning: Error loading web attendance: " + e.getMessage());
        }
    }

    private void loadNotices() {
        notices.clear();
        if (!noticesFile.exists()) return;
        try {
            List<List<String>> rows = CsvStorage.readCsv(noticesFile);
            boolean first = true;
            for (List<String> row : rows) {
                if (first) { first = false; continue; }
                if (row.size() < 6) continue;
                String id = row.get(0);
                String title = row.get(1);
                String cat = row.get(2);
                String date = row.get(3);
                String desc = row.get(4);
                String by = row.get(5);
                String status = row.size() > 6 ? row.get(6) : "ACTIVE";

                Notice n = new Notice(id, title, cat, date, desc, by, status);
                notices.put(id, n);
            }
        } catch (Exception e) {
            System.err.println("Warning: Error loading web notices: " + e.getMessage());
        }
    }

    private void loadEvents() {
        events.clear();
        if (!eventsFile.exists()) return;
        try {
            List<List<String>> rows = CsvStorage.readCsv(eventsFile);
            boolean first = true;
            for (List<String> row : rows) {
                if (first) { first = false; continue; }
                if (row.size() < 6) continue;
                String id = row.get(0);
                String name = row.get(1);
                String desc = row.get(2);
                String dt = row.get(3);
                String venue = row.get(4);
                String coord = row.get(5);
                String status = row.size() > 6 ? row.get(6) : "UPCOMING";

                CampusEvent ev = new CampusEvent(id, name, desc, dt, venue, coord, status);
                events.put(id, ev);
            }
        } catch (Exception e) {
            System.err.println("Warning: Error loading web events: " + e.getMessage());
        }
    }

    private void loadComplaints() {
        complaints.clear();
        if (!complaintsFile.exists()) return;
        try {
            List<List<String>> rows = CsvStorage.readCsv(complaintsFile);
            boolean first = true;
            for (List<String> row : rows) {
                if (first) { first = false; continue; }
                if (row.size() < 7) continue;
                String id = row.get(0);
                String sId = row.get(1);
                String subj = row.get(2);
                String cat = row.get(3);
                String desc = row.get(4);
                String date = row.get(5);
                String status = row.get(6);
                String handler = row.size() > 7 ? row.get(7) : "Campus Administration";
                String notes = row.size() > 8 ? row.get(8) : "";

                Complaint c = new Complaint(id, sId, subj, cat, desc, date, status, handler, notes);
                complaints.put(id, c);
            }
        } catch (Exception e) {
            System.err.println("Warning: Error loading web complaints: " + e.getMessage());
        }
    }

    private void loadAuditLogs() {
        auditLogs.clear();
        if (!auditFile.exists()) return;
        try {
            List<List<String>> rows = CsvStorage.readCsv(auditFile);
            boolean first = true;
            for (List<String> row : rows) {
                if (first) { first = false; continue; }
                if (row.size() < 5) continue;
                String id = row.get(0);
                String ts = row.get(1);
                String uId = row.get(2);
                String action = row.get(3);
                String details = row.get(4);

                auditLogs.add(new AuditLog(id, ts, uId, action, details));
            }
        } catch (Exception e) {
            System.err.println("Warning: Error loading web audit: " + e.getMessage());
        }
    }

    private void calculateMaxSequences() {
        for (User u : users.values()) {
            extractSeq(u.getUserId(), "ADM", seq -> maxAdminSeq = Math.max(maxAdminSeq, seq));
            extractSeq(u.getUserId(), "FAC", seq -> maxFacultySeq = Math.max(maxFacultySeq, seq));
            extractSeq(u.getUserId(), "STU", seq -> maxStudentSeq = Math.max(maxStudentSeq, seq));
        }
        for (Student s : students.values()) {
            extractSeq(s.getStudentId(), "STU", seq -> maxStudentSeq = Math.max(maxStudentSeq, seq));
        }
        for (Faculty f : faculties.values()) {
            extractSeq(f.getFacultyId(), "FAC", seq -> maxFacultySeq = Math.max(maxFacultySeq, seq));
        }
        for (Notice n : notices.values()) {
            extractSeq(n.getNoticeId(), "NOT", seq -> maxNoticeSeq = Math.max(maxNoticeSeq, seq));
        }
        for (CampusEvent ev : events.values()) {
            extractSeq(ev.getEventId(), "EVT", seq -> maxEventSeq = Math.max(maxEventSeq, seq));
        }
        for (Complaint c : complaints.values()) {
            extractSeq(c.getComplaintId(), "CMP", seq -> maxComplaintSeq = Math.max(maxComplaintSeq, seq));
        }
        for (Attendance a : attendances) {
            extractSeq(a.getAttendanceId(), "ATT", seq -> maxAttendanceSeq = Math.max(maxAttendanceSeq, seq));
        }
    }

    private void extractSeq(String id, String prefix, java.util.function.IntConsumer consumer) {
        if (id != null && id.toUpperCase().startsWith(prefix)) {
            try {
                int num = Integer.parseInt(id.substring(prefix.length()));
                consumer.accept(num);
            } catch (NumberFormatException ignored) {}
        }
    }

    // Sequence ID generators
    public synchronized String generateNextAdminId() { return "ADM" + (++maxAdminSeq); }
    public synchronized String generateNextFacultyId() { return "FAC" + (++maxFacultySeq); }
    public synchronized String generateNextStudentId() { return "STU" + (++maxStudentSeq); }
    public synchronized String generateNextNoticeId() { return "NOT" + (++maxNoticeSeq); }
    public synchronized String generateNextEventId() { return "EVT" + (++maxEventSeq); }
    public synchronized String generateNextComplaintId() { return "CMP" + (++maxComplaintSeq); }
    public synchronized String generateNextAttendanceId() { return "ATT" + (++maxAttendanceSeq); }

    // Save routines
    public synchronized void saveUsers() throws IOException {
        List<String> header = Arrays.asList("userId", "username", "passwordHash", "salt",
                "fullName", "role", "status", "mustChangePassword", "createdAt", "lastLogin");
        List<List<String>> rows = new ArrayList<>();
        for (User u : users.values()) {
            rows.add(Arrays.asList(u.getUserId(), u.getUsername(), u.getPasswordHash(), u.getSalt(),
                    u.getFullName(), u.getRole().name(), u.getStatus().name(),
                    String.valueOf(u.isMustChangePassword()), u.getCreatedAt(), u.getLastLogin()));
        }
        CsvStorage.writeCsv(usersFile, header, rows);
    }

    public synchronized void saveStudents() throws IOException {
        List<String> header = Arrays.asList("studentId", "fullName", "department", "semester",
                "email", "phone", "enrollmentDate", "status");
        List<List<String>> rows = new ArrayList<>();
        for (Student s : students.values()) {
            rows.add(Arrays.asList(s.getStudentId(), s.getFullName(), s.getDepartment(),
                    String.valueOf(s.getSemester()), s.getEmail(), s.getPhone(),
                    s.getEnrollmentDate(), s.getStatus()));
        }
        CsvStorage.writeCsv(studentsFile, header, rows);
    }

    public synchronized void saveFaculty() throws IOException {
        List<String> header = Arrays.asList("facultyId", "fullName", "department",
                "designation", "email", "phone", "status");
        List<List<String>> rows = new ArrayList<>();
        for (Faculty f : faculties.values()) {
            rows.add(Arrays.asList(f.getFacultyId(), f.getFullName(), f.getDepartment(),
                    f.getDesignation(), f.getEmail(), f.getPhone(), f.getStatus()));
        }
        CsvStorage.writeCsv(facultyFile, header, rows);
    }

    public synchronized void saveCourses() throws IOException {
        List<String> header = Arrays.asList("courseCode", "courseName", "department",
                "semester", "credits", "assignedFacultyId", "status");
        List<List<String>> rows = new ArrayList<>();
        for (Course c : courses.values()) {
            rows.add(Arrays.asList(c.getCourseCode(), c.getCourseName(), c.getDepartment(),
                    String.valueOf(c.getSemester()), String.valueOf(c.getCredits()),
                    c.getAssignedFacultyId(), c.getStatus()));
        }
        CsvStorage.writeCsv(coursesFile, header, rows);
    }

    public synchronized void saveAttendance() throws IOException {
        List<String> header = Arrays.asList("attendanceId", "studentId", "courseCode",
                "date", "status", "remarks");
        List<List<String>> rows = new ArrayList<>();
        for (Attendance a : attendances) {
            rows.add(Arrays.asList(a.getAttendanceId(), a.getStudentId(), a.getCourseCode(),
                    a.getDate(), a.getStatus().name(), a.getRemarks()));
        }
        CsvStorage.writeCsv(attendanceFile, header, rows);
    }

    public synchronized void saveNotices() throws IOException {
        List<String> header = Arrays.asList("noticeId", "title", "category",
                "publicationDate", "description", "publishedBy", "status");
        List<List<String>> rows = new ArrayList<>();
        for (Notice n : notices.values()) {
            rows.add(Arrays.asList(n.getNoticeId(), n.getTitle(), n.getCategory(),
                    n.getPublicationDate(), n.getDescription(), n.getPublishedBy(), n.getStatus()));
        }
        CsvStorage.writeCsv(noticesFile, header, rows);
    }

    public synchronized void saveEvents() throws IOException {
        List<String> header = Arrays.asList("eventId", "eventName", "description",
                "dateTime", "venue", "coordinator", "status");
        List<List<String>> rows = new ArrayList<>();
        for (CampusEvent ev : events.values()) {
            rows.add(Arrays.asList(ev.getEventId(), ev.getEventName(), ev.getDescription(),
                    ev.getDateTime(), ev.getVenue(), ev.getCoordinator(), ev.getStatus()));
        }
        CsvStorage.writeCsv(eventsFile, header, rows);
    }

    public synchronized void saveComplaints() throws IOException {
        List<String> header = Arrays.asList("complaintId", "studentId", "subject",
                "category", "description", "submissionDate", "status", "assignedHandler", "resolutionNotes");
        List<List<String>> rows = new ArrayList<>();
        for (Complaint c : complaints.values()) {
            rows.add(Arrays.asList(c.getComplaintId(), c.getStudentId(), c.getSubject(),
                    c.getCategory(), c.getDescription(), c.getSubmissionDate(),
                    c.getStatus(), c.getAssignedHandler(), c.getResolutionNotes()));
        }
        CsvStorage.writeCsv(complaintsFile, header, rows);
    }

    public synchronized void saveAuditLogs() throws IOException {
        List<String> header = Arrays.asList("logId", "timestamp", "userId", "action", "details");
        List<List<String>> rows = new ArrayList<>();
        for (AuditLog l : auditLogs) {
            rows.add(Arrays.asList(l.getLogId(), l.getTimestamp(), l.getUserId(), l.getAction(), l.getDetails()));
        }
        CsvStorage.writeCsv(auditFile, header, rows);
    }

    public synchronized void saveAll() {
        try {
            saveUsers();
            saveStudents();
            saveFaculty();
            saveCourses();
            saveAttendance();
            saveNotices();
            saveEvents();
            saveComplaints();
            saveAuditLogs();
        } catch (IOException e) {
            System.err.println("Critical error saving web data: " + e.getMessage());
        }
    }

    public synchronized void logAudit(String userId, String action, String details) {
        String logId = "LOG" + System.currentTimeMillis();
        AuditLog log = new AuditLog(logId, DateUtils.nowString(), userId, action, details);
        auditLogs.add(log);
        try {
            saveAuditLogs();
        } catch (IOException ignored) {}
    }

    // Accessors
    public Map<String, User> getUsers() { return users; }

    public Optional<User> findUserByUsername(String username) {
        if (username == null) return Optional.empty();
        return users.values().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public Map<String, Student> getStudents() { return students; }
    public Map<String, Faculty> getFaculties() { return faculties; }
    public Map<String, Course> getCourses() { return courses; }
    public List<Attendance> getAttendances() { return attendances; }
    public Map<String, Notice> getNotices() { return notices; }
    public Map<String, CampusEvent> getEvents() { return events; }
    public Map<String, Complaint> getComplaints() { return complaints; }
    public List<AuditLog> getAuditLogs() { return auditLogs; }

    // Backup & Restore
    public synchronized String createBackup() throws IOException {
        String ts = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backupDir = dataDir.resolve("backups").resolve("web_backup_" + ts);
        Files.createDirectories(backupDir);

        File[] files = dataDir.toFile().listFiles((dir, name) -> name.endsWith(".csv"));
        if (files != null) {
            for (File f : files) {
                Files.copy(f.toPath(), backupDir.resolve(f.getName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        logAudit("SYSTEM", "BACKUP_CREATED", "Web snapshot created at " + backupDir.getFileName());
        return backupDir.toString();
    }

    public synchronized boolean restoreBackup(Path backupDir) {
        if (!Files.isDirectory(backupDir)) return false;
        try {
            File[] files = backupDir.toFile().listFiles((dir, name) -> name.endsWith(".csv"));
            if (files != null) {
                for (File f : files) {
                    Files.copy(f.toPath(), dataDir.resolve(f.getName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            loadAllData();
            logAudit("SYSTEM", "BACKUP_RESTORED", "Restored web data from " + backupDir.getFileName());
            return true;
        } catch (IOException e) {
            System.err.println("Web restore failed: " + e.getMessage());
            return false;
        }
    }

    // Seed realistic sample college dataset
    private synchronized void seedSampleDataset() {
        // Admin
        String adminPass = System.getenv("ADMIN_PASSWORD");
        if (adminPass == null || adminPass.trim().isEmpty()) {
            adminPass = "Admin@123";
        }
        String saltAdmin = PasswordHasher.generateSalt();
        User admin = new User("ADM1001", "admin", PasswordHasher.hashPassword(adminPass, saltAdmin), saltAdmin,
                "Dr. Arthur Pendelton", Role.ADMIN, UserStatus.ACTIVE, false, DateUtils.nowString(), "");
        users.put(admin.getUserId(), admin);

        // Faculty
        String saltF1 = PasswordHasher.generateSalt();
        String saltF2 = PasswordHasher.generateSalt();
        String saltF3 = PasswordHasher.generateSalt();

        Faculty f1 = new Faculty("FAC1001", "Dr. Robert Vance", "Computer Science", "Professor & HOD", "r.vance@campus.edu", "+1-555-0101", "ACTIVE");
        Faculty f2 = new Faculty("FAC1002", "Prof. Elena Rostova", "Information Technology", "Associate Professor", "e.rostova@campus.edu", "+1-555-0102", "ACTIVE");
        Faculty f3 = new Faculty("FAC1003", "Dr. Alan Turing-Smith", "Artificial Intelligence", "Assistant Professor", "a.smith@campus.edu", "+1-555-0103", "ACTIVE");

        faculties.put(f1.getFacultyId(), f1);
        faculties.put(f2.getFacultyId(), f2);
        faculties.put(f3.getFacultyId(), f3);

        User uF1 = new User("FAC1001", "robert.vance", PasswordHasher.hashPassword("Faculty@123", saltF1), saltF1, "Dr. Robert Vance", Role.FACULTY, UserStatus.ACTIVE, false, DateUtils.nowString(), "");
        User uF2 = new User("FAC1002", "elena.rostova", PasswordHasher.hashPassword("Faculty@123", saltF2), saltF2, "Prof. Elena Rostova", Role.FACULTY, UserStatus.ACTIVE, false, DateUtils.nowString(), "");
        User uF3 = new User("FAC1003", "alan.smith", PasswordHasher.hashPassword("Faculty@123", saltF3), saltF3, "Dr. Alan Turing-Smith", Role.FACULTY, UserStatus.ACTIVE, false, DateUtils.nowString(), "");

        users.put(uF1.getUserId(), uF1);
        users.put(uF2.getUserId(), uF2);
        users.put(uF3.getUserId(), uF3);

        // Students
        String saltS1 = PasswordHasher.generateSalt();
        String saltS2 = PasswordHasher.generateSalt();
        String saltS3 = PasswordHasher.generateSalt();
        String saltS4 = PasswordHasher.generateSalt();
        String saltS5 = PasswordHasher.generateSalt();

        Student s1 = new Student("STU1001", "Alex Chen", "Computer Science", 4, "alex.chen@campus.edu", "+1-555-0201", "2024-08-15", "ACTIVE");
        Student s2 = new Student("STU1002", "Sophia Martinez", "Computer Science", 4, "sophia.m@campus.edu", "+1-555-0202", "2024-08-15", "ACTIVE");
        Student s3 = new Student("STU1003", "David Kim", "Information Technology", 3, "david.kim@campus.edu", "+1-555-0203", "2025-01-10", "ACTIVE");
        Student s4 = new Student("STU1004", "Maya Patel", "Artificial Intelligence", 2, "maya.patel@campus.edu", "+1-555-0204", "2025-08-20", "ACTIVE");
        Student s5 = new Student("STU1005", "Marcus Johnson", "Computer Science", 4, "marcus.j@campus.edu", "+1-555-0205", "2024-08-15", "ACTIVE");

        students.put(s1.getStudentId(), s1);
        students.put(s2.getStudentId(), s2);
        students.put(s3.getStudentId(), s3);
        students.put(s4.getStudentId(), s4);
        students.put(s5.getStudentId(), s5);

        User uS1 = new User("STU1001", "alex.chen", PasswordHasher.hashPassword("Student@123", saltS1), saltS1, "Alex Chen", Role.STUDENT, UserStatus.ACTIVE, false, DateUtils.nowString(), "");
        User uS2 = new User("STU1002", "sophia.m", PasswordHasher.hashPassword("Student@123", saltS2), saltS2, "Sophia Martinez", Role.STUDENT, UserStatus.ACTIVE, false, DateUtils.nowString(), "");
        User uS3 = new User("STU1003", "david.kim", PasswordHasher.hashPassword("Student@123", saltS3), saltS3, "David Kim", Role.STUDENT, UserStatus.ACTIVE, false, DateUtils.nowString(), "");
        User uS4 = new User("STU1004", "maya.patel", PasswordHasher.hashPassword("Student@123", saltS4), saltS4, "Maya Patel", Role.STUDENT, UserStatus.ACTIVE, false, DateUtils.nowString(), "");
        User uS5 = new User("STU1005", "marcus.j", PasswordHasher.hashPassword("Student@123", saltS5), saltS5, "Marcus Johnson", Role.STUDENT, UserStatus.ACTIVE, false, DateUtils.nowString(), "");

        users.put(uS1.getUserId(), uS1);
        users.put(uS2.getUserId(), uS2);
        users.put(uS3.getUserId(), uS3);
        users.put(uS4.getUserId(), uS4);
        users.put(uS5.getUserId(), uS5);

        // Courses
        Course c1 = new Course("CS301", "Database Systems & Design", "Computer Science", 4, 4, "FAC1001", "ACTIVE");
        Course c2 = new Course("CS302", "Algorithms & Complexity", "Computer Science", 4, 4, "FAC1001", "ACTIVE");
        Course c3 = new Course("IT205", "Web Application Architecture", "Information Technology", 3, 3, "FAC1002", "ACTIVE");
        Course c4 = new Course("AI101", "Neural Networks & Deep Learning", "Artificial Intelligence", 2, 4, "FAC1003", "ACTIVE");

        courses.put(c1.getCourseCode(), c1);
        courses.put(c2.getCourseCode(), c2);
        courses.put(c3.getCourseCode(), c3);
        courses.put(c4.getCourseCode(), c4);

        // Attendance records
        LocalDate today = LocalDate.now();
        for (int i = 5; i >= 1; i--) {
            String d = today.minusDays(i).toString();
            attendances.add(new Attendance("ATT" + (++maxAttendanceSeq), "STU1001", "CS301", d, AttendanceStatus.PRESENT, "Regular lecture"));
            attendances.add(new Attendance("ATT" + (++maxAttendanceSeq), "STU1002", "CS301", d, AttendanceStatus.PRESENT, "Regular lecture"));
            attendances.add(new Attendance("ATT" + (++maxAttendanceSeq), "STU1005", "CS301", d, i == 3 ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT, i == 3 ? "Medical" : "Regular lecture"));

            attendances.add(new Attendance("ATT" + (++maxAttendanceSeq), "STU1001", "CS302", d, AttendanceStatus.PRESENT, ""));
            attendances.add(new Attendance("ATT" + (++maxAttendanceSeq), "STU1002", "CS302", d, i == 2 ? AttendanceStatus.LATE : AttendanceStatus.PRESENT, ""));
            attendances.add(new Attendance("ATT" + (++maxAttendanceSeq), "STU1005", "CS302", d, (i == 1 || i == 4) ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT, ""));
        }

        // Notices
        Notice n1 = new Notice("NOT1001", "Mid-Term Examination Schedule Released", "ACADEMIC",
                today.minusDays(2).toString(),
                "The official schedule for Mid-Semester examinations is now available on the student portal. Students must report 15 minutes prior to hall entry.",
                "Dr. Robert Vance", "ACTIVE");
        Notice n2 = new Notice("NOT1002", "Annual Hackathon 2026 Registrations Open", "EVENT",
                today.minusDays(4).toString(),
                "Register your team of up to 4 members for HackCampus 2026 with $10,000 in grand prizes. Tracks include AI, IoT, and Cloud Computing.",
                "Admin", "ACTIVE");
        Notice n3 = new Notice("NOT1003", "Campus WiFi Infrastructure Maintenance Notice", "URGENT",
                today.minusDays(1).toString(),
                "Scheduled maintenance of campus backbone routers will occur tonight between 11:00 PM and 02:00 AM. Intermittent connectivity expected.",
                "Campus Administration", "ACTIVE");

        notices.put(n1.getNoticeId(), n1);
        notices.put(n2.getNoticeId(), n2);
        notices.put(n3.getNoticeId(), n3);

        // Events
        CampusEvent ev1 = new CampusEvent("EVT1001", "HackCampus 2026 Grand Hackathon",
                "36-hour collegiate hackathon celebrating software engineering and tech innovation.",
                today.plusDays(10) + " 09:00", "Innovation Hub, Block B", "Prof. Elena Rostova", "UPCOMING");
        CampusEvent ev2 = new CampusEvent("EVT1002", "Tech Career & Internship Fair",
                "Meet recruiters from over 40 leading technology organizations and startups.",
                today.plusDays(20) + " 10:00", "Central Auditorium", "Campus Placement Cell", "UPCOMING");
        CampusEvent ev3 = new CampusEvent("EVT1003", "Guest Lecture: Generative AI in Production",
                "Keynote address by Principal AI Architect from Silicon Valley.",
                today.plusDays(6) + " 14:00", "Seminar Hall 3", "Dr. Alan Turing-Smith", "UPCOMING");

        events.put(ev1.getEventId(), ev1);
        events.put(ev2.getEventId(), ev2);
        events.put(ev3.getEventId(), ev3);

        // Complaints
        Complaint cmp1 = new Complaint("CMP1001", "STU1001", "Air Conditioner Malfunction in Lab 304",
                "INFRASTRUCTURE", "The central air conditioning unit in CSE Lab 304 has been blowing warm air since yesterday afternoon.",
                today.minusDays(3).toString(), "IN_PROGRESS", "Facilities Team", "Technician dispatched to inspect refrigerant pressure.");
        Complaint cmp2 = new Complaint("CMP1002", "STU1002", "Library Quiet Room Noise Disturbance",
                "LIBRARY", "Excessive noise near the 2nd-floor individual study cubicles during peak afternoon study hours.",
                today.minusDays(5).toString(), "RESOLVED", "Chief Librarian", "Additional acoustic signage posted and patrol frequency increased.");
        Complaint cmp3 = new Complaint("CMP1003", "STU1005", "Hostel Hot Water Heater Disruption",
                "HOSTEL", "Hostel Block C 3rd floor water heater has intermittent power trip issues every morning.",
                today.minusDays(1).toString(), "PENDING", "Campus Administration", "");

        complaints.put(cmp1.getComplaintId(), cmp1);
        complaints.put(cmp2.getComplaintId(), cmp2);
        complaints.put(cmp3.getComplaintId(), cmp3);

        calculateMaxSequences();
        saveAll();
        logAudit("SYSTEM", "SEED_WEB_DATA", "Pre-seeded mobile web college dataset.");
    }
}
