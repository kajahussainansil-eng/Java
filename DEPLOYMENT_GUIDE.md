# CampusFlow Cloud Deployment & Mobile Guide (No PC Required)

CampusFlow is an all-Java mobile-first campus ERP application built without external PHP, MySQL, or Node.js dependencies. It runs an embedded Java HTTP REST server and serves a modern responsive Progressive Web Application (PWA), and connects directly to an Android native app (`CampusFlow.apk`).

---

## 1. Live Verified Public URL (Currently Running)

The backend is currently hosted live on an encrypted public Cloudflare tunnel:
- **Public URL**: `https://reuters-watts-neon-leading.trycloudflare.com`
- **Protocol**: HTTPS (HTTP/2 with TLS encryption)
- **Status**: Live and verified.
- **Direct Web Access**: Open this URL in any mobile browser (Chrome, Safari, Firefox, Edge, Samsung Internet) or install as a Progressive Web App (PWA) to your home screen.

### Pre-Configured Demo Credentials
| Role | Username | Password | Full Name & Access Scope |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `Admin@123` | Dr. Arthur Pendelton (Full system metrics, student CRUD, faculty CRUD, attendance, audit logs, backup & export) |
| **Faculty** | `robert.vance` | `Faculty@123` | Dr. Robert Vance (CS Dept, smart roll-call marking, attendance alerts, notice creation) |
| **Student** | `alex.chen` | `Student@123` | Alex Chen (Course schedule, personal attendance tracking, noticeboard, complaint filing & tracking) |

*(Credentials can be customized dynamically using environment variables or reset via the Admin Settings tab).*

---

## 2. Permanent Free Cloud Deployment Options (From Phone Browser)

You do **not** need a computer to host CampusFlow permanently. You can deploy it directly from your smartphone browser in under 3 minutes using any of the following free cloud platforms:

### Option A: Render.com (Recommended Free Cloud Hosting)
1. Open [render.com](https://render.com) on your phone browser and create a free account (sign in with GitHub or email).
2. Create a new **Web Service**.
3. Point Render to your GitHub repository containing the `campusflow_web` source code.
4. Render automatically detects `render.yaml` or you can fill in:
   - **Environment**: `Docker` (or `Java` with OpenJDK 21)
   - **Build Command**: `chmod +x compile_web.sh && ./compile_web.sh`
   - **Start Command**: `java -jar CampusFlowWeb.jar $PORT`
5. Click **Create Web Service**. Render builds the lightweight Alpine Linux container and issues a permanent public HTTPS URL like `https://campusflow-app.onrender.com`.
6. Enter `https://campusflow-app.onrender.com` in your mobile app settings!

### Option B: Koyeb (Free Docker Hosting)
1. Open [koyeb.com](https://www.koyeb.com) on your phone.
2. Select **Create App** -> **GitHub** (or public Docker container).
3. The included multi-stage `Dockerfile` will automatically build the OpenJDK Alpine runtime.
4. Koyeb automatically maps the internal port to an external HTTPS URL (e.g., `https://campusflow.koyeb.app`).

### Option C: Railway.app
1. Open [railway.app](https://railway.app) on your phone.
2. Select **Deploy from GitHub Repo**.
3. Railway detects `railway.toml` and starts the standalone Java JAR with automatic TLS.

---

## 3. Data Persistence & Ephemeral Cloud Storage

Free cloud tiers (Render free, Koyeb free, Heroku) run in ephemeral containers that restart after idle periods. To ensure your student records, attendance, and complaints persist:
1. **Persistent Disk / Volume**:
   - In Render: Add a free persistent disk mount path at `/app/web_data`. CampusFlow automatically respects the `DATA_DIR` environment variable (e.g. `DATA_DIR=/app/web_data`).
2. **Built-in In-App Backup & Restore**:
   - Admins can tap **Settings -> Backup Database** at any time. CampusFlow creates a timestamped zip archive of all CSV tables.
   - You can also export the full student list to CSV via **Reports -> Export CSV**.
3. **Desktop Application Sync**:
   - The REST API endpoint `POST /api/sync/desktop` synchronizes records directly with the desktop `campus_data` directory.

---

## 4. Mobile Access (Android Phone Setup)

You have two seamless ways to use CampusFlow on your phone:

### Method 1: Install the Android Native APK (`CampusFlow.apk`)
1. Download `CampusFlow.apk` (5.5 MB) directly to your Android phone.
2. Tap the file in your notification bar or Downloads folder to install (enable "Install unknown apps" if prompted).
3. Open the **CampusFlow** app.
4. The app is pre-configured to connect to the verified live cloud backend.
5. If you deploy your own custom server, tap **Change Server** on the login screen, enter your custom HTTPS URL (e.g. `https://my-campusflow.onrender.com`), and tap **Save & Connect**.

### Method 2: Mobile Web PWA (Zero Installation)
1. Open `https://reuters-watts-neon-leading.trycloudflare.com` in Chrome on your phone.
2. Tap the 3 dots menu in Chrome -> **Add to Home Screen** (or "Install App").
3. CampusFlow installs as a full-screen, standalone app with offline service worker caching and native bottom navigation.

---

## 5. Security & Architecture Review

CampusFlow fulfills all enterprise and academic security requirements:
- **Authentication**: Salted SHA-256 / PBKDF2 password hashing with secure cryptographic random salts.
- **Session Security**: 64-character cryptographically secure bearer tokens (`cfs_...`) with automatic 24-hour expiration and server-side invalidation on logout.
- **Role-Based Access Control (RBAC)**: Strict server-side route filtering. Students cannot access administrative mutations; Faculty can only submit grades and roll-call.
- **Transport Security**: Enforced HTTPS with Cloudflare TLS 1.3 / HTTP/2.
- **Cross-Origin Resource Sharing (CORS)**: Configured headers supporting secure mobile application requests with allowed headers (`Content-Type`, `Authorization`).
- **No External Dependencies**: 100% Java Standard Library HTTP server (`com.sun.net.httpserver.HttpServer`) without Spring, Node.js, PHP, or external SQL servers.
