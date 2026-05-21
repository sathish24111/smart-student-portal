package com.smartportal.controllers;

import com.smartportal.models.*;
import com.smartportal.repositories.*;
import com.smartportal.services.AuthService;
import com.smartportal.services.EmailSimulatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private AuthService authService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private AttendanceRecordRepository recordRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private EmailSimulatorService emailSimulatorService;

    // 1. Fetch Student Dashboard Data
    @GetMapping("/student")
    public ResponseEntity<?> getStudentDashboard(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized student access."));
        }

        Long studentId = sessionOpt.get().getId();
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Student not found."));
        }

        Student student = studentOpt.get();
        Map<String, Object> data = new HashMap<>();
        
        // Basic Profile Info
        data.put("studentName", student.getName());
        data.put("rollNumber", student.getRollNumber());
        data.put("attendancePercentage", student.getAttendancePercentage());
        data.put("departmentName", student.getDepartment() != null ? student.getDepartment().getName() : "General");

        // Attendance stats
        long totalRecords = recordRepository.countByStudentId(studentId);
        long presentCount = recordRepository.findByStudentId(studentId).stream()
                .filter(r -> "PRESENT".equalsIgnoreCase(r.getStatus())).count();
        long lateCount = recordRepository.findByStudentId(studentId).stream()
                .filter(r -> "LATE".equalsIgnoreCase(r.getStatus())).count();
        long absentCount = totalRecords - (presentCount + lateCount);

        data.put("totalRecords", totalRecords);
        data.put("presentCount", presentCount);
        data.put("lateCount", lateCount);
        data.put("absentCount", Math.max(0, absentCount));

        // Course/Subjects details
        List<Subject> subjects = student.getDepartment() != null ? 
                subjectRepository.findByDepartmentId(student.getDepartment().getId()) : List.of();
        data.put("subjects", subjects);

        // Assignment count
        List<Assignment> activeAssignments = student.getDepartment() != null ?
                assignmentRepository.findBySubjectDepartmentId(student.getDepartment().getId()) : List.of();
        long submissionsCount = submissionRepository.findByStudentId(studentId).size();
        
        data.put("totalAssignments", activeAssignments.size());
        data.put("pendingAssignments", Math.max(0, activeAssignments.size() - submissionsCount));

        // Timetable details
        List<Timetable> timetable = student.getDepartment() != null ?
                timetableRepository.findBySubjectDepartmentId(student.getDepartment().getId()) : List.of();
        data.put("timetable", timetable);

        // Email simulation log (Announcements / Alerts)
        List<EmailSimulatorService.MockEmail> emails = emailSimulatorService.getEmailsForUser(student.getEmail());
        data.put("alerts", emails);

        return ResponseEntity.ok(data);
    }

    // 2. Fetch Staff Dashboard Data
    @GetMapping("/staff")
    public ResponseEntity<?> getStaffDashboard(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized staff access."));
        }

        Long staffId = sessionOpt.get().getId();
        Optional<Staff> staffOpt = staffRepository.findById(staffId);
        if (staffOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Staff not found."));
        }

        Staff staff = staffOpt.get();
        Map<String, Object> data = new HashMap<>();
        
        data.put("staffName", staff.getName());
        data.put("departmentName", staff.getDepartment() != null ? staff.getDepartment().getName() : "General");

        // Taught Subjects
        List<Subject> taughtSubjects = subjectRepository.findAll().stream()
                .filter(s -> staff.getDepartment() != null && s.getDepartment().getId().equals(staff.getDepartment().getId()))
                .collect(Collectors.toList());
        data.put("subjects", taughtSubjects);

        // Students in staff's department
        long studentCount = staff.getDepartment() != null ? 
                studentRepository.findByDepartmentId(staff.getDepartment().getId()).size() : 0;
        data.put("studentCount", studentCount);

        // Active geo-attendance session
        List<AttendanceSession> active = sessionRepository.findByStaffId(staffId).stream()
                .filter(s -> LocalDateTime.now().isBefore(s.getExpiresAt()))
                .collect(Collectors.toList());
        data.put("activeSessions", active);

        // Assignments created by staff
        List<Assignment> staffAssignments = assignmentRepository.findByStaffId(staffId);
        data.put("assignments", staffAssignments);

        // Pending submissions to grade
        List<Submission> pendingGrading = submissionRepository.findByAssignmentStaffId(staffId).stream()
                .filter(s -> s.getGrade() == null)
                .collect(Collectors.toList());
        data.put("pendingGradingCount", pendingGrading.size());

        // Timetable taught by staff
        List<Timetable> schedule = timetableRepository.findByStaffId(staffId);
        data.put("timetable", schedule);

        return ResponseEntity.ok(data);
    }

    // 3. Fetch Admin Dashboard Data
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminDashboard(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"ADMIN".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized admin access."));
        }

        Map<String, Object> data = new HashMap<>();

        // Aggregated Registry counts
        long studentCount = studentRepository.count();
        long staffCount = staffRepository.count();
        long departmentCount = departmentRepository.count();
        long subjectCount = subjectRepository.count();

        data.put("studentCount", studentCount);
        data.put("staffCount", staffCount);
        data.put("departmentCount", departmentCount);
        data.put("subjectCount", subjectCount);

        // Calculate Average student attendance
        List<Student> students = studentRepository.findAll();
        double avgAttendance = students.stream()
                .mapToDouble(Student::getAttendancePercentage)
                .average()
                .orElse(100.0);
        data.put("averageAttendance", Math.round(avgAttendance * 10.0) / 10.0);

        // Active sessions total
        long activeSessionCount = sessionRepository.findByExpiresAtAfter(LocalDateTime.now()).size();
        data.put("activeSessionCount", activeSessionCount);

        // Simple listings for CRUD options mapping
        data.put("departments", departmentRepository.findAll());
        data.put("subjects", subjectRepository.findAll());
        data.put("staffList", staffRepository.findAll());
        data.put("studentsList", students);

        return ResponseEntity.ok(data);
    }
}
