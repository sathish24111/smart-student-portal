package com.smartportal.services;

import com.smartportal.models.*;
import com.smartportal.repositories.LeaveRequestRepository;
import com.smartportal.repositories.StudentRepository;
import com.smartportal.repositories.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    public LeaveRequest applyLeave(String roleType, Long userId, LocalDate start, LocalDate end, String reason, String ipAddress) {
        if (start.isAfter(end)) {
            throw new RuntimeException("Start date cannot be after end date");
        }

        LeaveRequest request = new LeaveRequest();
        request.setRoleType(roleType.toUpperCase());
        request.setStartDate(start);
        request.setEndDate(end);
        request.setReason(reason);
        request.setStatus("PENDING");

        String username = "";
        if ("STUDENT".equalsIgnoreCase(roleType)) {
            Student student = studentRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            request.setStudent(student);
            username = student.getUsername();
        } else if ("STAFF".equalsIgnoreCase(roleType)) {
            Staff staff = staffRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Staff member not found"));
            request.setStaff(staff);
            username = staff.getUsername();
        } else {
            throw new RuntimeException("Invalid role for leave request: must be STUDENT or STAFF");
        }

        LeaveRequest saved = leaveRequestRepository.save(request);

        auditLogService.logAction(roleType, username, "APPLY_LEAVE", ipAddress);
        notificationService.sendNotification(roleType, userId, "Leave Request Submitted", 
                String.format("Your leave request from %s to %s has been submitted.", start, end));

        return saved;
    }

    public LeaveRequest approveLeave(Long leaveId, String status, String comments, Admin admin, String ipAddress) {
        LeaveRequest request = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Leave request already evaluated");
        }

        String newStatus = status.toUpperCase();
        if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new RuntimeException("Invalid status: must be APPROVED or REJECTED");
        }

        request.setStatus(newStatus);
        request.setComments(comments);
        request.setApprovedByAdmin(admin);

        LeaveRequest saved = leaveRequestRepository.save(request);

        auditLogService.logAction("ADMIN", admin.getUsername(), "PROCESS_LEAVE_" + newStatus, ipAddress);

        // Notify applicant
        if (request.getStudent() != null) {
            notificationService.sendNotification("STUDENT", request.getStudent().getId(), "Leave Request Status Update", 
                    String.format("Your leave request has been %s by Admin. Comments: %s", newStatus.toLowerCase(), comments));
        } else if (request.getStaff() != null) {
            notificationService.sendNotification("STAFF", request.getStaff().getId(), "Leave Request Status Update", 
                    String.format("Your leave request has been %s by Admin. Comments: %s", newStatus.toLowerCase(), comments));
        }

        return saved;
    }

    public List<LeaveRequest> getUserLeaves(String roleType, Long userId) {
        if ("STUDENT".equalsIgnoreCase(roleType)) {
            return leaveRequestRepository.findByStudentId(userId);
        } else {
            return leaveRequestRepository.findByStaffId(userId);
        }
    }

    public List<LeaveRequest> getAllLeaves() {
        return leaveRequestRepository.findAll();
    }

    public List<LeaveRequest> getLeavesByStatus(String status) {
        return leaveRequestRepository.findByStatus(status);
    }
}
