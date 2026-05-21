package com.smartportal.repositories;

import com.smartportal.models.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByStudentId(Long studentId);
    List<AttendanceRecord> findBySessionId(Long sessionId);
    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    List<AttendanceRecord> findBySessionSubjectIdAndStudentId(Long subjectId, Long studentId);
    long countByStudentIdAndStatus(Long studentId, String status);
    long countByStudentId(Long studentId);
}
