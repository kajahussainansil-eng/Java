/**
 * CampusFlow Mobile Web Application
 * Single Page Application Controller & Touch UI Router
 */

// Global State
const state = {
  token: localStorage.getItem('cf_token') || null,
  user: JSON.parse(localStorage.getItem('cf_user') || 'null'),
  currentNav: 'dashboard',
  students: [],
  faculty: [],
  courses: [],
  notices: [],
  events: [],
  complaints: [],
  attendanceRoster: [],
  studentFilter: { dept: 'ALL', query: '' },
  noticeFilter: { cat: 'ALL' },
  complaintFilter: { status: 'ALL' }
};

// -----------------------------------------------------------------------------
// PWA & Connectivity
// -----------------------------------------------------------------------------
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(err => {
      console.log('SW registration error:', err);
    });
  });
}

window.addEventListener('online', () => {
  document.body.classList.remove('is-offline');
  showToast('Connection restored. Online.', 'success');
});

window.addEventListener('offline', () => {
  document.body.classList.add('is-offline');
  showToast('Network disconnected. Offline mode.', 'warning');
});

// -----------------------------------------------------------------------------
// Initialization
// -----------------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
  // Set default date for attendance marker to today
  const today = new Date().toISOString().split('T')[0];
  const attDateInput = document.getElementById('attDateInput');
  if (attDateInput) attDateInput.value = today;

  // Setup login form submission
  const loginForm = document.getElementById('loginForm');
  if (loginForm) {
    loginForm.addEventListener('submit', handleLoginForm);
  }

  // Setup logout button
  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', handleLogout);
  }

  // Check existing session
  if (state.token && state.user) {
    verifySession();
  } else {
    showLoginView();
  }
});

// -----------------------------------------------------------------------------
// API Helper
// -----------------------------------------------------------------------------
async function apiCall(endpoint, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (state.token) {
    headers['Authorization'] = 'Bearer ' + state.token;
  }

  try {
    const res = await fetch('/api' + endpoint, {
      ...options,
      headers
    });

    const data = await res.json().catch(() => ({}));
    if (res.status === 401) {
      handleLogout();
      throw new Error(data.error || 'Session expired. Please log in again.');
    }
    if (!res.ok) {
      throw new Error(data.error || 'Server error ' + res.status);
    }
    return data;
  } catch (err) {
    console.error('API Error:', err);
    throw err;
  }
}

// -----------------------------------------------------------------------------
// Authentication
// -----------------------------------------------------------------------------
async function quickLogin(username, password) {
  document.getElementById('usernameInput').value = username;
  document.getElementById('passwordInput').value = password;
  await doLogin(username, password);
}

async function handleLoginForm(e) {
  e.preventDefault();
  const u = document.getElementById('usernameInput').value.trim();
  const p = document.getElementById('passwordInput').value;
  await doLogin(u, p);
}

async function doLogin(username, password) {
  try {
    const res = await apiCall('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });

    if (res.success && res.token) {
      state.token = res.token;
      state.user = res.user;
      localStorage.setItem('cf_token', res.token);
      localStorage.setItem('cf_user', JSON.stringify(res.user));

      showToast(`Welcome back, ${res.user.fullName || res.user.username}!`, 'success');
      showAppShell();
      switchNav('dashboard', document.querySelector('.nav-tab'));
    } else {
      showToast(res.error || 'Authentication failed', 'error');
    }
  } catch (err) {
    showToast(err.message || 'Login failed', 'error');
  }
}

async function verifySession() {
  try {
    const res = await apiCall('/auth/me');
    if (res.authenticated) {
      state.user = res.user;
      showAppShell();
      switchNav('dashboard', document.querySelector('.nav-tab'));
    } else {
      showLoginView();
    }
  } catch {
    showLoginView();
  }
}

function handleLogout() {
  if (state.token) {
    apiCall('/auth/logout', { method: 'POST' }).catch(() => {});
  }
  state.token = null;
  state.user = null;
  localStorage.removeItem('cf_token');
  localStorage.removeItem('cf_user');
  showLoginView();
  showToast('Logged out successfully', 'info');
}

function showLoginView() {
  document.getElementById('appHeader').style.display = 'none';
  document.getElementById('bottomNav').style.display = 'none';
  hideAllViews();
  document.getElementById('viewLogin').classList.add('active-view');
}

function showAppShell() {
  document.getElementById('viewLogin').classList.remove('active-view');
  document.getElementById('appHeader').style.display = 'flex';
  document.getElementById('bottomNav').style.display = 'flex';

  const role = state.user.role || 'STUDENT';
  const roleBadge = document.getElementById('userRoleBadge');
  roleBadge.textContent = role;
  roleBadge.className = 'role-pill ' + role.toLowerCase();

  const navStudentsSpan = document.querySelector('#navStudentsTab span');
  if (navStudentsSpan) {
    navStudentsSpan.textContent = role === 'STUDENT' ? 'Courses' : 'Students';
  }

  // Role permissions on UI controls
  const addStudentBtn = document.getElementById('addStudentBtn');
  const addFacultyBtn = document.getElementById('addFacultyBtn');
  const addCourseBtn = document.getElementById('addCourseBtn');
  const addNoticeBtn = document.getElementById('addNoticeBtn');

  if (addStudentBtn) addStudentBtn.style.display = role === 'ADMIN' ? 'inline-flex' : 'none';
  if (addFacultyBtn) addFacultyBtn.style.display = role === 'ADMIN' ? 'inline-flex' : 'none';
  if (addCourseBtn) addCourseBtn.style.display = role === 'ADMIN' ? 'inline-flex' : 'none';
  if (addNoticeBtn) addNoticeBtn.style.display = (role === 'ADMIN' || role === 'FACULTY') ? 'inline-flex' : 'none';
}

function hideAllViews() {
  document.querySelectorAll('.view-section').forEach(sec => sec.classList.remove('active-view'));
}

// -----------------------------------------------------------------------------
// Navigation Router
// -----------------------------------------------------------------------------
function switchNav(navKey, navTabEl) {
  state.currentNav = navKey;
  hideAllViews();

  document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
  if (navTabEl) navTabEl.classList.add('active');

  switch (navKey) {
    case 'dashboard':
      document.getElementById('viewDashboard').classList.add('active-view');
      loadDashboard();
      break;
    case 'students':
      if (state.user.role === 'STUDENT') {
        // For student, show courses catalog
        document.getElementById('viewCourses').classList.add('active-view');
        loadCourses();
      } else {
        document.getElementById('viewStudents').classList.add('active-view');
        loadStudents();
      }
      break;
    case 'attendance':
      document.getElementById('viewAttendance').classList.add('active-view');
      initAttendanceView();
      break;
    case 'notices':
      document.getElementById('viewNotices').classList.add('active-view');
      loadNotices();
      break;
    case 'events':
      document.getElementById('viewEvents').classList.add('active-view');
      loadEvents();
      break;
    case 'services':
      if (state.user.role === 'ADMIN') {
        document.getElementById('viewReports').classList.add('active-view');
        loadLowAttendanceReport();
      } else {
        document.getElementById('viewComplaints').classList.add('active-view');
        loadComplaints();
      }
      break;
    default:
      document.getElementById('viewDashboard').classList.add('active-view');
  }
}

// -----------------------------------------------------------------------------
// Dashboard View
// -----------------------------------------------------------------------------
async function loadDashboard() {
  try {
    const data = await apiCall('/dashboard');
    const role = data.role || state.user.role;

    document.getElementById('dashWelcomeTitle').textContent = `Hello, ${state.user.fullName || state.user.username}`;
    document.getElementById('dashWelcomeSubtitle').textContent =
      `Role: ${role} \u2022 Academic Year 2026-2027 \u2022 Spring Semester`;

    const statsGrid = document.getElementById('dashStatsGrid');
    const alertBox = document.getElementById('lowAttendanceAlertBox');
    const ringCard = document.getElementById('studentAttendanceRingCard');
    const feedCards = document.getElementById('dashFeedCards');

    // 1. Render Stats Grid
    let statsHtml = '';
    if (role === 'ADMIN') {
      statsHtml = `
        <div class="stat-card" style="--card-accent: var(--primary);">
          <div class="stat-header"><span class="stat-title">Students</span></div>
          <div class="stat-value">${data.totalStudents || 0}</div>
          <div class="stat-subtitle">Enrolled Active</div>
        </div>
        <div class="stat-card" style="--card-accent: var(--purple);">
          <div class="stat-header"><span class="stat-title">Faculty</span></div>
          <div class="stat-value">${data.totalFaculty || 0}</div>
          <div class="stat-subtitle">Full-time Staff</div>
        </div>
        <div class="stat-card" style="--card-accent: var(--cyan);">
          <div class="stat-header"><span class="stat-title">Avg Attendance</span></div>
          <div class="stat-value">${data.avgAttendanceRate ? data.avgAttendanceRate.toFixed(1) : '86.4'}%</div>
          <div class="stat-subtitle">Campus Wide</div>
        </div>
        <div class="stat-card" style="--card-accent: var(--amber);">
          <div class="stat-header"><span class="stat-title">Open Tickets</span></div>
          <div class="stat-value">${data.openComplaints || 0}</div>
          <div class="stat-subtitle">Support Queue</div>
        </div>
      `;
    } else if (role === 'FACULTY') {
      statsHtml = `
        <div class="stat-card" style="--card-accent: var(--primary);">
          <div class="stat-header"><span class="stat-title">My Courses</span></div>
          <div class="stat-value">${data.coursesTeaching ? data.coursesTeaching.length : 2}</div>
          <div class="stat-subtitle">Spring Semester</div>
        </div>
        <div class="stat-card" style="--card-accent: var(--green);">
          <div class="stat-header"><span class="stat-title">Dept Attendance</span></div>
          <div class="stat-value">${data.avgAttendanceRate ? data.avgAttendanceRate.toFixed(1) : '88.5'}%</div>
          <div class="stat-subtitle">Classroom Avg</div>
        </div>
      `;
    } else { // STUDENT
      statsHtml = `
        <div class="stat-card" style="--card-accent: var(--primary);">
          <div class="stat-header"><span class="stat-title">Current GPA</span></div>
          <div class="stat-value">${data.student && data.student.gpa ? data.student.gpa.toFixed(2) : '3.85'}</div>
          <div class="stat-subtitle">Semester ${data.student ? data.student.semester : '4'}</div>
        </div>
        <div class="stat-card" style="--card-accent: var(--green);">
          <div class="stat-header"><span class="stat-title">Attendance</span></div>
          <div class="stat-value">${data.attendancePercentage ? data.attendancePercentage.toFixed(1) : '88.2'}%</div>
          <div class="stat-subtitle">Eligible for Exams</div>
        </div>
      `;
    }
    statsGrid.innerHTML = statsHtml;

    // 2. Low Attendance Banner
    if (data.lowAttendanceCount && data.lowAttendanceCount > 0) {
      alertBox.style.display = 'block';
      document.getElementById('lowAttendanceAlertText').textContent =
        `${data.lowAttendanceCount} student(s) currently have attendance below the 75% threshold. Review reports for details.`;
    } else {
      alertBox.style.display = 'none';
    }

    // 3. Student Circular Gauge
    if (role === 'STUDENT' && data.attendancePercentage !== undefined) {
      ringCard.style.display = 'block';
      const pct = Math.round(data.attendancePercentage);
      document.getElementById('dashRingPercent').textContent = pct + '%';
      const fillEl = document.getElementById('dashRingFill');
      const offset = 251.2 - (251.2 * (pct / 100));
      fillEl.style.strokeDashoffset = offset;
      if (pct < 75) {
        fillEl.style.stroke = 'var(--rose)';
        document.getElementById('dashRingStatus').textContent = 'Defaulter Risk (< 75%)';
        document.getElementById('dashRingStatus').style.color = 'var(--rose)';
      } else {
        fillEl.style.stroke = 'var(--green)';
        document.getElementById('dashRingStatus').textContent = 'Good Standing (Eligible)';
        document.getElementById('dashRingStatus').style.color = 'var(--green)';
      }
    } else {
      ringCard.style.display = 'none';
    }

    // 4. Activity Feed Cards (Recent Notices & Events)
    let feedHtml = '<h3 style="font-size: 15px; font-weight: 700; margin: 18px 0 10px;">Official Campus Feed</h3>';
    if (data.recentNotices && data.recentNotices.length > 0) {
      data.recentNotices.forEach(n => {
        const cat = n.category || 'ACADEMIC';
        const date = n.publicationDate || n.postedDate || '';
        const title = n.title || 'Campus Notice';
        const desc = n.description || n.content || '';
        feedHtml += `
          <div class="card">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px;">
              <span class="badge ${cat === 'ALERT' || cat === 'URGENT' ? 'badge-red' : 'badge-cyan'}">${cat}</span>
              <span style="font-size: 11px; color: var(--text-muted);">${date}</span>
            </div>
            <h4 style="font-size: 14px; font-weight: 700; margin-bottom: 4px; color: #fff;">${escapeHtml(title)}</h4>
            <p style="font-size: 12px; color: var(--text-secondary);">${escapeHtml(desc)}</p>
          </div>
        `;
      });
    }

    if (data.upcomingEvents && data.upcomingEvents.length > 0) {
      data.upcomingEvents.slice(0, 2).forEach(ev => {
        const evName = ev.eventName || ev.title || 'Campus Event';
        const evDate = ev.dateTime || ev.eventDate || '';
        const venue = ev.venue || 'Main Auditorium';
        feedHtml += `
          <div class="card" style="border-left: 3px solid var(--purple);">
            <div style="display: flex; justify-content: space-between; align-items: flex-start;">
              <div>
                <span class="badge badge-purple" style="background: rgba(139, 92, 246, 0.2); color: var(--purple);">EVENT</span>
                <h4 style="font-size: 14px; font-weight: 700; margin: 4px 0; color: #fff;">${escapeHtml(evName)}</h4>
                <p style="font-size: 12px; color: var(--text-muted);">&#128197; ${evDate} &bull; &#128205; ${escapeHtml(venue)}</p>
              </div>
            </div>
          </div>
        `;
      });
    }
    feedCards.innerHTML = feedHtml;

  } catch (err) {
    showToast('Failed to load dashboard: ' + err.message, 'error');
  }
}

// -----------------------------------------------------------------------------
// Students View & CRUD
// -----------------------------------------------------------------------------
async function loadStudents() {
  try {
    const list = await apiCall('/students');
    state.students = list || [];
    renderStudentCards();
  } catch (err) {
    showToast('Failed to load students: ' + err.message, 'error');
  }
}

function setStudentDeptFilter(dept, btn) {
  state.studentFilter.dept = dept;
  document.querySelectorAll('#studentDeptChips .chip').forEach(c => c.classList.remove('active'));
  btn.classList.add('active');
  renderStudentCards();
}

function filterStudents() {
  state.studentFilter.query = document.getElementById('studentSearchInput').value.toLowerCase().trim();
  renderStudentCards();
}

function renderStudentCards() {
  const container = document.getElementById('studentsList');
  if (!container) return;

  const { dept, query } = state.studentFilter;
  const filtered = state.students.filter(s => {
    const fullName = s.fullName || s.name || '';
    const studentId = s.studentId || s.id || '';
    const sDept = s.department || '';
    const email = s.email || '';

    const matchDept = (dept === 'ALL') || (sDept.toLowerCase().includes(dept.toLowerCase()));
    const matchQuery = !query ||
      (fullName.toLowerCase().includes(query)) ||
      (studentId.toLowerCase().includes(query)) ||
      (email.toLowerCase().includes(query));
    return matchDept && matchQuery;
  });

  if (filtered.length === 0) {
    container.innerHTML = '<div class="card" style="text-align: center; color: var(--text-muted);">No student records found.</div>';
    return;
  }

  const isAdmin = state.user && state.user.role === 'ADMIN';

  container.innerHTML = filtered.map(s => {
    const id = s.studentId || s.id || '';
    const name = s.fullName || s.name || 'Student';
    const sDept = s.department || 'General';
    const sem = s.semester || 1;
    const email = s.email || '';
    const phone = s.phone || '';
    const gpa = s.gpa ? parseFloat(s.gpa).toFixed(2) : (3.85 - ((sem % 4) * 0.12)).toFixed(2);
    const gpaBadgeColor = parseFloat(gpa) >= 3.5 ? 'badge-green' : (parseFloat(gpa) >= 3.0 ? 'badge-cyan' : 'badge-amber');

    return `
      <div class="record-card">
        <div class="record-card-header">
          <div>
            <div class="record-title">${escapeHtml(name)}</div>
            <div class="record-id">${escapeHtml(id)} &bull; Sem ${sem}</div>
          </div>
          <span class="badge ${gpaBadgeColor}">GPA ${gpa}</span>
        </div>
        <div class="record-meta-row">
          <span>&#127891; ${escapeHtml(sDept)}</span>
          <span>&#9993;&#65039; ${escapeHtml(email)}</span>
          <span>&#128222; ${escapeHtml(phone)}</span>
        </div>
        ${isAdmin ? `
          <div class="record-actions">
            <button class="btn btn-secondary btn-sm" onclick="editStudent('${id}')">Edit</button>
            <button class="btn btn-danger btn-sm" onclick="deleteStudent('${id}', '${escapeHtml(name)}')">Delete</button>
          </div>
        ` : ''}
      </div>
    `;
  }).join('');
}

function openAddStudentModal() {
  document.getElementById('studentFormId').value = '';
  document.getElementById('studentModalTitle').textContent = 'Add Student';
  document.getElementById('stdRollNo').value = '';
  document.getElementById('stdName').value = '';
  document.getElementById('stdEmail').value = '';
  document.getElementById('stdPhone').value = '';
  document.getElementById('stdDept').value = 'Computer Science';
  document.getElementById('stdSem').value = '4';
  document.getElementById('stdGpa').value = '3.50';
  openModal('studentModal');
}

function editStudent(id) {
  const s = state.students.find(x => x.id === id);
  if (!s) return;
  document.getElementById('studentFormId').value = s.id;
  document.getElementById('studentModalTitle').textContent = 'Edit Student';
  document.getElementById('stdRollNo').value = s.rollNumber;
  document.getElementById('stdName').value = s.fullName;
  document.getElementById('stdEmail').value = s.email;
  document.getElementById('stdPhone').value = s.phone;
  document.getElementById('stdDept').value = s.department;
  document.getElementById('stdSem').value = s.semester;
  document.getElementById('stdGpa').value = s.gpa;
  openModal('studentModal');
}

async function submitStudentForm(e) {
  e.preventDefault();
  const id = document.getElementById('studentFormId').value;
  const payload = {
    rollNumber: document.getElementById('stdRollNo').value.trim(),
    fullName: document.getElementById('stdName').value.trim(),
    email: document.getElementById('stdEmail').value.trim(),
    phone: document.getElementById('stdPhone').value.trim(),
    department: document.getElementById('stdDept').value,
    semester: parseInt(document.getElementById('stdSem').value, 10),
    gpa: parseFloat(document.getElementById('stdGpa').value)
  };

  try {
    if (id) {
      await apiCall('/students/' + id, { method: 'PUT', body: JSON.stringify(payload) });
      showToast('Student updated successfully', 'success');
    } else {
      await apiCall('/students', { method: 'POST', body: JSON.stringify(payload) });
      showToast('Student added successfully', 'success');
    }
    closeModal('studentModal');
    loadStudents();
  } catch (err) {
    showToast('Failed to save student: ' + err.message, 'error');
  }
}

async function deleteStudent(id, name) {
  if (!confirm(`Are you sure you want to delete student "${name}"?`)) return;
  try {
    await apiCall('/students/' + id, { method: 'DELETE' });
    showToast(`Student ${name} deleted`, 'info');
    loadStudents();
  } catch (err) {
    showToast('Failed to delete student: ' + err.message, 'error');
  }
}

// -----------------------------------------------------------------------------
// Courses View
// -----------------------------------------------------------------------------
async function loadCourses() {
  try {
    const list = await apiCall('/courses');
    state.courses = list || [];
    renderCourseCards();
  } catch (err) {
    showToast('Failed to load courses: ' + err.message, 'error');
  }
}

function filterCourses() {
  renderCourseCards();
}

function renderCourseCards() {
  const container = document.getElementById('coursesList');
  if (!container) return;

  const q = (document.getElementById('courseSearchInput') ? document.getElementById('courseSearchInput').value : '').toLowerCase().trim();
  const filtered = state.courses.filter(c => {
    return !q ||
      (c.courseCode && c.courseCode.toLowerCase().includes(q)) ||
      (c.courseName && c.courseName.toLowerCase().includes(q)) ||
      (c.department && c.department.toLowerCase().includes(q));
  });

  if (filtered.length === 0) {
    container.innerHTML = '<div class="card" style="text-align: center; color: var(--text-muted);">No courses found.</div>';
    return;
  }

  container.innerHTML = filtered.map(c => `
    <div class="record-card">
      <div class="record-card-header">
        <div>
          <div class="record-title">${escapeHtml(c.courseName)}</div>
          <div class="record-id">${escapeHtml(c.courseCode)} &bull; ${c.credits} Credits</div>
        </div>
        <span class="badge badge-blue">${escapeHtml(c.department)}</span>
      </div>
      <div class="record-meta-row">
        <span>&#128104;&#8205;&#127979; Instructor: ${escapeHtml(c.facultyName || 'Department Faculty')}</span>
        <span>&#128101; Capacity: ${c.enrolledCount || 0} / ${c.maxCapacity} Enrolled</span>
      </div>
    </div>
  `).join('');
}

// -----------------------------------------------------------------------------
// Smart Attendance Marker
// -----------------------------------------------------------------------------
async function initAttendanceView() {
  try {
    const courses = await apiCall('/courses');
    state.courses = courses || [];

    const select = document.getElementById('attCourseSelect');
    if (select) {
      select.innerHTML = courses.map(c => `<option value="${c.courseCode || c.id}">${escapeHtml(c.courseCode)} - ${escapeHtml(c.courseName)}</option>`).join('');
    }
    await loadAttendanceRoster();
  } catch (err) {
    showToast('Failed to initialize attendance: ' + err.message, 'error');
  }
}

async function loadAttendanceRoster() {
  const courseId = document.getElementById('attCourseSelect').value;
  const date = document.getElementById('attDateInput').value;
  if (!courseId || !date) return;

  try {
    const data = await apiCall(`/attendance/roster?courseId=${courseId}&date=${date}`);
    state.attendanceRoster = data.students || [];

    const container = document.getElementById('attendanceRosterList');
    if (!state.attendanceRoster || state.attendanceRoster.length === 0) {
      container.innerHTML = '<div class="card" style="text-align: center; color: var(--text-muted);">No enrolled students found for this course.</div>';
      return;
    }

    container.innerHTML = state.attendanceRoster.map(s => {
      const status = s.currentStatus || 'PRESENT';
      return `
        <div class="record-card" data-student-id="${s.studentId}">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div>
              <div style="font-weight: 700; font-size: 14px;">${escapeHtml(s.studentName)}</div>
              <div style="font-size: 11px; color: var(--cyan);">${escapeHtml(s.rollNumber)}</div>
            </div>
            <span id="badge-${s.studentId}" class="badge ${getStatusBadgeClass(status)}">${status}</span>
          </div>
          <div class="att-chip-group">
            <button type="button" class="att-btn ${status === 'PRESENT' ? 'selected-P' : ''}" onclick="selectAttendance('${s.studentId}', 'PRESENT')">P (Present)</button>
            <button type="button" class="att-btn ${status === 'ABSENT' ? 'selected-A' : ''}" onclick="selectAttendance('${s.studentId}', 'ABSENT')">A (Absent)</button>
            <button type="button" class="att-btn ${status === 'LATE' ? 'selected-L' : ''}" onclick="selectAttendance('${s.studentId}', 'LATE')">L (Late)</button>
            <button type="button" class="att-btn ${status === 'EXCUSED' ? 'selected-E' : ''}" onclick="selectAttendance('${s.studentId}', 'EXCUSED')">E (Excused)</button>
          </div>
        </div>
      `;
    }).join('');

  } catch (err) {
    showToast('Failed to load roster: ' + err.message, 'error');
  }
}

function selectAttendance(studentId, status) {
  const student = state.attendanceRoster.find(s => s.studentId === studentId);
  if (student) {
    student.currentStatus = status;
  }

  // Update UI chips
  const card = document.querySelector(`[data-student-id="${studentId}"]`);
  if (card) {
    card.querySelectorAll('.att-btn').forEach(btn => {
      btn.className = 'att-btn';
    });
    const prefix = status[0];
    const targetBtn = Array.from(card.querySelectorAll('.att-btn')).find(b => b.textContent.startsWith(prefix));
    if (targetBtn) targetBtn.classList.add('selected-' + prefix);

    const badge = document.getElementById('badge-' + studentId);
    if (badge) {
      badge.textContent = status;
      badge.className = 'badge ' + getStatusBadgeClass(status);
    }
  }
}

function markAllAttendance(status) {
  state.attendanceRoster.forEach(s => {
    selectAttendance(s.studentId, status);
  });
  showToast(`Marked all students as ${status}`, 'info');
}

async function submitAttendanceBatch() {
  const courseId = document.getElementById('attCourseSelect').value;
  const date = document.getElementById('attDateInput').value;

  if (!courseId || !date) {
    showToast('Course and date are required', 'warning');
    return;
  }

  const records = state.attendanceRoster.map(s => ({
    studentId: s.studentId,
    status: s.currentStatus || 'PRESENT'
  }));

  try {
    const res = await apiCall('/attendance/batch', {
      method: 'POST',
      body: JSON.stringify({
        courseId,
        date,
        records
      })
    });

    showToast(`Attendance saved successfully (${res.recordedCount || records.length} records)`, 'success');
  } catch (err) {
    showToast('Failed to save attendance: ' + err.message, 'error');
  }
}

function getStatusBadgeClass(status) {
  switch (status) {
    case 'PRESENT': return 'badge-green';
    case 'ABSENT': return 'badge-red';
    case 'LATE': return 'badge-amber';
    case 'EXCUSED': return 'badge-cyan';
    default: return 'badge-slate';
  }
}

// -----------------------------------------------------------------------------
// Notices View
// -----------------------------------------------------------------------------
async function loadNotices() {
  try {
    const list = await apiCall('/notices');
    state.notices = list || [];
    renderNoticeCards();
  } catch (err) {
    showToast('Failed to load notices: ' + err.message, 'error');
  }
}

function setNoticeFilter(cat, btn) {
  state.noticeFilter.cat = cat;
  document.querySelectorAll('#noticeCategoryChips .chip').forEach(c => c.classList.remove('active'));
  btn.classList.add('active');
  renderNoticeCards();
}

function renderNoticeCards() {
  const container = document.getElementById('noticesList');
  if (!container) return;

  const cat = state.noticeFilter.cat;
  const filtered = state.notices.filter(n => (cat === 'ALL') || (n.category === cat));

  if (filtered.length === 0) {
    container.innerHTML = '<div class="card" style="text-align: center; color: var(--text-muted);">No announcements in this category.</div>';
    return;
  }

  container.innerHTML = filtered.map(n => {
    const cat = n.category || 'GENERAL';
    const date = n.publicationDate || n.postedDate || '';
    const title = n.title || 'Campus Notice';
    const desc = n.description || n.content || '';
    const author = n.publishedBy || n.postedByName || 'Dean of Academics';

    return `
      <div class="card" style="border-left: 3px solid ${cat === 'ALERT' || cat === 'URGENT' ? 'var(--rose)' : 'var(--cyan)'};">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px;">
          <span class="badge ${cat === 'ALERT' || cat === 'URGENT' ? 'badge-red' : 'badge-cyan'}">${cat}</span>
          <span style="font-size: 11px; color: var(--text-muted);">${date}</span>
        </div>
        <h3 style="font-size: 15px; font-weight: 700; margin-bottom: 6px; color: #fff;">${escapeHtml(title)}</h3>
        <p style="font-size: 13px; color: var(--text-secondary); line-height: 1.5;">${escapeHtml(desc)}</p>
        <div style="margin-top: 10px; font-size: 11px; color: var(--text-muted);">Posted by: ${escapeHtml(author)}</div>
      </div>
    `;
  }).join('');
}

function openAddNoticeModal() {
  document.getElementById('notTitle').value = '';
  document.getElementById('notContent').value = '';
  openModal('noticeModal');
}

async function submitNoticeForm(e) {
  e.preventDefault();
  const payload = {
    category: document.getElementById('notCategory').value,
    title: document.getElementById('notTitle').value.trim(),
    content: document.getElementById('notContent').value.trim()
  };

  try {
    await apiCall('/notices', { method: 'POST', body: JSON.stringify(payload) });
    closeModal('noticeModal');
    showToast('Notice broadcast successfully', 'success');
    loadNotices();
  } catch (err) {
    showToast('Failed to post notice: ' + err.message, 'error');
  }
}

// -----------------------------------------------------------------------------
// Complaints Desk View
// -----------------------------------------------------------------------------
async function loadComplaints() {
  try {
    const list = await apiCall('/complaints');
    state.complaints = list || [];
    renderComplaintCards();
  } catch (err) {
    showToast('Failed to load tickets: ' + err.message, 'error');
  }
}

function setComplaintFilter(status, btn) {
  state.complaintFilter.status = status;
  document.querySelectorAll('#complaintStatusChips .chip').forEach(c => c.classList.remove('active'));
  btn.classList.add('active');
  renderComplaintCards();
}

function renderComplaintCards() {
  const container = document.getElementById('complaintsList');
  if (!container) return;

  const st = state.complaintFilter.status;
  const filtered = state.complaints.filter(c => (st === 'ALL') || (c.status === st));

  if (filtered.length === 0) {
    container.innerHTML = '<div class="card" style="text-align: center; color: var(--text-muted);">No complaint tickets match this filter.</div>';
    return;
  }

  const isStaff = state.user && (state.user.role === 'ADMIN' || state.user.role === 'FACULTY');

  container.innerHTML = filtered.map(c => {
    const id = c.complaintId || c.id || '';
    const subject = c.subject || c.title || 'Support Ticket';
    const cat = c.category || 'GENERAL';
    const date = c.submissionDate || c.filedDate || '';
    const desc = c.description || '';
    const student = c.studentId || c.studentName || 'Student';
    const status = c.status || 'PENDING';
    const notes = c.resolutionNotes || '';

    return `
      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px;">
          <span class="badge ${getComplaintBadgeClass(status)}">${status}</span>
          <span style="font-size: 11px; color: var(--text-muted);">${date}</span>
        </div>
        <h3 style="font-size: 14px; font-weight: 700; color: #fff; margin-bottom: 4px;">${escapeHtml(subject)}</h3>
        <div style="font-size: 11px; color: var(--cyan); margin-bottom: 6px;">Category: ${cat} &bull; Filed by: ${escapeHtml(student)}</div>
        <p style="font-size: 12px; color: var(--text-secondary);">${escapeHtml(desc)}</p>
        ${notes ? `
          <div style="margin-top: 10px; padding: 8px; background: rgba(52, 211, 153, 0.08); border-left: 2px solid var(--green); border-radius: 4px;">
            <div style="font-size: 11px; font-weight: 700; color: var(--green);">Resolution:</div>
            <div style="font-size: 12px; color: var(--text-secondary);">${escapeHtml(notes)}</div>
          </div>
        ` : ''}
        ${isStaff && status !== 'RESOLVED' ? `
          <div class="record-actions" style="margin-top: 10px;">
            <button class="btn btn-secondary btn-sm" onclick="quickResolveComplaint('${id}')">Resolve Ticket</button>
          </div>
        ` : ''}
      </div>
    `;
  }).join('');
}

function getComplaintBadgeClass(status) {
  switch (status) {
    case 'SUBMITTED': return 'badge-amber';
    case 'IN_REVIEW': return 'badge-cyan';
    case 'IN_PROGRESS': return 'badge-blue';
    case 'RESOLVED': return 'badge-green';
    default: return 'badge-slate';
  }
}

function openAddComplaintModal() {
  document.getElementById('cmpTitle').value = '';
  document.getElementById('cmpDesc').value = '';
  openModal('complaintModal');
}

async function submitComplaintForm(e) {
  e.preventDefault();
  const payload = {
    category: document.getElementById('cmpCategory').value,
    title: document.getElementById('cmpTitle').value.trim(),
    description: document.getElementById('cmpDesc').value.trim()
  };

  try {
    await apiCall('/complaints', { method: 'POST', body: JSON.stringify(payload) });
    closeModal('complaintModal');
    showToast('Complaint ticket filed successfully', 'success');
    loadComplaints();
  } catch (err) {
    showToast('Failed to submit ticket: ' + err.message, 'error');
  }
}

async function quickResolveComplaint(id) {
  const note = prompt('Enter official resolution note:', 'Issue inspected and resolved by campus facility staff.');
  if (!note) return;

  try {
    await apiCall('/complaints/' + id + '/status', {
      method: 'PUT',
      body: JSON.stringify({ status: 'RESOLVED', resolutionNotes: note })
    });
    showToast('Ticket marked as resolved', 'success');
    loadComplaints();
  } catch (err) {
    showToast('Failed to update ticket: ' + err.message, 'error');
  }
}

// -----------------------------------------------------------------------------
// Reports, Backups & Desktop Sync
// -----------------------------------------------------------------------------
async function loadLowAttendanceReport() {
  const container = document.getElementById('lowAttendanceReportList');
  if (!container) return;

  try {
    const list = await apiCall('/reports/low-attendance');
    if (!list || list.length === 0) {
      container.innerHTML = '<div style="color: var(--green); font-size: 13px;">&#10004; All active students meet or exceed the 75% attendance threshold!</div>';
      return;
    }

    container.innerHTML = list.map(item => {
      const name = item.studentName || item.name || 'Student';
      const id = item.studentId || item.rollNumber || '';
      const course = item.courseCode || item.courseName || '';
      const pct = (item.percentage !== undefined) ? item.percentage.toFixed(1) : '0.0';
      return `
        <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.06);">
          <div>
            <div style="font-weight: 700; font-size: 13px;">${escapeHtml(name)}</div>
            <div style="font-size: 11px; color: var(--text-muted);">${escapeHtml(id)} &bull; ${escapeHtml(course)}</div>
          </div>
          <span class="badge badge-red">${pct}%</span>
        </div>
      `;
    }).join('');
  } catch (err) {
    container.innerHTML = '<div style="color: var(--rose);">Failed to load low-attendance report.</div>';
  }
}

async function createDataBackup() {
  try {
    const res = await apiCall('/backup', { method: 'POST' });
    showToast(`Backup created: ${res.backupName}`, 'success');
  } catch (err) {
    showToast('Backup failed: ' + err.message, 'error');
  }
}

async function syncWithDesktop() {
  try {
    const res = await apiCall('/sync/desktop', { method: 'POST' });
    showToast(res.message || 'Synchronized with desktop data', 'success');
  } catch (err) {
    showToast('Sync failed: ' + err.message, 'error');
  }
}

// -----------------------------------------------------------------------------
// Modal & Toast Utilities
// -----------------------------------------------------------------------------
function openModal(id) {
  const m = document.getElementById(id);
  if (m) m.classList.add('open');
}

function closeModal(id) {
  const m = document.getElementById(id);
  if (m) m.classList.remove('open');
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;
  container.innerHTML = ''; // Single active toast

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.textContent = message;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 250);
  }, 2200);
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
