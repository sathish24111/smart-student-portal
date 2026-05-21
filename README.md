# 🎓 Smart Student Management Portal with Location-Based Geofencing

An advanced, premium-designed, full-stack Student Management Portal that secures attendance marking by validating the student's physical location against classroom GPS coordinates using **Geofencing**. Students can only mark themselves present when they are inside the allowed radius (e.g., 100 meters) set by the professor.

This application is built with a **Java Spring Boot backend**, a **MySQL database**, and a **Vanilla HTML/CSS/JS frontend** featuring dynamic role-based dashboards, Light/Dark themes, glassmorphism aesthetics, an integrated GPS coordinate simulator, QR session builders, assignment submission vaults, and an automated low-attendance email alert simulator.

---

## 🚀 Key Features

*   **🔒 Secure Session Authentication**: Role-based access control for Admins, Staff, and Students.
*   **📍 Location-Based Attendance**: Haversine distance algorithm verified on the backend prevents proxy or fake attendance logs.
*   **📱 GPS Mock & Simulator Control**: Integrated panel in the student dashboard allows developers to mock coordinates easily (on-campus vs. off-campus midtown cafe) on non-HTTPS local runs.
*   **📂 Homework & Grading Repository**: Students upload assignments (PDF/DOC), and staff can view, review, and grade submissions.
*   **📅 Interactive Timetable Schedule**: Unified schedule for subjects, classrooms, and assigned lecturers.
*   **📬 Email Alert Logging Simulator**: Automated warning notifications log when attendance drops below the **75% academic threshold**.
*   **🔑 Full Admin Panel**: Manage student registries, add faculty profiles, configure departments, and audit global attendance logs.

---

## 🛠️ Technology Stack

*   **Frontend**: HTML5, Vanilla CSS3 (Custom variables, responsive grid systems, animations), Vanilla JavaScript ES6 (Fetch API, DOM rendering, client-side math).
*   **Backend**: Java 17, Spring Boot 3.2.5 (Spring MVC, Spring Data JPA).
*   **Database**: MySQL 8.x.
*   **Build Tool**: Apache Maven.

---

## 📦 Database Setup (MySQL)

1.  Start your MySQL server on port `3306`.
2.  Login to your MySQL database command line or GUI tool and create the database:
    ```sql
    CREATE DATABASE IF NOT EXISTS student_portal;
    ```
3.  Configure database credentials in the application configuration if they differ from the defaults:
    *   File location: `backend/src/main/resources/application.properties`
    *   Default settings:
        ```properties
        spring.datasource.url=jdbc:mysql://localhost:3306/student_portal?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
        spring.datasource.username=root
        spring.datasource.password=password
        ```

> [!NOTE]
> The database schema and relations are automatically initialized on application boot using the `schema.sql` definition script, and automatically seeded with robust mockup demo accounts via the Spring boot loader `DataInitializer`.

---

## 📂 Project Directory Structure

```text
smart-student-portal/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/smartportal/   # Spring Boot Source Code
│   │   │   └── resources/
│   │   │       ├── static/             # Unified Frontend static assets
│   │   │       │   ├── index.html       # Landing Page / Login
│   │   │       │   ├── dashboard.html   # Main Dashboard (3-in-1 Role Shells)
│   │   │       │   ├── style.css        # Premium stylesheets
│   │   │       │   └── script.js        # Core Javascript AJAX engine
│   │   │       ├── application.properties
│   │   │       └── schema.sql          # MySQL Relational Schema
│   │   └── test/
│   └── pom.xml                         # Maven dependencies
├── frontend/                           # Separate Frontend source copy
│   ├── index.html
│   ├── dashboard.html
│   ├── style.css
│   └── script.js
└── README.md                           # This setup guide
```

---

## 🚀 Running the Application Locally

### Step 1: Compile the Backend
Open a terminal in the `backend/` directory and compile the codebase:
```bash
mvn clean install
```

### Step 2: Boot up the Spring Boot Service
Start the dev server:
```bash
mvn spring-boot:run
```

Once running successfully, the system console will log `Tomcat started on port 8080`.

### Step 3: Open the Web Portal
Launch your favorite modern web browser and navigate to:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 🧪 Developer Demo Sandbox Credentials

The system seeds the database with the following demo credentials during the first startup:

| Role | Username | Password | Purpose & View |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | Manage departments/subjects, staff directory, student registry, view audit logs. |
| **Faculty Staff** | `prof_smith` | `staff123` | Start classroom coordinates attendance sessions, post assignments, grade papers. |
| **Faculty Staff** | `prof_davis` | `staff123` | Backup science instructor credentials. |
| **Student** | `student_john` | `student123` | Mark geolocated attendance, submit homework files, view schedule, read academic alerts. |
| **Student** | `student_jane` | `student123` | Alternate student account. |

---

## 📍 Step-by-Step Geofenced Testing Walkthrough

Follow these steps to demonstrate the core geolocation verification mechanics:

### 1. Create a Geofenced Attendance Session (Staff)
1. Log into the portal using **Staff** credentials (`prof_smith` / `staff123`).
2. Go to the **Initiate Attendance Session** tab.
3. Select a subject (e.g., *Software Engineering*).
4. Set the coordinates. Click **Use Current Device Location** or leave the default classroom coordinates:
   * **Latitude**: `40.712800`
   * **Longitude**: `-74.006000`
5. Specify the active **Duration** (e.g., 30 minutes) and allowed **Radius** (e.g., 100 meters).
6. Click **Activate Geofenced Session**. A simulated QR Code with the session details will immediately generate on the panel!

### 2. Test "Out of Range" Rejection (Student)
1. In a separate browser tab/window, open the portal and log in as a **Student** (`student_john` / `student123`).
2. Navigate to the **Mark Geofenced Attendance** tab.
3. Select the active lecture from the dropdown list.
4. Locate the **GPS Simulator Control Box** on the right side.
5. Select **Spoof: Cafe (Out of Range)**.
6. Click **Sync Location**. The tracker will lock onto coordinates far away (e.g., Midtown Cafe coordinates `40.730600, -73.935200`).
7. Notice that the live distance calculation shows `~6300 meters` and the alert turns red showing: `🚨 Out of Range! Maximum allowed classroom geofence is 100m`.
8. The **Mark Verified Attendance** button will be automatically **blocked & disabled**. Attempting to bypass this on the backend will result in a hard API coordinates rejection.

### 3. Test "In Range" Success (Student)
1. On the same Student attendance panel, switch the **GPS Simulator Control Box** to **Spoof: Inside Classroom (In Range)**.
2. Click **Sync Location**.
3. Notice that the location is spoofed within a few meters of the professor's setup coordinates. The alert bar turns green: `✅ Verified! You are within the allowed classroom geofence`.
4. The **Mark Verified Attendance** button is now unlocked!
5. Click **Mark Verified Attendance**. You will see a success confirmation popup.
6. The dashboard will instantly update, showing your registered record in the **Personal Attendance History** table with the exact GPS markers.

### 4. Review Academic Audit Trail (Staff & Admin)
1. Log back in as **Staff** or **Admin**.
2. Staff can go to **Attendance Logs Audit** to see Student John marked as **PRESENT** along with his verified coordinates.
3. Admin can go to **Global System Auditing** to see a full system-wide overview of all geolocated attendance matches across the campus!

---
*Created with 💙 by Antigravity.*
