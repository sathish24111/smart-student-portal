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
    private EmailSimulatorService emailSimulatorService;

    // Haversine formula to calculate geodesic distance in meters between two points
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
        
        // Generate simple QR Code token
        session.setQrCodeToken("ATT_QR_" + System.currentTimeMillis() + "_" + subjectId);
        
        return sessionRepository.save(session);
    }

    @Transactional
    public AttendanceRecord markAttendance(Long sessionId, Long studentId, Double studentLat, Double studentLng) throws Exception {
        // 1. Fetch Session
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new Exception("Attendance session not found."));

        // 2. Verify Session Expiry
        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            throw new Exception("This attendance session has already expired.");
        }

        // 3. Verify Student Registry
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found."));

        // 4. Prevent Multiple Submissions
        Optional<AttendanceRecord> existingRecord = recordRepository.findBySessionIdAndStudentId(sessionId, studentId);
        if (existingRecord.isPresent()) {
            throw new Exception("You have already marked attendance for this session.");
        }

        // 5. Calculate Distance & Validate Radius
        double distance = calculateDistance(session.getLatitude(), session.getLongitude(), studentLat, studentLng);
        boolean isVerified = distance <= session.getRadiusMeters();
        
        if (!isVerified) {
            throw new Exception(String.format("Location verification failed. You are %.1f meters away. Maximum allowed radius is %.1f meters.", 
                    distance, session.getRadiusMeters()));
        }

        // 6. Save Record
        AttendanceRecord record = new AttendanceRecord();
        record.setSession(session);
        record.setStudent(student);
        record.setLatitude(studentLat);
        record.setLongitude(studentLng);
        record.setMarkedAt(LocalDateTime.now());
        record.setIsVerified(true);

        // Determine if LATE (e.g., if marked after 10 minutes from creation)
        if (LocalDateTime.now().isAfter(session.getCreatedAt().plusMinutes(10))) {
            record.setStatus("LATE");
        } else {
            record.setStatus("PRESENT");
        }

        AttendanceRecord savedRecord = recordRepository.save(record);

        // 7. Update Student Attendance Percentage
        recalculateAttendancePercentage(student);

        return savedRecord;
    }

    @Transactional
    public void recalculateAttendancePercentage(Student student) {
        if (student == null || student.getDepartment() == null) return;

        // Total sessions are those created for subjects belonging to the student's department
        List<Subject> subjects = subjectRepository.findByDepartmentId(student.getDepartment().getId());
        if (subjects.isEmpty()) {
            student.setAttendancePercentage(100.0);
            studentRepository.save(student);
            return;
        }

        long totalSessions = 0;
        for (Subject subject : subjects) {
            // Count total sessions created for this subject
            // Let's count them from sessionRepository
            List<AttendanceSession> sessions = sessionRepository.findAll(); // Simple lookup for mock size
            totalSessions += sessions.stream().filter(s -> s.getSubject().getId().equals(subject.getId())).count();
        }

        if (totalSessions == 0) {
            student.setAttendancePercentage(100.0);
            studentRepository.save(student);
            return;
        }

        // Student's present count (status PRESENT or LATE)
        long presentCount = recordRepository.findByStudentId(student.getId()).stream()
                .filter(r -> "PRESENT".equalsIgnoreCase(r.getStatus()) || "LATE".equalsIgnoreCase(r.getStatus()))
                .count();

        double percentage = ((double) presentCount / totalSessions) * 100.0;
        student.setAttendancePercentage(Math.round(percentage * 10.0) / 10.0); // round to 1 decimal
        studentRepository.save(student);

        // Trigger simulation email if low attendance threshold violated
        if (student.getAttendancePercentage() < 75.0) {
            emailSimulatorService.sendLowAttendanceAlert(student, student.getAttendancePercentage());
        }
    }
}
