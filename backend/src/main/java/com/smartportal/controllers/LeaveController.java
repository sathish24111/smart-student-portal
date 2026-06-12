package com.smartportal.controllers;

import com.smartportal.models.*;
import com.smartportal.repositories.AdminRepository;
import com.smartportal.services.AuthService;
import com.smartportal.services.LeaveService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminRepository adminRepository;

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized session"));
        }

        AuthService.UserSession session = sessionOpt.get();
        String startStr = payload.get("startDate");
        String endStr = payload.get("endDate");
        String reason = payload.get("reason");

        if (startStr == null || endStr == null || reason == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "startDate, endDate, and reason are required"));
        }

        try {
            LocalDate start = LocalDate.parse(startStr);
            LocalDate end = LocalDate.parse(endStr);
            String ipAddress = getClientIp(request);
            
            LeaveRequest leave = leaveService.applyLeave(
                    session.getRole(), session.getId(), start, end, reason, ipAddress);
            return ResponseEntity.ok(leave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveLeave(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {

        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"ADMIN".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized admin session"));
        }

        Admin admin = adminRepository.findById(sessionOpt.get().getId())
                .orElseThrow(() -> new RuntimeException("Admin details not found"));

        String status = payload.get("status");
        String comments = payload.get("comments");

        if (status == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "status is required (APPROVED or REJECTED)"));
        }

        try {
            String ipAddress = getClientIp(request);
            LeaveRequest leave = leaveService.approveLeave(id, status, comments, admin, ipAddress);
            return ResponseEntity.ok(leave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getMyLeaves(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized session"));
        }

        AuthService.UserSession session = sessionOpt.get();
        if ("ADMIN".equalsIgnoreCase(session.getRole())) {
            return ResponseEntity.ok(leaveService.getAllLeaves());
        }

        List<LeaveRequest> list = leaveService.getUserLeaves(session.getRole(), session.getId());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllLeaves(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || "STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized access"));
        }
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingLeaves(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || "STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized access"));
        }
        return ResponseEntity.ok(leaveService.getLeavesByStatus("PENDING"));
    }
}
