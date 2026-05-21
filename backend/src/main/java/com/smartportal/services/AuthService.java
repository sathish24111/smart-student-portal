package com.smartportal.services;

import com.smartportal.models.Admin;
import com.smartportal.models.Staff;
import com.smartportal.models.Student;
import com.smartportal.repositories.AdminRepository;
import com.smartportal.repositories.StaffRepository;
import com.smartportal.repositories.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StudentRepository studentRepository;

    // Session token storage: Maps token -> UserSessionDetails
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    public static class UserSession {
        private final String token;
        private final String role; // "ADMIN", "STAFF", "STUDENT"
        private final Long id;
        private final String username;
        private final String name;

        public UserSession(String token, String role, Long id, String username, String name) {
            this.token = token;
            this.role = role;
            this.id = id;
            this.username = username;
            this.name = name;
        }

        public String getToken() { return token; }
        public String getRole() { return role; }
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getName() { return name; }
    }

    public Optional<UserSession> login(String username, String password, String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            Optional<Admin> adminOpt = adminRepository.findByUsername(username);
            if (adminOpt.isPresent() && adminOpt.get().getPassword().equals(password)) {
                Admin admin = adminOpt.get();
                String token = "ADMIN_" + UUID.randomUUID().toString().replace("-", "");
                UserSession session = new UserSession(token, "ADMIN", admin.getId(), admin.getUsername(), admin.getName());
                activeSessions.put(token, session);
                return Optional.of(session);
            }
        } else if ("STAFF".equalsIgnoreCase(role)) {
            Optional<Staff> staffOpt = staffRepository.findByUsername(username);
            if (staffOpt.isPresent() && staffOpt.get().getPassword().equals(password)) {
                Staff staff = staffOpt.get();
                String token = "STAFF_" + UUID.randomUUID().toString().replace("-", "");
                UserSession session = new UserSession(token, "STAFF", staff.getId(), staff.getUsername(), staff.getName());
                activeSessions.put(token, session);
                return Optional.of(session);
            }
        } else if ("STUDENT".equalsIgnoreCase(role)) {
            Optional<Student> studentOpt = studentRepository.findByUsername(username);
            if (studentOpt.isPresent() && studentOpt.get().getPassword().equals(password)) {
                Student student = studentOpt.get();
                String token = "STUDENT_" + UUID.randomUUID().toString().replace("-", "");
                UserSession session = new UserSession(token, "STUDENT", student.getId(), student.getUsername(), student.getName());
                activeSessions.put(token, session);
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    public Optional<UserSession> validateToken(String token) {
        if (token == null) return Optional.empty();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Optional.ofNullable(activeSessions.get(token));
    }

    public void logout(String token) {
        if (token != null) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            activeSessions.remove(token);
        }
    }
}
