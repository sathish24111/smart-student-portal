package com.smartportal.controllers;

import com.smartportal.models.*;
import com.smartportal.repositories.*;
import com.smartportal.services.AuthService;
import com.smartportal.services.EmailSimulatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AuthService authService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private EmailSimulatorService emailSimulatorService;

    private boolean verifyAdmin(String token) {
        Optional<AuthService.UserSession> session = authService.validateToken(token);
        return session.isPresent() && "ADMIN".equalsIgnoreCase(session.get().getRole());
    }

    // ==========================================
    // DEPARTMENT CRUD
    // ==========================================
    @GetMapping("/departments")
    public ResponseEntity<?> listDepartments(@RequestHeader("Authorization") String token) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @PostMapping("/departments")
    public ResponseEntity<?> createDepartment(
            @RequestHeader("Authorization") String token,
            @RequestBody Department department) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        try {
            return ResponseEntity.ok(departmentRepository.save(department));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<?> deleteDepartment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        try {
            departmentRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Department deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error deleting department."));
        }
    }

    // ==========================================
    // SUBJECT CRUD
    // ==========================================
    @GetMapping("/subjects")
    public ResponseEntity<?> listSubjects(@RequestHeader("Authorization") String token) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        return ResponseEntity.ok(subjectRepository.findAll());
    }

    @PostMapping("/subjects")
    public ResponseEntity<?> createSubject(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> req) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        try {
            Subject subject = new Subject();
            subject.setName(req.get("name").toString());
            subject.setCode(req.get("code").toString());
            
            Long deptId = Long.valueOf(req.get("departmentId").toString());
            Department dept = departmentRepository.findById(deptId).orElseThrow();
            subject.setDepartment(dept);

            return ResponseEntity.ok(subjectRepository.save(subject));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error creating subject: " + e.getMessage()));
        }
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<?> deleteSubject(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        try {
            subjectRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Subject deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error deleting subject."));
        }
    }

    // ==========================================
    // STAFF CRUD
    // ==========================================
    @GetMapping("/staff")
    public ResponseEntity<?> listStaff(@RequestHeader("Authorization") String token) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        return ResponseEntity.ok(staffRepository.findAll());
    }

    @PostMapping("/staff")
    public ResponseEntity<?> createStaff(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> req) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        try {
            Staff staff = new Staff();
            staff.setUsername(req.get("username").toString());
            staff.setPassword(req.get("password").toString());
            staff.setName(req.get("name").toString());
            staff.setEmail(req.get("email").toString());
            staff.setPhone(req.containsKey("phone") ? req.get("phone").toString() : "");

            if (req.containsKey("departmentId") && req.get("departmentId") != null) {
                Long deptId = Long.valueOf(req.get("departmentId").toString());
                Department dept = departmentRepository.findById(deptId).orElse(null);
                staff.setDepartment(dept);
            }

            Staff saved = staffRepository.save(staff);
            emailSimulatorService.sendWelcomeEmail("Staff Member", saved.getName(), saved.getEmail(), saved.getUsername());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<?> updateStaff(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody Map<String, Object> req) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        try {
            Staff staff = staffRepository.findById(id).orElseThrow();
            if (req.containsKey("name")) staff.setName(req.get("name").toString());
            if (req.containsKey("email")) staff.setEmail(req.get("email").toString());
            if (req.containsKey("phone")) staff.setPhone(req.get("phone").toString());
            if (req.containsKey("password")) staff.setPassword(req.get("password").toString());
            if (req.containsKey("departmentId") && req.get("departmentId") != null) {
                Long deptId = Long.valueOf(req.get("departmentId").toString());
                Department dept = departmentRepository.findById(deptId).orElse(null);
                staff.setDepartment(dept);
            }
            return ResponseEntity.ok(staffRepository.save(staff));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<?> deleteStaff(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        try {
            staffRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Staff deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error deleting staff."));
        }
    }

    // ==========================================
    // STUDENT CRUD
    // ==========================================
    @GetMapping("/students")
    public ResponseEntity<?> listStudents(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "search", required = false) String search) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(studentRepository.findByNameContainingIgnoreCaseOrRollNumberContainingIgnoreCase(search, search));
        }
        return ResponseEntity.ok(studentRepository.findAll());
    }

    @PostMapping("/students")
    public ResponseEntity<?> createStudent(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> req) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        try {
            Student student = new Student();
            student.setUsername(req.get("username").toString());
            student.setPassword(req.get("password").toString());
            student.setName(req.get("name").toString());
            student.setEmail(req.get("email").toString());
            student.setRollNumber(req.get("rollNumber").toString());
            student.setAttendancePercentage(100.0);

            if (req.containsKey("departmentId") && req.get("departmentId") != null) {
                Long deptId = Long.valueOf(req.get("departmentId").toString());
                Department dept = departmentRepository.findById(deptId).orElse(null);
                student.setDepartment(dept);
            }

            Student saved = studentRepository.save(student);
            emailSimulatorService.sendWelcomeEmail("Student", saved.getName(), saved.getEmail(), saved.getUsername());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<?> updateStudent(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody Map<String, Object> req) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        try {
            Student student = studentRepository.findById(id).orElseThrow();
            if (req.containsKey("name")) student.setName(req.get("name").toString());
            if (req.containsKey("email")) student.setEmail(req.get("email").toString());
            if (req.containsKey("rollNumber")) student.setRollNumber(req.get("rollNumber").toString());
            if (req.containsKey("password")) student.setPassword(req.get("password").toString());
            if (req.containsKey("departmentId") && req.get("departmentId") != null) {
                Long deptId = Long.valueOf(req.get("departmentId").toString());
                Department dept = departmentRepository.findById(deptId).orElse(null);
                student.setDepartment(dept);
            }
            return ResponseEntity.ok(studentRepository.save(student));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<?> deleteStudent(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        if (!verifyAdmin(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        try {
            studentRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Student deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error deleting student."));
        }
    }
}
