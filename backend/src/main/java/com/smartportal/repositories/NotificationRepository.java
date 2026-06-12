package com.smartportal.repositories;

import com.smartportal.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStudentId(Long studentId);
    List<Notification> findByStaffId(Long staffId);
    List<Notification> findByAdminId(Long adminId);
    
    List<Notification> findByStudentIdAndIsReadFalse(Long studentId);
    List<Notification> findByStaffIdAndIsReadFalse(Long staffId);
    List<Notification> findByAdminIdAndIsReadFalse(Long adminId);
}
