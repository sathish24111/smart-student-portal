package com.smartportal.controllers;

import com.smartportal.models.*;
import com.smartportal.repositories.AttendanceRecordRepository;
import com.smartportal.repositories.AttendanceSessionRepository;
import com.smartportal.repositories.StudentRepository;
import com.smartportal.services.AttendanceService;
import com.smartportal.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private AttendanceRecordRepository recordRepository;

    @Autowired
    private StudentRepository studentRepository;

    // 1. Staff creates attendance session
    @PostMapping("/session")
    public ResponseEntity<?> createSession(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {

        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized staff access."));
        }

        try {
            Long subjectId = Long.valueOf(request.get("subjectId").toString());
            Double latitude = Double.valueOf(request.get("latitude").toString());
            Double longitude = Double.valueOf(request.get("longitude").toString());
            Double radiusMeters = request.containsKey("radiusMeters") ? Double.valueOf(request.get("radiusMeters").toString()) : 100.0;
            int durationMinutes = request.containsKey("durationMinutes") ? Integer.parseInt(request.get("durationMinutes").toString()) : 15;

            AttendanceSession created = attendanceService.createSession(
                    subjectId, sessionOpt.get().getId(), latitude, longitude, radiusMeters, durationMinutes);
            
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error creating session: " + e.getMessage()));
        }
    }

    // 2. Student lists active sessions for their department
    @GetMapping("/active-sessions")
    public ResponseEntity<?> getActiveSessions(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized student access."));
        }

        Optional<Student> studentOpt = studentRepository.findById(sessionOpt.get().getId());
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Student registry not found."));
        }
        
        Student student = studentOpt.get();
        if (student.getDepartment() == null) {
            return ResponseEntity.ok(List.of());
        }

        // Fetch sessions whose expires_at is in the future
        List<AttendanceSession> active = sessionRepository.findByExpiresAtAfter(LocalDateTime.now());
        
        // Filter by student's department subjects
        List<AttendanceSession> departmentActive = active.stream()
                .filter(s -> s.getSubject().getDepartment() != null && 
                             s.getSubject().getDepartment().getId().equals(student.getDepartment().getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(departmentActive);
    }

    // 3. Student submits geolocation-verified attendance
    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request,
            HttpServletRequest servletRequest) {

        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized student access."));
        }

        try {
            Long sessionId = request.containsKey("sessionId") && request.get("sessionId") != null ? Long.valueOf(request.get("sessionId").toString()) : null;
            Double latitude = request.containsKey("latitude") && request.get("latitude") != null ? Double.valueOf(request.get("latitude").toString()) : null;
            Double longitude = request.containsKey("longitude") && request.get("longitude") != null ? Double.valueOf(request.get("longitude").toString()) : null;
            String method = request.containsKey("method") ? request.get("method").toString() : "GPS";
            String qrToken = request.containsKey("qrToken") ? request.get("qrToken").toString() : null;
            String faceEmbedding = request.containsKey("faceEmbedding") ? request.get("faceEmbedding").toString() : null;
            String ipAddress = getClientIp(servletRequest);

            AttendanceRecord record = attendanceService.markAttendance(
                    sessionId, sessionOpt.get().getId(), latitude, longitude, method, qrToken, faceEmbedding, ipAddress);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Attendance marked successfully! Status: " + record.getStatus(),
                    "record", record
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/face/enroll")
    public ResponseEntity<?> enrollFace(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> request) {
        
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized student access."));
        }

        try {
            String embedding = request.get("embeddingVector");
            if (embedding == null || embedding.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "embeddingVector is required"));
            }
            FaceProfile profile = attendanceService.registerFace(sessionOpt.get().getId(), embedding);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // 4. Student views their own history
    @GetMapping("/my-records")
    public ResponseEntity<?> getMyRecords(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized."));
        }
        
        return ResponseEntity.ok(recordRepository.findByStudentId(sessionOpt.get().getId()));
    }

    // 5. Staff views attendance logs for their subjects
    @GetMapping("/staff-records")
    public ResponseEntity<?> getStaffRecords(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized."));
        }

        // Return sessions and records created by this staff member
        List<AttendanceSession> sessions = sessionRepository.findByStaffId(sessionOpt.get().getId());
        List<Long> sessionIds = sessions.stream().map(AttendanceSession::getId).collect(Collectors.toList());
        
        List<AttendanceRecord> records = recordRepository.findAll().stream()
                .filter(r -> sessionIds.contains(r.getSession().getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(records);
    }

    // 6. Admin views all attendance logs
    @GetMapping("/all-records")
    public ResponseEntity<?> getAllRecords(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"ADMIN".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized."));
        }
        return ResponseEntity.ok(recordRepository.findAll());
    }
}
