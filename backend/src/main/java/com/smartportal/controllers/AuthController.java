package com.smartportal.controllers;

import com.smartportal.models.Admin;
import com.smartportal.models.Staff;
import com.smartportal.models.Student;
import com.smartportal.repositories.AdminRepository;
import com.smartportal.repositories.StaffRepository;
import com.smartportal.repositories.StudentRepository;
import com.smartportal.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AdminRepository adminRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String role = request.get("role");

        if (username == null || password == null || role == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username, password, and role are required."));
        }

        Optional<AuthService.UserSession> sessionOpt = authService.login(username, password, role);
        if (sessionOpt.isPresent()) {
            return ResponseEntity.ok(sessionOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid credentials or role selection."));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader(value = "Authorization", required = false) String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isPresent()) {
            return ResponseEntity.ok(sessionOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid or expired session."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization", required = false) String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized access."));
        }

        AuthService.UserSession session = sessionOpt.get();
        if ("STUDENT".equalsIgnoreCase(session.getRole())) {
            Optional<Student> s = studentRepository.findById(session.getId());
            if (s.isPresent()) return ResponseEntity.ok(s.get());
        } else if ("STAFF".equalsIgnoreCase(session.getRole())) {
            Optional<Staff> sf = staffRepository.findById(session.getId());
            if (sf.isPresent()) return ResponseEntity.ok(sf.get());
        } else if ("ADMIN".equalsIgnoreCase(session.getRole())) {
            Optional<Admin> a = adminRepository.findById(session.getId());
            if (a.isPresent()) return ResponseEntity.ok(a.get());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Profile not found."));
    }
}
