# CampusFlow — Mobile Web Application
> **SMART CAMPUS MANAGEMENT & STUDENT SERVICES SYSTEM**  
> *"Your Campus. Connected."*

---

## 1. Executive Overview

**CampusFlow Mobile Web** is a mobile-first, Progressive Web Application (PWA) edition of the CampusFlow Smart Campus Management System. It is engineered specifically for modern Android smartphones (such as the Samsung Galaxy A35 5G, 360–430px viewport width) while maintaining 100% architectural fidelity to the college project's core requirement: **a pure Java backend with zero external server dependencies**.

- **No PHP, No MySQL, No Python, No Node.js runtime required for backend hosting.**
- Powered by Java's native `com.sun.net.httpserver.HttpServer` on Java 11+.
- Fully standalone, multi-threaded REST API server with an embedded, zero-dependency JSON serializer and in-memory cryptographic session token manager.
- Persistent, atomic CSV transactional database stored in `campusflow_web/web_data/` with automated snapshots and desktop application data synchronization.
- Mobile PWA frontend featuring the Cyber Navy design language, fluid thumb-driven bottom navigation, rapid-tap attendance recording, and offline application shell caching.

---

## 2. Technology Stack & Design Architecture

### Backend (Pure Java Standard Library)
| Component | Implementation | Description |
| :--- | :--- | :--- |
| **HTTP Engine** | `com.sun.net.httpserver.HttpServer` | Multi-threaded HTTP server listening on `0.0.0.0:8080` |
| **REST Router** | `web.ApiHandler` | Full RESTful controller handling auth, dashboard, students, faculty, courses, attendance, notices, events, complaints, reports, CSV exports, backups, and desktop sync |
| **JSON Engine** | `web.JsonUtils` | Handcrafted pure Java JSON parser and reflective serializer (Zero external JSON JARs) |
| **Security & Auth** | `security.PasswordHasher` & `web.SessionManager` | PBKDF2-HMAC-SHA256 salted password hashing, 12,000 iterations, 32-byte cryptographic session tokens with TTL |
| **Persistence** | `storage.WebDataManager` & `storage.CsvStorage` | Atomic multi-entity CSV persistence (`.tmp` -> rename) with auto-increment ID generation and file locking |

### Mobile Frontend (Progressive Web App)
| Feature | Details |
| :--- | :--- |
| **Color Palette** | Cyber Navy Dark Mode: Deep Void (`#0B1020`), Surface Card (`#162038`), Elevated (`#1D2A47`), Border (`#233050`), Electric Blue (`#4F7CFF`), Cyan (`#22D3EE`), Emerald Green (`#34D399`), Rose (`#F87171`) |
| **Ergonomics** | 48px+ touch targets, bottom fixed navigation bar, horizontal scrolling filter chips, floating action buttons |
| **PWA Features** | `manifest.json` (installable to Android home screen, standalone display), `sw.js` (Service Worker offline app shell caching), responsive meta viewport tags |
| **Interactive Widgets** | 4-chip rapid roll-call buttons (`[P]`, `[A]`, `[L]`, `[E]`), dynamic SVG attendance gauge ring with animated stroke-dashoffset |

---

## 3. Supported User Roles & Default Demo Credentials

The backend strictly enforces role-based access control (RBAC) at the API handler layer.

| Role | Username | Password | Full Name & Title | Mobile UI Features |
| :--- | :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `Admin@123` | Dr. Arthur Pendelton (Dean) | Full system metrics, student directory CRUD, faculty directory, attendance marker, low-attendance defaulter reports, CSV downloads, backups, desktop synchronization |
| **Faculty** | `robert.vance` | `Faculty@123` | Dr. Robert Vance (CS Professor) | Department courses view, class attendance summaries, smart roll-call marker, official notice broadcaster |
| **Student** | `alex.chen` | `Student@123` | Alex Chen (CS Sophomore) | Circular attendance status gauge (eligibility tracking), current GPA card, course schedule, campus event feed, complaints desk with status tracking |

---

## 4. Directory Structure

```
campusflow_web/
├── CampusFlowWeb.jar          # Standalone executable JAR (144 KB, zero dependencies)
├── run_web.sh                 # Linux/macOS server startup script
├── run_web.bat                # Windows server startup script
├── compile_web.sh             # Full build & test automation script
├── README.md                  # This architectural and operational guide
├── src/                       # Java backend source code
│   ├── model/                 # Domain entities (User, Student, Faculty, Course, Attendance, Notice, Event, Complaint)
│   ├── security/              # PBKDF2 password hasher & RBAC permissions
│   ├── service/               # Business logic services (Auth, Student, Attendance, Reports, etc.)
│   ├── storage/               # Atomic CSV persistence engine & initial seed data generator
│   ├── util/                  # Validation and date utilities
│   └── web/                   # Embedded HTTP server, REST controller, JSON serializer, session manager
├── static/                    # Mobile PWA frontend assets
│   ├── index.html             # Mobile web application shell & templates
│   ├── styles.css             # Cyber Navy responsive mobile-first styling
│   ├── app.js                 # Single-page application touch controller & API client
│   ├── manifest.json          # PWA web manifest
│   ├── sw.js                  # Service Worker offline cache engine
│   └── icons/                 # PWA application icons (192x192, 512x512)
├── web_data/                  # Persistent CSV storage directory
│   ├── users.csv              # Salted user credentials
│   ├── students.csv           # Student records
│   ├── faculty.csv            # Faculty directory
│   ├── courses.csv            # Curriculum and schedules
│   ├── attendance.csv         # Session attendance logs
│   ├── notices.csv            # Broadcast notices
│   ├── events.csv             # Campus hackathons and events
│   ├── complaints.csv         # Student support tickets
│   └── audit_logs.csv         # Security and transaction audit logs
├── test/                      # Integration test suite
│   └── WebIntegrationTest.java# 63 automated assertions testing all layers
└── screenshots/               # Verified mobile viewport screenshots (Galaxy A35/A55)
    ├── 01_mobile_login.png
    ├── 02_mobile_admin_dashboard.png
    ├── 03_mobile_student_directory.png
    ├── 04_mobile_attendance_marker.png
    ├── 05_mobile_reports_system.png
    ├── 06_mobile_student_dashboard.png
    ├── 07_mobile_complaints_desk.png
    └── 08_mobile_campus_notices.png
```

---

## 5. How to Run the Mobile Web Application

### Option A: Running the Standalone Executable JAR (Recommended)
Make sure Java 11 or higher is installed:
```bash
java -jar CampusFlowWeb.jar 8080
```
On Windows: double-click `run_web.bat` or run:
```cmd
java -jar CampusFlowWeb.jar 8080
```

### Option B: Compiling from Source & Running Tests
To rebuild the application and execute all 63 unit/integration tests:
```bash
./compile_web.sh
./run_web.sh 8080
```

---

## 6. Accessing on Android Phone (Samsung Galaxy A35 5G)

1. Connect your computer and your Samsung Galaxy phone to the **same Wi-Fi network**.
2. Run the application server (`./run_web.sh 8080` or `java -jar CampusFlowWeb.jar 8080`).
3. Note the Wi-Fi IP address printed on the server console (e.g., `http://192.168.1.15:8080/`).
4. On your phone's Chrome or Samsung Internet browser, navigate to:
   ```
   http://<YOUR_COMPUTER_IP>:8080/
   ```
5. **Install as PWA**:
   - In Google Chrome on Android, tap the three dots menu (**⋮**) in the top right.
   - Tap **"Install app"** or **"Add to Home screen"**.
   - CampusFlow will install to your Android home screen with the official icon and launch in full-screen standalone mode without URL address bar distraction!

---

## 7. Data Synchronization with Desktop Application

The mobile web application stores its primary records in `campusflow_web/web_data/` and includes bidirectional synchronization with the Swing desktop application:
- **Web Export to Desktop**: Copies live web CSV records directly into `campus_data/` so the desktop application immediately reflects changes made on mobile.
- **Desktop Import to Web**: Ingests updated desktop CSV files from `campus_data/` into `campusflow_web/web_data/` and reloads in-memory caches dynamically.
- Accessible directly from the **Services / Reports** menu under **"Sync Data With Desktop App"** or via the REST API endpoint `POST /api/sync/desktop`.

---

## 8. Verification & Test Suite Summary

The included test suite (`test/WebIntegrationTest.java`) tests the entire application stack without mock frameworks:
- **JsonUtils**: Parsing and bidirectional serialization of nested maps, arrays, primitives, and POJO reflection.
- **PasswordHasher**: PBKDF2-HMAC-SHA256 hash generation, salt uniqueness, and constant-time verification.
- **SessionManager**: Token generation, TTL validation, role permissions, and invalidation.
- **WebAuthService**: Multi-role login validation and password rejection.
- **StudentService**: Student CRUD operations and data validation.
- **AttendanceService**: Batch session recording, percentage calculations, and low-attendance defaulter alerts (< 75%).
- **Complaints Desk**: Student lodging, category validation, status progression (`SUBMITTED` -> `IN_REVIEW` -> `RESOLVED`), and resolution notes.
- **HTTP Server**: Live in-process testing of `/index.html`, `/styles.css`, `/app.js`, `/manifest.json`, `/api/auth/login`, `/api/dashboard`, `/api/students`, `/api/reports/low-attendance`, `/api/export/csv`, `/api/backup`, and `/api/sync/desktop`.

**Result:** 63/63 test assertions passed cleanly in ~1 second.
