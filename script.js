// Javascript Core Engine for Smart Student Management Portal

const API_BASE = "http://localhost:8080/api";
// State Management
let currentUser = null; // Contains user session details
let activeTab = "";
let studentSpoofMode = "REAL"; // REAL, IN_RANGE, OUT_RANGE
let currentGPSCoords = { lat: null, lng: null };
let activeSessionsList = []; // For student selection
let selectedSession = null;  // Active selected attendance session

// Initialize Page
document.addEventListener("DOMContentLoaded", () => {
    initTheme();
    initClock();

    // Route based on current HTML page
    const path = window.location.pathname;
    if (path.includes("dashboard.html")) {
        checkAuthentication();
    } else {
        initLoginPage();
    }
});

// ==========================================
// THEME MANAGEMENT
// ==========================================
function initTheme() {
    const savedTheme = localStorage.getItem("theme") || "light";
    document.documentElement.setAttribute("data-theme", savedTheme);

    const themeBtns = [document.getElementById("theme-btn"), document.getElementById("dash-theme-btn")];
    themeBtns.forEach(btn => {
        if (btn) {
            btn.addEventListener("click", () => {
                const current = document.documentElement.getAttribute("data-theme");
                const next = current === "dark" ? "light" : "dark";
                document.documentElement.setAttribute("data-theme", next);
                localStorage.setItem("theme", next);
            });
        }
    });
}

// ==========================================
// TIME TICKER
// ==========================================
function initClock() {
    const ticker = document.getElementById("live-time-ticker");
    if (!ticker) return;
    
    const update = () => {
        const now = new Date();
        ticker.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) + " | " + now.toLocaleDateString();
    };
    update();
    setInterval(update, 1000);
}

// ==========================================
// AUTHENTICATION LOGIC (index.html)
// ==========================================
function initLoginPage() {
    const loginForm = document.getElementById("login-form");
    if (!loginForm) return;

    // Role selector card highlighting
    const roleCards = document.querySelectorAll(".role-card");
    roleCards.forEach(card => {
        card.addEventListener("click", () => {
            roleCards.forEach(c => c.classList.remove("active"));
            card.classList.add("active");
            card.querySelector("input").checked = true;
        });
    });

    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const submitBtn = document.getElementById("login-submit-btn");
        const errorBanner = document.getElementById("login-error");
        
        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;
        const role = loginForm.querySelector("input[name='role']:checked").value;

        submitBtn.disabled = true;
        submitBtn.querySelector("span").textContent = "Authenticating...";
        errorBanner.classList.add("hide");

        try {
            const res = await fetch(`${API_BASE}/auth/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password, role })
            });

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || "Invalid credentials.");
            }

            const data = await res.json();
            
            // Save details
            localStorage.setItem("authToken", data.token);
            localStorage.setItem("userRole", data.role);
            localStorage.setItem("userName", data.name);
            localStorage.setItem("userId", data.id);

            // Redirect
            window.location.href = "dashboard.html";
        } catch (err) {
            errorBanner.classList.remove("hide");
            errorBanner.querySelector(".err-msg").textContent = err.message;
            submitBtn.disabled = false;
            submitBtn.querySelector("span").textContent = "Sign In to Portal";
        }
    });
}

// Autofill for sandbox accounts helper
window.fillCredentials = function(username, password, role) {
    document.getElementById("username").value = username;
    document.getElementById("password").value = password;
    
    const roleCards = document.querySelectorAll(".role-card");
    roleCards.forEach(card => {
        card.classList.remove("active");
        const rad = card.querySelector("input");
        if (rad.value === role) {
            card.classList.add("active");
            rad.checked = true;
        }
    });
};

// ==========================================
// SESSION CHECK (dashboard.html)
// ==========================================
async function checkAuthentication() {
    const token = localStorage.getItem("authToken");
    if (!token) {
        window.location.href = "index.html";
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/auth/validate`, {
            headers: { "Authorization": `Bearer ${token}` }
        });

        if (!res.ok) throw new Error("Invalid session");

        currentUser = await res.json();
        
        // Setup User details in layout
        document.getElementById("user-display-name").textContent = currentUser.name;
        document.getElementById("user-role-badge").textContent = currentUser.role;
        
        const avatarMap = { STUDENT: "🎓", STAFF: "👨‍🏫", ADMIN: "🔑" };
        document.getElementById("avatar-emoji").textContent = avatarMap[currentUser.role] || "👤";

        // Setup Role Panel views
        setupRoleLayouts(currentUser.role);
    } catch (err) {
        handleLogout();
    }
}

function handleLogout() {
    const token = localStorage.getItem("authToken");
    if (token) {
        fetch(`${API_BASE}/auth/logout`, {
            method: "POST",
            headers: { "Authorization": `Bearer ${token}` }
        }).catch(() => {});
    }
    localStorage.clear();
    window.location.href = "index.html";
}

// Setup Dashboard View Panes
function setupRoleLayouts(role) {
    // Hide all menu structures
    document.getElementById("menu-student").classList.add("hide");
    document.getElementById("menu-staff").classList.add("hide");
    document.getElementById("menu-admin").classList.add("hide");

    // Display corresponding role elements
    if (role === "STUDENT") {
        document.getElementById("menu-student").classList.remove("hide");
        switchTab("student-home");
        triggerDeviceCoordinates(); // Boot Geolocation engine
    } else if (role === "STAFF") {
        document.getElementById("menu-staff").classList.remove("hide");
        switchTab("staff-home");
    } else if (role === "ADMIN") {
        document.getElementById("menu-admin").classList.remove("hide");
        switchTab("admin-home");
    }
}

// Tab switcher engine
window.switchTab = function(tabId) {
    activeTab = tabId;

    // Toggle active sidebar item
    const sidebarItems = document.querySelectorAll(".sidebar-menu li");
    sidebarItems.forEach(item => item.classList.remove("active"));
    
    // Find sidebar trigger and activate it
    const clicker = Array.from(sidebarItems).find(i => i.onclick.toString().includes(tabId));
    if (clicker) clicker.classList.add("active");

    // Hide all dashboard tabs
    const panes = document.querySelectorAll(".tab-pane");
    panes.forEach(pane => {
        pane.classList.add("hide");
        pane.classList.remove("active");
    });

    // Display selected pane
    const target = document.getElementById(`tab-${tabId}`);
    if (target) {
        target.classList.remove("hide");
        target.classList.add("active");
    }

    // Dynamic Title descriptions
    const titles = {
        'student-home': { t: "Academic Overview", s: "Real-time attendance logs & analytics dashboard" },
        'student-attendance': { t: "Mark Geofenced Attendance", s: "Submit class attendance using live physical location coordinates" },
        'student-assignments': { t: "Assignment Deadline Tracker", s: "Upload homework files and track professor grade reviews" },
        'student-timetable': { t: "Weekly Lecture Timetable", s: "Your department class schedule calendar" },
        'student-alerts': { t: "Academic Inbox alerts", s: "Low attendance threshold warnings and welcome emails" },
        'staff-home': { t: "Staff Operations Portal", s: "Enrollment metrics & active coordinates broadcast dashboard" },
        'staff-sessions': { t: "Initiate Attendance Session", s: "Start coordinates-bound geofenced attendance windows with QR" },
        'staff-grading': { t: "Homework Grading queue", s: "Evaluate student submission files and submit grading comments" },
        'staff-reports': { t: "Attendance Logs audit", s: "View list of verified coordinates marks from students" },
        'staff-assignments-manage': { t: "Post Assignment Task", s: "Broadcast new homework prompts to student directories" },
        'admin-home': { t: "System Administration", s: "Global portal metrics, courses data, and active server nodes" },
        'admin-students': { t: "Manage Student Registry", s: "Create, view, modify, and delete student accounts" },
        'admin-staff': { t: "Manage Faculty Staff Directory", s: "Create, modify, and manage professor credentials" },
        'admin-courses': { t: "Academic Departments Scheduler", s: "Add courses, list subjects, and organize class divisions" },
        'admin-reports': { t: "Global System Auditing", s: "Audit trail of student attendance GPS matches" }
    };

    const details = titles[tabId] || { t: "Portal Dashboard", s: "System Management Portal" };
    document.getElementById("tab-title").textContent = details.t;
    document.getElementById("tab-subtitle").textContent = details.s;

    // Trigger specific Tab reload feeds
    reloadTabFeeds(tabId);
};

// ==========================================
// DYNAMIC METRIC FEEDS ROUTING
// ==========================================
function reloadTabFeeds(tabId) {
    const token = localStorage.getItem("authToken");

    if (tabId === "student-home" || tabId === "student-alerts") {
        fetchStudentDashboard(token);
    } else if (tabId === "student-attendance") {
        fetchStudentAttendancePortal(token);
    } else if (tabId === "student-assignments") {
        fetchStudentAssignments(token);
    } else if (tabId === "student-timetable") {
        fetchStudentTimetable(token);
    } else if (tabId === "staff-home") {
        fetchStaffDashboard(token);
    } else if (tabId === "staff-sessions") {
        fetchStaffSessionsPage(token);
    } else if (tabId === "staff-grading") {
        fetchStaffGradingPage(token);
    } else if (tabId === "staff-reports") {
        fetchStaffReports(token);
    } else if (tabId === "staff-assignments-manage") {
        fetchStaffAssignmentsManage(token);
    } else if (tabId === "admin-home") {
        fetchAdminDashboard(token);
    } else if (tabId === "admin-students") {
        fetchAdminStudents(token);
    } else if (tabId === "admin-staff") {
        fetchAdminStaff(token);
    } else if (tabId === "admin-courses") {
        fetchAdminCourses(token);
    } else if (tabId === "admin-reports") {
        fetchAdminReports(token);
    }
}

// ==========================================
// 1. STUDENT DASHBOARD MODULES
// ==========================================
async function fetchStudentDashboard(token) {
    try {
        const res = await fetch(`${API_BASE}/dashboard/student`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (!res.ok) throw new Error();
        const data = await res.json();

        // Render Stats
        document.getElementById("student-attendance-percent").textContent = `${data.attendancePercentage}%`;
        document.getElementById("student-progress-fill").style.width = `${data.attendancePercentage}%`;
        document.getElementById("student-present-count").textContent = data.presentCount;
        document.getElementById("student-late-absent-count").textContent = data.lateCount + data.absentCount;
        document.getElementById("student-pending-assignments").textContent = data.pendingAssignments;

        const warningLabel = document.getElementById("attendance-warning-status");
        if (data.attendancePercentage < 75.0) {
            warningLabel.textContent = "🚨 Low Attendance Alert Triggered!";
            warningLabel.className = "metric-trend text-danger";
        } else {
            warningLabel.textContent = "✅ Satisfactory academic status";
            warningLabel.className = "metric-trend text-success";
        }

        // Render mini home alerts
        const alertsContainer = document.getElementById("student-home-alerts");
        const alertBadge = document.getElementById("alert-badge-count");
        
        alertsContainer.innerHTML = "";
        alertBadge.textContent = data.alerts.length;

        if (data.alerts.length === 0) {
            alertsContainer.innerHTML = `<div class="inbox-item-empty">No alerts received. Your academic status is safe!</div>`;
        } else {
            data.alerts.forEach(mail => {
                const item = document.createElement("div");
                item.className = "inbox-item";
                item.innerHTML = `
                    <span class="inbox-item-date">${new Date(mail.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    <h5>${mail.subject}</h5>
                    <p>${mail.body}</p>
                `;
                alertsContainer.appendChild(item);
            });
        }

        // Render Full alerts in alert list tab too
        const fullAlertList = document.getElementById("student-alerts-full-list");
        if (fullAlertList) {
            fullAlertList.innerHTML = "";
            if (data.alerts.length === 0) {
                fullAlertList.innerHTML = `<div class="inbox-item-empty">No alerts in your registry inbox.</div>`;
            } else {
                data.alerts.forEach(mail => {
                    const item = document.createElement("div");
                    item.className = "inbox-item mb-3";
                    item.innerHTML = `
                        <span class="inbox-item-date">${new Date(mail.timestamp).toLocaleString()}</span>
                        <h5>${mail.subject}</h5>
                        <p>${mail.body}</p>
                    `;
                    fullAlertList.appendChild(item);
                });
            }
        }
    } catch (e) {
        console.error("Error updating Student dashboard", e);
    }
}

async function fetchStudentAttendancePortal(token) {
    try {
        const res = await fetch(`${API_BASE}/attendance/active-sessions`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (!res.ok) throw new Error();
        activeSessionsList = await res.json();

        // Populate Select option
        const selector = document.getElementById("active-session-select");
        selector.innerHTML = `<option value="">-- Select Active Lecture --</option>`;

        if (activeSessionsList.length === 0) {
            selector.innerHTML = `<option value="">-- No Active Lectures Found --</option>`;
            document.getElementById("map-msg").style.display = "flex";
            document.getElementById("map-msg").textContent = "No active lectures found in CSE/IT dept.";
            document.getElementById("mark-attendance-btn").disabled = true;
        } else {
            activeSessionsList.forEach(s => {
                const opt = document.createElement("option");
                opt.value = s.id;
                opt.textContent = `${s.subject.name} (${s.subject.code}) - Prof. ${s.staff.name}`;
                selector.appendChild(opt);
            });
        }

        // Reload personal attendance history
        const historyRes = await fetch(`${API_BASE}/attendance/my-records`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const history = await historyRes.json();
        
        const tbody = document.getElementById("student-attendance-table");
        tbody.innerHTML = "";
        
        if (history.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center">No attendance logs registered yet.</td></tr>`;
        } else {
            history.forEach(r => {
                const date = new Date(r.markedAt).toLocaleString();
                const statusPill = r.status === "PRESENT" ? 
                    `<span class="pill-badge student">Present</span>` : 
                    `<span class="pill-badge admin">Late</span>`;

                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${r.session.subject.name}</strong></td>
                    <td>${date}</td>
                    <td><code>${r.latitude.toFixed(6)}, ${r.longitude.toFixed(6)}</code></td>
                    <td><span class="text-success">Verified Inside</span></td>
                    <td>${statusPill}</td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (e) {
        console.error("Error fetching student attendance portal", e);
    }
}

// Map visualization and range feedback updates
window.loadSessionDetails = function() {
    const selector = document.getElementById("active-session-select");
    const sessionId = selector.value;
    const banner = document.getElementById("attendance-status-banner");

    if (!sessionId) {
        selectedSession = null;
        document.getElementById("map-msg").style.display = "flex";
        document.getElementById("map-msg").textContent = "Select an active session to lock coordinates...";
        document.getElementById("mark-attendance-btn").disabled = true;
        document.getElementById("student-distance-lbl").textContent = "--";
        banner.classList.add("hide");
        return;
    }

    selectedSession = activeSessionsList.find(s => s.id == sessionId);
    
    // Update map overlays
    document.getElementById("map-msg").style.display = "none";
    document.getElementById("map-center-lbl").textContent = `${selectedSession.latitude.toFixed(4)}, ${selectedSession.longitude.toFixed(4)}`;
    document.getElementById("map-radius-lbl").textContent = selectedSession.radiusMeters;

    // Refresh range distance checks
    updateLocationRangeAuditor();
};

// Geolocation GPS Simulator control sync
window.toggleGPSSpoof = function() {
    const spoofMode = document.querySelector("input[name='gps-spoof-mode']:checked").value;
    studentSpoofMode = spoofMode;
    
    const radioCards = document.querySelectorAll(".radio-card");
    radioCards.forEach(c => {
        c.classList.remove("active");
        if (c.querySelector("input").value === spoofMode) c.classList.add("active");
    });

    triggerDeviceCoordinates();
};

// Device Geolocation locks
window.triggerDeviceCoordinates = function() {
    const latLbl = document.getElementById("student-lat-lbl");
    const lngLbl = document.getElementById("student-lng-lbl");

    if (studentSpoofMode === "IN_RANGE" && selectedSession) {
        // Spoof inside boundary: center coordinates + tiny offset (approx 2m)
        currentGPSCoords.lat = selectedSession.latitude + 0.00002;
        currentGPSCoords.lng = selectedSession.longitude + 0.00002;
        latLbl.textContent = currentGPSCoords.lat.toFixed(6) + " (Spoofed)";
        lngLbl.textContent = currentGPSCoords.lng.toFixed(6) + " (Spoofed)";
        updateLocationRangeAuditor();
    } else if (studentSpoofMode === "OUT_RANGE") {
        // Spoof far away: Off-campus coordinates (New York midtown cafe!)
        currentGPSCoords.lat = 40.7306;
        currentGPSCoords.lng = -73.9352;
        latLbl.textContent = currentGPSCoords.lat.toFixed(6) + " (Cafe Spoof)";
        lngLbl.textContent = currentGPSCoords.lng.toFixed(6) + " (Cafe Spoof)";
        updateLocationRangeAuditor();
    } else {
        // Use Real Browser geolocation
        latLbl.textContent = "Locking GPS satellite...";
        lngLbl.textContent = "Locking GPS satellite...";
        
        if (!navigator.geolocation) {
            latLbl.textContent = "Not supported";
            lngLbl.textContent = "Not supported";
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (pos) => {
                currentGPSCoords.lat = pos.coords.latitude;
                currentGPSCoords.lng = pos.coords.longitude;
                latLbl.textContent = currentGPSCoords.lat.toFixed(6);
                lngLbl.textContent = currentGPSCoords.lng.toFixed(6);
                updateLocationRangeAuditor();
            },
            (err) => {
                latLbl.textContent = "GPS Blocked / Denied";
                lngLbl.textContent = "GPS Blocked / Denied";
                document.getElementById("student-distance-lbl").textContent = "--";
                document.getElementById("mark-attendance-btn").disabled = true;
            },
            { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 }
        );
    }
};

// Client Side Haversine formula calculation for quick warnings
function calculateHaversineDistance(lat1, lon1, lat2, lon2) {
    const R = 6371000; // in meters
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

function updateLocationRangeAuditor() {
    if (!selectedSession || !currentGPSCoords.lat || !currentGPSCoords.lng) {
        document.getElementById("mark-attendance-btn").disabled = true;
        return;
    }

    const dist = calculateHaversineDistance(
        selectedSession.latitude, selectedSession.longitude,
        currentGPSCoords.lat, currentGPSCoords.lng
    );

    const distLbl = document.getElementById("student-distance-lbl");
    distLbl.textContent = `${dist.toFixed(1)} meters`;

    const banner = document.getElementById("attendance-status-banner");
    banner.classList.remove("hide");

    if (dist <= selectedSession.radiusMeters) {
        distLbl.style.color = "var(--success)";
        banner.className = "status-banner success";
        banner.innerHTML = `<span class="banner-icon">✅</span><span>Verified! You are within the allowed classroom geofence.</span>`;
        document.getElementById("mark-attendance-btn").disabled = false;
    } else {
        distLbl.style.color = "var(--danger)";
        banner.className = "status-banner danger";
        banner.innerHTML = `<span class="banner-icon">🚨</span><span>Out of Range! Maximum allowed classroom geofence is ${selectedSession.radiusMeters}m.</span>`;
        document.getElementById("mark-attendance-btn").disabled = true;
    }
}

async function submitAttendance() {
    if (!selectedSession || !currentGPSCoords.lat || !currentGPSCoords.lng) return;
    
    const token = localStorage.getItem("authToken");
    const btn = document.getElementById("mark-attendance-btn");
    btn.disabled = true;
    btn.querySelector("span:last-child").textContent = "Submitting GPS Audit...";

    try {
        const res = await fetch(`${API_BASE}/attendance/mark`, {
            method: "POST",
            headers: { 
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                sessionId: selectedSession.id,
                latitude: currentGPSCoords.lat,
                longitude: currentGPSCoords.lng
            })
        });

        const data = await res.json();
        
        if (!res.ok) throw new Error(data.message);

        alert(data.message);
        // Refresh portal details
        fetchStudentAttendancePortal(token);
    } catch (err) {
        alert("GPS Attendance Rejected: " + err.message);
    } finally {
        btn.disabled = false;
        btn.querySelector("span:last-child").textContent = "Mark Verified Attendance";
    }
}

// Student homework panels
async function fetchStudentAssignments(token) {
    try {
        const res = await fetch(`${API_BASE}/assignments/student`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const assignments = await res.json();

        // Get submissions list
        const subRes = await fetch(`${API_BASE}/assignments/my-submissions`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const submissions = await subRes.json();

        const tbody = document.getElementById("student-assignments-table");
        tbody.innerHTML = "";

        if (assignments.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center">No assignments assigned in your course syllabus.</td></tr>`;
            return;
        }

        assignments.forEach(a => {
            const submission = submissions.find(s => s.assignment.id === a.id);
            const deadline = new Date(a.dueDate).toLocaleString();
            
            let statusPill = `<span class="pill-badge admin">Pending</span>`;
            let actionBtn = `<button class="btn btn-outline-primary btn-sm" onclick="openSubmitModal(${a.id}, '${a.title}')">Submit File</button>`;
            
            if (submission) {
                if (submission.grade) {
                    statusPill = `<span class="pill-badge staff" title="Feedback: ${submission.feedback || 'None'}">Graded: ${submission.grade}</span>`;
                    actionBtn = `<span class="text-success">Finished ✓</span>`;
                } else {
                    statusPill = `<span class="pill-badge student">Submitted</span>`;
                    actionBtn = `<button class="btn btn-outline btn-sm" onclick="openSubmitModal(${a.id}, '${a.title}')">Resubmit</button>`;
                }
            }

            const downloadLink = a.filePath ? 
                `<a href="http://localhost:8080${a.filePath}" target="_blank" class="btn btn-outline btn-sm">💾 Download</a>` : 
                `<span class="text-muted-small">No attachments</span>`;

            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${a.subject.name}</strong></td>
                <td>
                    <h5>${a.title}</h5>
                    <p class="text-muted-small">${a.description}</p>
                </td>
                <td><code>${deadline}</code></td>
                <td>${downloadLink}</td>
                <td>${statusPill}</td>
                <td>${actionBtn}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error(e);
    }
}

window.openSubmitModal = function(id, title) {
    document.getElementById("submit-assignment-id").value = id;
    document.getElementById("submit-modal-title").textContent = `Upload Homework: ${title}`;
    document.getElementById("submit-assignment-modal").classList.remove("hide");
};

window.closeSubmitModal = function() {
    document.getElementById("submit-assignment-modal").classList.add("hide");
    document.getElementById("submission-form").reset();
};

window.handleAssignmentSubmit = async function(e) {
    e.preventDefault();
    const token = localStorage.getItem("authToken");
    const assignmentId = document.getElementById("submit-assignment-id").value;
    const fileInput = document.getElementById("submission-file");
    
    if (!fileInput.files || fileInput.files.length === 0) return;

    const formData = new FormData();
    formData.append("assignmentId", assignmentId);
    formData.append("file", fileInput.files[0]);

    try {
        const res = await fetch(`${API_BASE}/assignments/submit`, {
            method: "POST",
            headers: { "Authorization": `Bearer ${token}` },
            body: formData
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message);

        alert("Homework uploaded successfully!");
        closeSubmitModal();
        fetchStudentAssignments(token);
    } catch (err) {
        alert("Submission Failed: " + err.message);
    }
};

async function fetchStudentTimetable(token) {
    try {
        const res = await fetch(`${API_BASE}/timetable/student`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const schedule = await res.json();
        
        const tbody = document.getElementById("student-timetable-table");
        tbody.innerHTML = "";

        if (schedule.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center">No timetable events registered.</td></tr>`;
            return;
        }

        schedule.forEach(t => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${t.dayOfWeek}</strong></td>
                <td><h5>${t.subject.name}</h5><p class="text-muted-small">${t.subject.code}</p></td>
                <td>${t.staff.name}</td>
                <td><code>${t.startTime.substring(0,5)} - ${t.endTime.substring(0,5)}</code></td>
                <td><span class="pill-badge student">${t.classroom}</span></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error(e);
    }
}

// ==========================================
// 2. STAFF DASHBOARD MODULES
// ==========================================
async function fetchStaffDashboard(token) {
    try {
        const res = await fetch(`${API_BASE}/dashboard/staff`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const data = await res.json();

        document.getElementById("staff-student-count").textContent = data.studentCount;
        document.getElementById("staff-active-sessions-count").textContent = data.activeSessions.length;
        document.getElementById("staff-pending-grades").textContent = data.pendingGradingCount;

        // Render active sessions
        const sessionTbody = document.getElementById("staff-active-sessions-table");
        sessionTbody.innerHTML = "";
        
        if (data.activeSessions.length === 0) {
            sessionTbody.innerHTML = `<tr><td colspan="5" class="text-center">No active attendance sessions started by you.</td></tr>`;
        } else {
            data.activeSessions.forEach(s => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${s.subject.name}</strong></td>
                    <td><code>${s.latitude.toFixed(4)}, ${s.longitude.toFixed(4)}</code></td>
                    <td>${s.radiusMeters}m</td>
                    <td><code>${new Date(s.expiresAt).toLocaleTimeString()}</code></td>
                    <td><span class="pill-badge student">${s.qrCodeToken}</span></td>
                `;
                sessionTbody.appendChild(tr);
            });
        }

        // Render staff schedules
        const schedTbody = document.getElementById("staff-timetable-table");
        schedTbody.innerHTML = "";
        if (data.timetable.length === 0) {
            schedTbody.innerHTML = `<tr><td colspan="3" class="text-center">No classes registered.</td></tr>`;
        } else {
            data.timetable.forEach(t => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${t.subject.name} (${t.dayOfWeek})</strong></td>
                    <td><code>${t.startTime.substring(0,5)} - ${t.endTime.substring(0,5)}</code></td>
                    <td>${t.classroom}</td>
                `;
                schedTbody.appendChild(tr);
            });
        }
    } catch (e) {
        console.error(e);
    }
}

async function fetchStaffSessionsPage(token) {
    try {
        const res = await fetch(`${API_BASE}/dashboard/staff`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const data = await res.json();

        // Populate subject option select
        const selector = document.getElementById("session-subject-select");
        selector.innerHTML = `<option value="">-- Select Subject --</option>`;
        
        data.subjects.forEach(s => {
            const opt = document.createElement("option");
            opt.value = s.id;
            opt.textContent = `${s.name} (${s.code})`;
            selector.appendChild(opt);
        });

        // Initialize GPS inputs with a default classroom preset
        document.getElementById("session-lat").value = 40.7128;
        document.getElementById("session-lng").value = -74.0060;
    } catch (e) {
        console.error(e);
    }
}

window.getCurrentStaffCoordinates = function() {
    const latIn = document.getElementById("session-lat");
    const lngIn = document.getElementById("session-lng");
    
    latIn.value = "";
    lngIn.value = "";

    if (!navigator.geolocation) {
        alert("Geolocation not supported by your browser");
        return;
    }

    navigator.geolocation.getCurrentPosition(
        (pos) => {
            latIn.value = pos.coords.latitude;
            lngIn.value = pos.coords.longitude;
        },
        (err) => {
            alert("Error locking device coordinates: " + err.message + ". Setting campus default.");
            latIn.value = 40.7128;
            lngIn.value = -74.0060;
        }
    );
};

window.handleCreateSession = async function(e) {
    e.preventDefault();
    const token = localStorage.getItem("authToken");

    const subjectId = document.getElementById("session-subject-select").value;
    const durationMinutes = document.getElementById("session-duration").value;
    const radiusMeters = document.getElementById("session-radius").value;
    const latitude = document.getElementById("session-lat").value;
    const longitude = document.getElementById("session-lng").value;

    try {
        const res = await fetch(`${API_BASE}/attendance/session`, {
            method: "POST",
            headers: { 
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ subjectId, durationMinutes, radiusMeters, latitude, longitude })
        });

        if (!res.ok) throw new Error();
        const session = await res.json();

        alert("Geofenced Session Started Successfully!");

        // Render simulated QR Code on right visual card
        document.getElementById("qr-placeholder").classList.add("hide");
        
        const display = document.getElementById("qr-display-box");
        display.classList.remove("hide");
        document.getElementById("qr-subject-title").textContent = `${session.subject.name} Session Code: ${session.qrCodeToken}`;
        document.getElementById("qr-expire-lbl").textContent = `Expires at: ${new Date(session.expiresAt).toLocaleTimeString()}`;
    } catch (err) {
        alert("Failed to start session. Verify database connection.");
    }
};

async function fetchStaffGradingPage(token) {
    try {
        // Load assignments selector
        const res = await fetch(`${API_BASE}/dashboard/staff`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const data = await res.json();

        const selector = document.getElementById("grading-assignment-select");
        selector.innerHTML = `<option value="">-- Select Assignment --</option>`;

        data.assignments.forEach(a => {
            const opt = document.createElement("option");
            opt.value = a.id;
            opt.textContent = a.title;
            selector.appendChild(opt);
        });
    } catch (e) {
        console.error(e);
    }
}

window.loadGradingSubmissions = async function() {
    const token = localStorage.getItem("authToken");
    const assignmentId = document.getElementById("grading-assignment-select").value;
    const tbody = document.getElementById("staff-grading-table");

    if (!assignmentId) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center">Select an assignment above to populate the queue.</td></tr>`;
        return;
    }

    tbody.innerHTML = `<tr><td colspan="6" class="text-center">Loading submissions...</td></tr>`;

    try {
        const res = await fetch(`${API_BASE}/assignments/submissions?assignmentId=${assignmentId}`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const list = await res.json();

        tbody.innerHTML = "";
        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center">No student submissions uploaded for this task yet.</td></tr>`;
            return;
        }

        list.forEach(s => {
            const date = new Date(s.submittedAt).toLocaleString();
            const gradePill = s.grade ? 
                `<span class="pill-badge student">${s.grade}</span>` : 
                `<span class="pill-badge admin">Ungraded</span>`;

            const actionBtn = `<button class="btn btn-outline-primary btn-sm" onclick="openGradeModal(${s.id})">Grade Card</button>`;

            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${s.student.name}</strong></td>
                <td><code>${s.student.rollNumber}</code></td>
                <td>${date}</td>
                <td><a href="http://localhost:8080${s.filePath}" target="_blank" class="btn btn-outline btn-sm">💾 Homework File</a></td>
                <td>${gradePill}</td>
                <td>${actionBtn}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger">Failed to fetch grading data.</td></tr>`;
    }
};

window.openGradeModal = function(submissionId) {
    document.getElementById("grade-submission-id").value = submissionId;
    document.getElementById("grade-submission-modal").classList.remove("hide");
};

window.closeGradeModal = function() {
    document.getElementById("grade-submission-modal").classList.add("hide");
    document.getElementById("grading-submit-form").reset();
};

window.handleGradeSubmit = async function(e) {
    e.preventDefault();
    const token = localStorage.getItem("authToken");
    
    const submissionId = document.getElementById("grade-submission-id").value;
    const grade = document.getElementById("grade-value-select").value;
    const feedback = document.getElementById("grade-feedback").value;

    try {
        const res = await fetch(`${API_BASE}/assignments/grade`, {
            method: "POST",
            headers: { 
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ submissionId, grade, feedback })
        });

        if (!res.ok) throw new Error();
        
        alert("Submissions evaluated & graded!");
        closeGradeModal();
        loadGradingSubmissions(); // reload queue
    } catch (err) {
        alert("Failed to grade submission.");
    }
};

async function fetchStaffReports(token) {
    try {
        const res = await fetch(`${API_BASE}/attendance/staff-records`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const records = await res.json();

        const tbody = document.getElementById("staff-attendance-logs-table");
        tbody.innerHTML = "";

        if (records.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center">No logs generated.</td></tr>`;
            return;
        }

        records.forEach(r => {
            const date = new Date(r.markedAt).toLocaleString();
            const statusPill = r.status === "PRESENT" ? 
                `<span class="pill-badge student">Present</span>` : 
                `<span class="pill-badge admin">Late</span>`;

            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${r.student.name}</strong></td>
                <td><code>${r.student.rollNumber}</code></td>
                <td>${r.session.subject.name}</td>
                <td>${date}</td>
                <td><code>${r.latitude.toFixed(5)}, ${r.longitude.toFixed(5)}</code></td>
                <td>${statusPill}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error(e);
    }
}

async function fetchStaffAssignmentsManage(token) {
    try {
        // Populate subject options in assignment publisher form
        const res = await fetch(`${API_BASE}/dashboard/staff`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const data = await res.json();

        const selector = document.getElementById("assign-subject-select");
        selector.innerHTML = `<option value="">-- Select Subject --</option>`;
        data.subjects.forEach(s => {
            const opt = document.createElement("option");
            opt.value = s.id;
            opt.textContent = `${s.name} (${s.code})`;
            selector.appendChild(opt);
        });

        // List posted assignments
        const listTbody = document.getElementById("staff-posted-assignments-table");
        listTbody.innerHTML = "";
        
        if (data.assignments.length === 0) {
            listTbody.innerHTML = `<tr><td colspan="4" class="text-center">No assignments posted yet.</td></tr>`;
        } else {
            data.assignments.forEach(a => {
                const due = new Date(a.dueDate).toLocaleDateString();
                const file = a.filePath ? `<a href="http://localhost:8080${a.filePath}" target="_blank">💾 File</a>` : "None";
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${a.subject.name}</strong></td>
                    <td>${a.title}</td>
                    <td><code>${due}</code></td>
                    <td>${file}</td>
                `;
                listTbody.appendChild(tr);
            });
        }
    } catch (e) {
        console.error(e);
    }
}

window.handleCreateAssignment = async function(e) {
    e.preventDefault();
    const token = localStorage.getItem("authToken");

    const subjectId = document.getElementById("assign-subject-select").value;
    const title = document.getElementById("assign-title").value;
    const description = document.getElementById("assign-desc").value;
    const dueDate = document.getElementById("assign-due").value;
    const fileInput = document.getElementById("assign-file");

    const formData = new FormData();
    formData.append("subjectId", subjectId);
    formData.append("title", title);
    formData.append("description", description);
    formData.append("dueDate", dueDate);
    if (fileInput.files.length > 0) {
        formData.append("file", fileInput.files[0]);
    }

    try {
        const res = await fetch(`${API_BASE}/assignments`, {
            method: "POST",
            headers: { "Authorization": `Bearer ${token}` },
            body: formData
        });

        if (!res.ok) throw new Error();
        alert("Assignment Broadcated successfully!");
        document.getElementById("create-assignment-form").reset();
        fetchStaffAssignmentsManage(token);
    } catch (err) {
        alert("Failed to broadcast assignment.");
    }
};

// ==========================================
// 3. ADMIN PORTAL MODULES
// ==========================================
async function fetchAdminDashboard(token) {
    try {
        const res = await fetch(`${API_BASE}/dashboard/admin`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const data = await res.json();

        document.getElementById("admin-total-students").textContent = data.studentCount;
        document.getElementById("admin-total-staff").textContent = data.staffCount;
        document.getElementById("admin-total-depts").textContent = data.departmentCount;
        document.getElementById("admin-avg-attendance").textContent = `${data.averageAttendance}%`;
        document.getElementById("admin-active-sessions-ticker").textContent = data.activeSessionCount;

        // Render mini-depts list
        const tbody = document.getElementById("admin-dept-table-mini");
        tbody.innerHTML = "";
        data.departments.slice(0, 3).forEach(d => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><code>${d.id}</code></td>
                <td><strong>${d.name}</strong></td>
                <td><span class="pill-badge student">${d.code}</span></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error(e);
    }
}

// 3a. Admin Students Registry (CRUD)
let cachedStudentsList = []; // For client searches

async function fetchAdminStudents(token) {
    try {
        const res = await fetch(`${API_BASE}/admin/students`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        cachedStudentsList = await res.json();
        renderStudentsRegistryList(cachedStudentsList);

        // Load departments list in modal selection options
        loadAdminDeptSelections();
    } catch (e) {
        console.error(e);
    }
}

function renderStudentsRegistryList(list) {
    const tbody = document.getElementById("admin-students-table");
    tbody.innerHTML = "";
    
    if (list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center">No students found.</td></tr>`;
        return;
    }

    list.forEach(s => {
        const deptCode = s.department ? s.department.code : "General";
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${s.name}</strong></td>
            <td><code>${s.rollNumber}</code></td>
            <td>${s.username}</td>
            <td>${s.email}</td>
            <td><span class="pill-badge student">${deptCode}</span></td>
            <td><strong>${s.attendancePercentage}%</strong></td>
            <td>
                <button class="btn btn-outline btn-sm" onclick="editStudent(${s.id}, '${s.name}', '${s.rollNumber}', '${s.username}', '${s.email}', ${s.department ? s.department.id : ''})">Edit</button>
                <button class="btn btn-outline-danger btn-sm" onclick="deleteStudent(${s.id})">Delete</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// Client Side instant search filter
window.filterStudentsRegistry = function() {
    const val = document.getElementById("admin-student-search-input").value.toLowerCase().trim();
    if (!val) {
        renderStudentsRegistryList(cachedStudentsList);
        return;
    }

    const filtered = cachedStudentsList.filter(s => 
        s.name.toLowerCase().includes(val) || 
        s.rollNumber.toLowerCase().includes(val) || 
        s.email.toLowerCase().includes(val)
    );
    renderStudentsRegistryList(filtered);
};

async function loadAdminDeptSelections() {
    const token = localStorage.getItem("authToken");
    try {
        const res = await fetch(`${API_BASE}/admin/departments`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const depts = await res.json();

        const studentSel = document.getElementById("student-dept-select");
        const staffSel = document.getElementById("staff-dept-select");
        const subSel = document.getElementById("sub-dept-select");

        const populate = (sel) => {
            if (!sel) return;
            sel.innerHTML = `<option value="">-- Select Department --</option>`;
            depts.forEach(d => {
                const opt = document.createElement("option");
                opt.value = d.id;
                opt.textContent = `${d.name} (${d.code})`;
                sel.appendChild(opt);
            });
        };

        populate(studentSel);
        populate(staffSel);
        populate(subSel);
    } catch (e) {
        console.error(e);
    }
}

window.openStudentModal = function() {
    document.getElementById("student-edit-id").value = "";
    document.getElementById("student-modal-title").textContent = "Add New Student";
    document.getElementById("student-modal").classList.remove("hide");
};

window.closeStudentModal = function() {
    document.getElementById("student-modal").classList.add("hide");
    document.getElementById("student-form").reset();
};

window.editStudent = function(id, name, roll, user, email, deptId) {
    document.getElementById("student-edit-id").value = id;
    document.getElementById("student-name").value = name;
    document.getElementById("student-roll").value = roll;
    document.getElementById("student-username").value = user;
    document.getElementById("student-email").value = email;
    document.getElementById("student-password").value = "********"; // dummy
    document.getElementById("student-dept-select").value = deptId || "";
    
    document.getElementById("student-modal-title").textContent = `Edit Student: ${name}`;
    document.getElementById("student-modal").classList.remove("hide");
};

window.handleSaveStudent = async function(e) {
    e.preventDefault();
    const token = localStorage.getItem("authToken");
    const id = document.getElementById("student-edit-id").value;

    const payload = {
        name: document.getElementById("student-name").value,
        rollNumber: document.getElementById("student-roll").value,
        username: document.getElementById("student-username").value,
        email: document.getElementById("student-email").value,
        departmentId: document.getElementById("student-dept-select").value
    };

    const pwd = document.getElementById("student-password").value;
    if (pwd !== "********") payload.password = pwd;

    try {
        let url = `${API_BASE}/admin/students`;
        let method = "POST";
        if (id) {
            url += `/${id}`;
            method = "PUT";
        }

        const res = await fetch(url, {
            method,
            headers: { 
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!res.ok) throw new Error();

        alert("Student record saved successfully!");
        closeStudentModal();
        fetchAdminStudents(token);
    } catch (err) {
        alert("Failed to save student record.");
    }
};

window.deleteStudent = async function(id) {
    if (!confirm("Are you sure you want to delete this student from academic registry?")) return;
    const token = localStorage.getItem("authToken");

    try {
        const res = await fetch(`${API_BASE}/admin/students/${id}`, {
            method: "DELETE",
            headers: { "Authorization": `Bearer ${token}` }
        });

        if (!res.ok) throw new Error();
        alert("Student deleted successfully!");
        fetchAdminStudents(token);
    } catch (err) {
        alert("Error deleting student.");
    }
};

// 3b. Admin Staff CRUD
async function fetchAdminStaff(token) {
    try {
        const res = await fetch(`${API_BASE}/admin/staff`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const list = await res.json();
        
        const tbody = document.getElementById("admin-staff-table");
        tbody.innerHTML = "";

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center">No faculty registered.</td></tr>`;
            return;
        }

        list.forEach(s => {
            const deptCode = s.department ? s.department.code : "General";
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${s.name}</strong></td>
                <td>${s.username}</td>
                <td>${s.email}</td>
                <td>${s.phone || 'N/A'}</td>
                <td><span class="pill-badge staff">${deptCode}</span></td>
                <td>
                    <button class="btn btn-outline btn-sm" onclick="editStaff(${s.id}, '${s.name}', '${s.username}', '${s.email}', '${s.phone || ''}', ${s.department ? s.department.id : ''})">Edit</button>
                    <button class="btn btn-outline-danger btn-sm" onclick="deleteStaff(${s.id})">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error(e);
    }
}

window.openStaffModal = function() {
    document.getElementById("staff-edit-id").value = "";
    document.getElementById("staff-modal-title").textContent = "Add Faculty Member";
    document.getElementById("staff-modal").classList.remove("hide");
};

window.closeStaffModal = function() {
    document.getElementById("staff-modal").classList.add("hide");
    document.getElementById("staff-form").reset();
};

window.editStaff = function(id, name, user, email, phone, deptId) {
    document.getElementById("staff-edit-id").value = id;
    document.getElementById("staff-name").value = name;
    document.getElementById("staff-username").value = user;
    document.getElementById("staff-email").value = email;
    document.getElementById("staff-phone").value = phone;
    document.getElementById("staff-password").value = "********";
    document.getElementById("staff-dept-select").value = deptId || "";

    document.getElementById("staff-modal-title").textContent = `Edit Faculty: ${name}`;
    document.getElementById("staff-modal").classList.remove("hide");
};

window.handleSaveStaff = async function(e) {
    e.preventDefault();
    const token = localStorage.getItem("authToken");
    const id = document.getElementById("staff-edit-id").value;

    const payload = {
        name: document.getElementById("staff-name").value,
        username: document.getElementById("staff-username").value,
        email: document.getElementById("staff-email").value,
        phone: document.getElementById("staff-phone").value,
        departmentId: document.getElementById("staff-dept-select").value
    };

    const pwd = document.getElementById("staff-password").value;
    if (pwd !== "********") payload.password = pwd;

    try {
        let url = `${API_BASE}/admin/staff`;
        let method = "POST";
        if (id) {
            url += `/${id}`;
            method = "PUT";
        }

        const res = await fetch(url, {
            method,
            headers: { 
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        if (!res.ok) throw new Error();

        alert("Faculty saved successfully!");
        closeStaffModal();
        fetchAdminStaff(token);
    } catch (err) {
        alert("Failed to save faculty record.");
    }
};

window.deleteStaff = async function(id) {
    if (!confirm("Are you sure you want to delete this professor?")) return;
    const token = localStorage.getItem("authToken");

    try {
        const res = await fetch(`${API_BASE}/admin/staff/${id}`, {
            method: "DELETE",
            headers: { "Authorization": `Bearer ${token}` }
        });

        if (!res.ok) throw new Error();
        alert("Faculty record deleted.");
        fetchAdminStaff(token);
    } catch (err) {
        alert("Error deleting faculty.");
    }
};

// 3c. Admin Courses Management (Depts & Subjects)
async function fetchAdminCourses(token) {
    try {
        // Fetch departments
        const deptRes = await fetch(`${API_BASE}/admin/departments`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const depts = await deptRes.json();
        
        const deptTbody = document.getElementById("admin-dept-table-full");
        deptTbody.innerHTML = "";
        
        if (depts.length === 0) {
            deptTbody.innerHTML = `<tr><td colspan="3" class="text-center">No depts created.</td></tr>`;
        } else {
            depts.forEach(d => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><code>${d.code}</code></td>
                    <td><strong>${d.name}</strong></td>
                    <td><button class="btn btn-outline-danger btn-sm" onclick="deleteDept(${d.id})">&times;</button></td>
                `;
                deptTbody.appendChild(tr);
            });
        }

        // Fetch subjects
        const subRes = await fetch(`${API_BASE}/admin/subjects`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const subjects = await subRes.json();
        
        const subTbody = document.getElementById("admin-subject-table-full");
        subTbody.innerHTML = "";

        if (subjects.length === 0) {
            subTbody.innerHTML = `<tr><td colspan="4" class="text-center">No subjects created.</td></tr>`;
        } else {
            subjects.forEach(s => {
                const deptName = s.department ? s.department.name : "N/A";
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><code>${s.code}</code></td>
                    <td><strong>${s.name}</strong></td>
                    <td>${deptName}</td>
                    <td><button class="btn btn-outline-danger btn-sm" onclick="deleteSubject(${s.id})">&times;</button></td>
                `;
                subTbody.appendChild(tr);
            });
        }

        // Reload Select overlays
        loadAdminDeptSelections();
    } catch (e) {
        console.error(e);
    }
}

window.handleSaveDept = async function(e) {
    e.preventDefault();
    const token = localStorage.getItem("authToken");

    const name = document.getElementById("dept-name").value.trim();
    const code = document.getElementById("dept-code").value.trim();

    try {
        const res = await fetch(`${API_BASE}/admin/departments`, {
            method: "POST",
            headers: { 
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ name, code })
        });

        if (!res.ok) throw new Error();
        alert("Department created successfully!");
        document.getElementById("dept-form").reset();
        fetchAdminCourses(token);
    } catch (err) {
        alert("Error saving department.");
    }
};

window.deleteDept = async function(id) {
    if (!confirm("Delete department and unlink all students/staff?")) return;
    const token = localStorage.getItem("authToken");

    try {
        const res = await fetch(`${API_BASE}/admin/departments/${id}`, {
            method: "DELETE",
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (!res.ok) throw new Error();
        fetchAdminCourses(token);
    } catch (err) {
        alert("Error deleting department.");
    }
};

window.handleSaveSubject = async function(e) {
    e.preventDefault();
    const token = localStorage.getItem("authToken");

    const name = document.getElementById("sub-name").value.trim();
    const code = document.getElementById("sub-code").value.trim();
    const departmentId = document.getElementById("sub-dept-select").value;

    try {
        const res = await fetch(`${API_BASE}/admin/subjects`, {
            method: "POST",
            headers: { 
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ name, code, departmentId })
        });

        if (!res.ok) throw new Error();
        alert("Subject created!");
        document.getElementById("subject-form").reset();
        fetchAdminCourses(token);
    } catch (err) {
        alert("Error saving subject.");
    }
};

window.deleteSubject = async function(id) {
    if (!confirm("Delete course subject?")) return;
    const token = localStorage.getItem("authToken");

    try {
        const res = await fetch(`${API_BASE}/admin/subjects/${id}`, {
            method: "DELETE",
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (!res.ok) throw new Error();
        fetchAdminCourses(token);
    } catch (err) {
        alert("Error deleting subject.");
    }
};

// 3d. Admin Global Audit Logs
async function fetchAdminReports(token) {
    try {
        const res = await fetch(`${API_BASE}/attendance/all-records`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        const list = await res.json();
        
        const tbody = document.getElementById("admin-audit-logs-table");
        tbody.innerHTML = "";

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center">No logs generated.</td></tr>`;
            return;
        }

        list.forEach(r => {
            const date = new Date(r.markedAt).toLocaleString();
            
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${r.student.name}</strong></td>
                <td><code>${r.student.rollNumber}</code></td>
                <td>${r.session.subject.name}</td>
                <td>${date}</td>
                <td><code>${r.latitude.toFixed(6)}, ${r.longitude.toFixed(6)}</code></td>
                <td><span class="text-success">Verified Inside</span></td>
                <td><span class="pill-badge student">${r.status}</span></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error(e);
    }
}
