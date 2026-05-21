package com.smartportal.controllers;

import com.smartportal.models.Assignment;
import com.smartportal.models.Staff;
import com.smartportal.models.Subject;
import com.smartportal.models.Submission;
import com.smartportal.services.AssignmentService;
import com.smartportal.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AssignmentService assignmentService;

    // 1. Staff creates an assignment with custom materials
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> createAssignment(
            @RequestHeader("Authorization") String token,
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("dueDate") String dueDateStr,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized staff access."));
        }

        try {
            Assignment assignment = new Assignment();
            assignment.setTitle(title);
            assignment.setDescription(description);
            assignment.setDueDate(LocalDateTime.parse(dueDateStr));
            
            Subject subject = new Subject();
            subject.setId(subjectId);
            assignment.setSubject(subject);
            
            Staff staff = new Staff();
            staff.setId(sessionOpt.get().getId());
            assignment.setStaff(staff);

            Assignment created = assignmentService.createAssignment(assignment, file);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error posting assignment: " + e.getMessage()));
        }
    }

    // 2. Student lists assignments for their department
    @GetMapping("/student")
    public ResponseEntity<?> getStudentAssignments(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized student access."));
        }

        try {
            List<Assignment> assignments = assignmentService.getAssignmentsForStudent(sessionOpt.get().getId());
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 3. Student uploads assignment file (PDF/DOC)
    @PostMapping(value = "/submit", consumes = {"multipart/form-data"})
    public ResponseEntity<?> submitAssignment(
            @RequestHeader("Authorization") String token,
            @RequestParam("assignmentId") Long assignmentId,
            @RequestParam("file") MultipartFile file) {

        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized student access."));
        }

        try {
            Submission submission = assignmentService.submitAssignment(assignmentId, sessionOpt.get().getId(), file);
            return ResponseEntity.ok(Map.of(
                    "message", "Assignment submitted successfully!",
                    "submission", submission
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 4. Student gets all their submissions
    @GetMapping("/my-submissions")
    public ResponseEntity<?> getMySubmissions(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized student access."));
        }

        List<Submission> submissions = assignmentService.getSubmissionsForStudent(sessionOpt.get().getId());
        return ResponseEntity.ok(submissions);
    }

    // 5. Staff lists submissions for their posted assignment
    @GetMapping("/submissions")
    public ResponseEntity<?> getSubmissions(
            @RequestHeader("Authorization") String token,
            @RequestParam("assignmentId") Long assignmentId) {

        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized staff access."));
        }

        List<Submission> submissions = assignmentService.getSubmissionsForAssignment(assignmentId);
        return ResponseEntity.ok(submissions);
    }

    // 6. Staff uploads grade & reviews submissions
    @PostMapping("/grade")
    public ResponseEntity<?> gradeSubmission(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {

        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized staff access."));
        }

        try {
            Long submissionId = Long.valueOf(request.get("submissionId").toString());
            String grade = request.get("grade").toString();
            String feedback = request.containsKey("feedback") ? request.get("feedback").toString() : "";

            Submission graded = assignmentService.gradeSubmission(submissionId, grade, feedback);
            return ResponseEntity.ok(Map.of(
                    "message", "Submission graded successfully!",
                    "submission", graded
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 7. Staff lists assignments they created
    @GetMapping("/staff")
    public ResponseEntity<?> getStaffAssignments(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized staff access."));
        }
        
        List<Assignment> assignments = assignmentService.getAssignmentsForStaff(sessionOpt.get().getId());
        return ResponseEntity.ok(assignments);
    }
}
