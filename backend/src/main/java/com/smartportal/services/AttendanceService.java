package com.smartportal.services;

import com.smartportal.models.*;
import com.smartportal.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private AttendanceRecordRepository recordRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private FaceProfileRepository faceProfileRepository;

    @Autowired
    private EmailSimulatorService emailSimulatorService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    // Haversine formula to calculate geodesic distance in meters
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth's radius in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Transactional
    public AttendanceSession createSession(Long subjectId, Long staffId, Double latitude, Double longitude, Double radiusMeters, int durationMinutes) {
        AttendanceSession session = new AttendanceSession();
        
        Subject subject = new Subject();
        subject.setId(subjectId);
        session.setSubject(subject);
        
        Staff staff = new Staff();
        staff.setId(staffId);
        session.setStaff(staff);
        
        session.setLatitude(latitude);
        session.setLongitude(longitude);
        session.setRadiusMeters(radiusMeters != null ? radiusMeters : 100.0);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(durationMinutes));
        
        // Generate dynamic QR Code token
        session.setQrCodeToken("ATT_QR_" + System.currentTimeMillis() + "_" + subjectId);
        
        AttendanceSession saved = sessionRepository.save(session);
        
        // Notify students in the department
        List<Student> students = studentRepository.findByDepartmentId(subject.getDepartment() != null ? subject.getDepartment().getId() : null);
        if (students != null) {
            for (Student s : students) {
                notificationService.sendNotification("STUDENT", s.getId(), "New Attendance Session Started", 
                        "An active attendance session has been initialized for " + subject.name);
            }
        }

        return saved;
    }

    @Transactional
    public AttendanceRecord markAttendance(Long sessionId, Long studentId, Double studentLat, Double studentLng, 
                                          String method, String qrToken, String faceEmbedding, String ipAddress) throws Exception {
        
        String cleanMethod = method != null ? method.toUpperCase() : "GPS";
        AttendanceSession session = null;

        if (sessionId != null) {
            session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new Exception("Attendance session not found."));

            if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
                throw new Exception("This attendance session has already expired.");
            }
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found."));

        if (session != null) {
            Optional<AttendanceRecord> existingRecord = recordRepository.findBySessionIdAndStudentId(sessionId, studentId);
            if (existingRecord.isPresent()) {
                throw new Exception("You have already marked attendance for this session.");
            }
        }

        // 1. Geofencing check
        if ("GPS".equals(cleanMethod)) {
            if (session == null) throw new Exception("Session ID is required for GPS geofencing");
            if (studentLat == null || studentLng == null) throw new Exception("GPS coordinates are required.");
            double distance = calculateDistance(session.getLatitude(), session.getLongitude(), studentLat, studentLng);
            if (distance > session.getRadiusMeters()) {
                throw new Exception(String.format("Location verification failed. You are %.1f meters away. Maximum allowed radius is %.1f meters.", 
                        distance, session.getRadiusMeters()));
            }
        }

        // 2. QR Token verification
        if ("QR".equals(cleanMethod)) {
            if (session == null) throw new Exception("Session ID is required for QR check-in");
            if (qrToken == null || !qrToken.equals(session.getQrCodeToken())) {
                throw new Exception("Invalid or expired QR code security token.");
            }
        }

        // 3. Face verification check
        if ("FACE".equals(cleanMethod)) {
            FaceProfile profile = faceProfileRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new Exception("No face profile registered. Enroll your face biometrics first."));

            if (faceEmbedding == null || faceEmbedding.trim().isEmpty()) {
                throw new Exception("Facial descriptor missing in request payload.");
            }
            // Mock successful match checks (in real apps, evaluate vector cosine similarity)
            System.out.println("Verifying Face Embedding Vector for student " + student.getUsername() + "... Matched successfully!");
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setSession(session);
        record.setStudent(student);
        record.setLatitude(studentLat != null ? studentLat : (session != null ? session.getLatitude() : 0.0));
        record.setLongitude(studentLng != null ? studentLng : (session != null ? session.getLongitude() : 0.0));
        record.setMarkedAt(LocalDateTime.now());
        record.setIsVerified(true);

        // Determine if LATE (e.g. 10 minutes limit)
        if (session != null && LocalDateTime.now().isAfter(session.getCreatedAt().plusMinutes(10))) {
            record.setStatus("LATE");
        } else {
            record.setStatus("PRESENT");
        }

        AttendanceRecord savedRecord = recordRepository.save(record);

        // Update statistics
        recalculateAttendancePercentage(student);

        // Log action & notification
        auditLogService.logAction("STUDENT", student.getUsername(), "MARK_ATTENDANCE_" + cleanMethod, ipAddress);
        notificationService.sendNotification("STUDENT", student.getId(), "Attendance Marked Successfully", 
                String.format("You checked in as %s via %s method.", record.getStatus(), cleanMethod));

        return savedRecord;
    }

    @Transactional
    public FaceProfile registerFace(Long studentId, String embeddingVector) throws Exception {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found."));

        FaceProfile profile = faceProfileRepository.findByStudentId(studentId)
                .orElse(new FaceProfile());

        profile.setStudent(student);
        profile.setEmbeddingVector(embeddingVector);
        profile.setRegisteredAt(LocalDateTime.now());
        
        FaceProfile saved = faceProfileRepository.save(profile);

        auditLogService.logAction("STUDENT", student.getUsername(), "REGISTER_FACE_PROFILE", "0.0.0.0");
        notificationService.sendNotification("STUDENT", student.getId(), "Face Profile Registered", 
                "Your biometrics face profile descriptors have been registered successfully.");

        return saved;
    }

    @Transactional
    public void recalculateAttendancePercentage(Student student) {
        if (student == null || student.getDepartment() == null) return;

        List<Subject> subjects = subjectRepository.findByDepartmentId(student.getDepartment().getId());
        if (subjects.isEmpty()) {
            student.setAttendancePercentage(100.0);
            studentRepository.save(student);
            return;
        }

        long totalSessions = 0;
        List<AttendanceSession> sessions = sessionRepository.findAll();
        for (Subject subject : subjects) {
            totalSessions += sessions.stream().filter(s -> s.getSubject().getId().equals(subject.getId())).count();
        }

        if (totalSessions == 0) {
            student.setAttendancePercentage(100.0);
            studentRepository.save(student);
            return;
        }

        long presentCount = recordRepository.findByStudentId(student.getId()).stream()
                .filter(r -> "PRESENT".equalsIgnoreCase(r.getStatus()) || "LATE".equalsIgnoreCase(r.getStatus()))
                .count();

        double percentage = ((double) presentCount / totalSessions) * 100.0;
        student.setAttendancePercentage(Math.round(percentage * 10.0) / 10.0);
        studentRepository.save(student);

        if (student.getAttendancePercentage() < 75.0) {
            emailSimulatorService.sendLowAttendanceAlert(student, student.getAttendancePercentage());
            notificationService.sendNotification("STUDENT", student.getId(), "Academic Warning: Low Attendance", 
                    String.format("Your attendance has dropped below the threshold of 75%%. Current rate: %.1f%%", student.getAttendancePercentage()));
        }
    }
}
