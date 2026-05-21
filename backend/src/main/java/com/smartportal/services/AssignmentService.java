package com.smartportal.services;

import com.smartportal.models.Assignment;
import com.smartportal.models.Student;
import com.smartportal.models.Submission;
import com.smartportal.repositories.AssignmentRepository;
import com.smartportal.repositories.StudentRepository;
import com.smartportal.repositories.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public Assignment createAssignment(Assignment assignment, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String uniqueName = "assignment_" + System.currentTimeMillis() + "_" + fileName;
            
            Path uploadPath = Paths.get(uploadDir, "assignments");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path filePath = uploadPath.resolve(uniqueName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            assignment.setFilePath("/uploads/assignments/" + uniqueName);
        }
        return assignmentRepository.save(assignment);
    }

    public Submission submitAssignment(Long assignmentId, Long studentId, MultipartFile file) throws Exception {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new Exception("Assignment not found."));

        if (LocalDateTime.now().isAfter(assignment.getDueDate())) {
            throw new Exception("Submission deadline has passed.");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found."));

        if (file == null || file.isEmpty()) {
            throw new Exception("Please upload a file.");
        }

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String uniqueName = "submit_" + studentId + "_" + assignmentId + "_" + System.currentTimeMillis() + "_" + fileName;

        Path uploadPath = Paths.get(uploadDir, "submissions");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(uniqueName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Check for existing submission to overwrite/update
        Optional<Submission> existingSub = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
        Submission submission;
        if (existingSub.isPresent()) {
            submission = existingSub.get();
            // Delete old file if exists
            try {
                if (submission.getFilePath() != null) {
                    String oldFileRelative = submission.getFilePath().substring(9); // remove /uploads/
                    Files.deleteIfExists(Paths.get(uploadDir, oldFileRelative));
                }
            } catch (Exception ignored) {}
        } else {
            submission = new Submission();
            submission.setAssignment(assignment);
            submission.setStudent(student);
        }

        submission.setSubmittedAt(LocalDateTime.now());
        submission.setFilePath("/uploads/" + Paths.get("submissions", uniqueName).toString().replace("\\", "/"));
        submission.setGrade(null); // Reset grade on resubmission
        submission.setFeedback(null);

        return submissionRepository.save(submission);
    }

    public Submission gradeSubmission(Long submissionId, String grade, String feedback) throws Exception {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new Exception("Submission not found."));
        submission.setGrade(grade);
        submission.setFeedback(feedback);
        return submissionRepository.save(submission);
    }

    public List<Assignment> getAssignmentsForStudent(Long studentId) throws Exception {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found."));
        if (student.getDepartment() == null) {
            return List.of();
        }
        return assignmentRepository.findBySubjectDepartmentId(student.getDepartment().getId());
    }

    public List<Assignment> getAssignmentsForStaff(Long staffId) {
        return assignmentRepository.findByStaffId(staffId);
    }

    public List<Submission> getSubmissionsForStudent(Long studentId) {
        return submissionRepository.findByStudentId(studentId);
    }

    public List<Submission> getSubmissionsForAssignment(Long assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId);
    }
}
