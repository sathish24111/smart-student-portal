package com.smartportal.controllers;

import com.smartportal.services.AuthService;
import com.smartportal.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private AuthService authService;

    @GetMapping("/export")
    public ResponseEntity<?> exportReport(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "csv") String format) {

        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized session"));
        }

        AuthService.UserSession session = sessionOpt.get();
        Long queryStudentId = studentId;
        Long queryStaffId = null;

        // Security check: Students can only export their own logs
        if ("STUDENT".equalsIgnoreCase(session.getRole())) {
            queryStudentId = session.getId();
        } else if ("STAFF".equalsIgnoreCase(session.getRole())) {
            if (queryStudentId == null) {
                queryStaffId = session.getId(); // Staff default to their own classes
            }
        }

        LocalDateTime start = startDate != null ? 
                LocalDate.parse(startDate).atStartOfDay() : 
                LocalDate.now().minusDays(30).atStartOfDay();

        LocalDateTime end = endDate != null ? 
                LocalDate.parse(endDate).atTime(LocalTime.MAX) : 
                LocalDate.now().atTime(LocalTime.MAX);

        byte[] reportContent;
        String filename;
        MediaType mediaType;

        try {
            if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
                reportContent = reportService.generateExcelReport(queryStudentId, queryStaffId, start, end);
                filename = "attendance_report_" + LocalDate.now() + ".xlsx";
                mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else if ("pdf".equalsIgnoreCase(format)) {
                reportContent = reportService.generatePdfReport(queryStudentId, queryStaffId, start, end);
                filename = "attendance_report_" + LocalDate.now() + ".pdf";
                mediaType = MediaType.APPLICATION_PDF;
            } else {
                reportContent = reportService.generateCsvReport(queryStudentId, queryStaffId, start, end);
                filename = "attendance_report_" + LocalDate.now() + ".csv";
                mediaType = MediaType.parseMediaType("text/csv");
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to generate report file: " + e.getMessage()));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(reportContent);
    }
}
