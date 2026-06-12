package com.smartportal.services;

import com.smartportal.models.Admin;
import com.smartportal.models.Notification;
import com.smartportal.models.Staff;
import com.smartportal.models.Student;
import com.smartportal.repositories.AdminRepository;
import com.smartportal.repositories.NotificationRepository;
import com.smartportal.repositories.StaffRepository;
import com.smartportal.repositories.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AdminRepository adminRepository;

    public Notification sendNotification(String roleType, Long userId, String title, String message) {
        Notification notification = new Notification();
        notification.setRoleType(roleType.toUpperCase());
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);

        if ("STUDENT".equalsIgnoreCase(roleType)) {
            Student student = studentRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            notification.setStudent(student);
        } else if ("STAFF".equalsIgnoreCase(roleType)) {
            Staff staff = staffRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Staff member not found"));
            notification.setStaff(staff);
        } else if ("ADMIN".equalsIgnoreCase(roleType)) {
            Admin admin = adminRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));
            notification.setAdmin(admin);
        }

        return notificationRepository.save(notification);
    }

    public List<Notification> getNotifications(String roleType, Long userId) {
        if ("STUDENT".equalsIgnoreCase(roleType)) {
            return notificationRepository.findByStudentId(userId);
        } else if ("STAFF".equalsIgnoreCase(roleType)) {
            return notificationRepository.findByStaffId(userId);
        } else {
            return notificationRepository.findByAdminId(userId);
        }
    }

    public List<Notification> getUnreadNotifications(String roleType, Long userId) {
        if ("STUDENT".equalsIgnoreCase(roleType)) {
            return notificationRepository.findByStudentIdAndIsReadFalse(userId);
        } else if ("STAFF".equalsIgnoreCase(roleType)) {
            return notificationRepository.findByStaffIdAndIsReadFalse(userId);
        } else {
            return notificationRepository.findByAdminIdAndIsReadFalse(userId);
        }
    }

    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(String roleType, Long userId) {
        List<Notification> unread = getUnreadNotifications(roleType, userId);
        for (Notification n : unread) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
