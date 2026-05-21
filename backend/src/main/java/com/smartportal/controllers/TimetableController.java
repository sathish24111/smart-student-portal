package com.smartportal.controllers;

import com.smartportal.models.Staff;
import com.smartportal.models.Student;
import com.smartportal.models.Timetable;
import com.smartportal.repositories.StudentRepository;
import com.smartportal.repositories.TimetableRepository;
import com.smartportal.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {

    @Autowired
    private AuthService authService;

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private StudentRepository studentRepository;

    @GetMapping("/student")
    public ResponseEntity<?> getStudentTimetable(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized student access."));
        }

        Optional<Student> studentOpt = studentRepository.findById(sessionOpt.get().getId());
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Student not found."));
        }

        Student student = studentOpt.get();
        if (student.getDepartment() == null) {
            return ResponseEntity.ok(List.of());
        }

        List<Timetable> schedule = timetableRepository.findBySubjectDepartmentId(student.getDepartment().getId());
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/staff")
    public ResponseEntity<?> getStaffTimetable(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized staff access."));
        }

        List<Timetable> schedule = timetableRepository.findByStaffId(sessionOpt.get().getId());
        return ResponseEntity.ok(schedule);
    }
}
