package com.smartportal.repositories;

import com.smartportal.models.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findByStaffId(Long staffId);
    List<AttendanceSession> findByExpiresAtAfter(LocalDateTime now);
    Optional<AttendanceSession> findByQrCodeToken(String token);
}
