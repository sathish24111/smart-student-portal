package com.smartportal.config;

import com.smartportal.models.*;
import com.smartportal.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private FeeRecordRepository feeRecordRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("[DATA INITIALIZER] Resetting sandbox database tables...");

        try {
            // Disable foreign key checks for clean deletion
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");

            // Purge all tables in order
            jdbcTemplate.execute("DELETE FROM audit_logs;");
            jdbcTemplate.execute("DELETE FROM notifications;");
            jdbcTemplate.execute("DELETE FROM leave_requests;");
            jdbcTemplate.execute("DELETE FROM submissions;");
            jdbcTemplate.execute("DELETE FROM assignments;");
            jdbcTemplate.execute("DELETE FROM attendance_records;");
            jdbcTemplate.execute("DELETE FROM attendance_sessions;");
            jdbcTemplate.execute("DELETE FROM face_profiles;");
            jdbcTemplate.execute("DELETE FROM timetable;");
            jdbcTemplate.execute("DELETE FROM fee_records;");
            jdbcTemplate.execute("DELETE FROM students;");
            jdbcTemplate.execute("DELETE FROM staff;");
            jdbcTemplate.execute("DELETE FROM admins;");
            jdbcTemplate.execute("DELETE FROM subjects;");
            jdbcTemplate.execute("DELETE FROM departments;");

            // Re-enable foreign key checks
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;");
            System.out.println("[DATA INITIALIZER] Database tables purged successfully.");
        } catch (Exception e) {
            System.err.println("[DATA INITIALIZER] Failed to purge tables: " + e.getMessage());
        }

        System.out.println("[DATA INITIALIZER] Seeding fresh sandbox data...");

        // 1. Departments
        Department csDept = departmentRepository.save(new Department(null, "Computer Science & Engineering", "CSE"));
        Department itDept = departmentRepository.save(new Department(null, "Information Technology", "IT"));
        Department eceDept = departmentRepository.save(new Department(null, "Electronics & Communication", "ECE"));

        // 2. Subjects
        Subject dsa = subjectRepository.save(new Subject(null, "Data Structures & Algorithms", "CSE-201", csDept));
        Subject os = subjectRepository.save(new Subject(null, "Operating Systems", "CSE-202", csDept));
        Subject dbms = subjectRepository.save(new Subject(null, "Database Management Systems", "CSE-203", csDept));
        Subject wt = subjectRepository.save(new Subject(null, "Web Technologies", "IT-301", itDept));
        Subject se = subjectRepository.save(new Subject(null, "Software Engineering", "IT-302", itDept));

        // 3. Admin Account
        adminRepository.save(new Admin(null, "admin_nexus", "admin123", "admin@portal.edu", "Dr. Richard Winters (System Admin)"));

        // 4. Staff Accounts
        Staff profSmith = staffRepository.save(new Staff(null, "prof_smith", "staff123", "smith@portal.edu", "Prof. Albert Smith", "+1-555-0199", csDept));
        Staff profDavis = staffRepository.save(new Staff(null, "prof_davis", "staff123", "davis@portal.edu", "Prof. Sarah Davis", "+1-555-0188", itDept));

        // 5. Student Accounts
        Student john = studentRepository.save(new Student(null, "student_john", "student123", "john@student.edu", "John Doe", "CSE-2026-001", csDept, 100.0));
        Student jane = studentRepository.save(new Student(null, "student_jane", "student123", "jane@student.edu", "Jane Miller", "CSE-2026-002", csDept, 100.0));
        Student bob = studentRepository.save(new Student(null, "student_bob", "student123", "bob@student.edu", "Bob Wilson", "IT-2026-001", itDept, 100.0));

        // 5b. Fee Records
        feeRecordRepository.save(new FeeRecord(null, john, 5000.0, 5000.0, "PAID", java.time.LocalDate.now().minusMonths(1), null));
        feeRecordRepository.save(new FeeRecord(null, jane, 5000.0, 2000.0, "PENDING", java.time.LocalDate.now().plusMonths(1), null));
        feeRecordRepository.save(new FeeRecord(null, bob, 6000.0, 1500.0, "OVERDUE", java.time.LocalDate.now().minusWeeks(1), null));

        // 6. Timetable
        timetableRepository.save(new Timetable(null, dsa, profSmith, "MONDAY", LocalTime.of(9, 0), LocalTime.of(10, 30), "Room LH-101"));
        timetableRepository.save(new Timetable(null, os, profSmith, "TUESDAY", LocalTime.of(11, 0), LocalTime.of(12, 30), "Room LH-102"));
        timetableRepository.save(new Timetable(null, dbms, profSmith, "WEDNESDAY", LocalTime.of(9, 0), LocalTime.of(10, 30), "Room LH-101"));
        timetableRepository.save(new Timetable(null, wt, profDavis, "THURSDAY", LocalTime.of(14, 0), LocalTime.of(15, 30), "Room LH-201"));
        timetableRepository.save(new Timetable(null, se, profDavis, "FRIDAY", LocalTime.of(14, 0), LocalTime.of(15, 30), "Room LH-202"));

        System.out.println("[DATA INITIALIZER] Seeding complete! Sandbox database is ready.");
    }
}
