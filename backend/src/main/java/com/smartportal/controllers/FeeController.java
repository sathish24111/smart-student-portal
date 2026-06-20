package com.smartportal.controllers;

import com.smartportal.models.FeeRecord;
import com.smartportal.models.Student;
import com.smartportal.repositories.FeeRecordRepository;
import com.smartportal.services.AuthService;
import com.smartportal.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/fees")
public class FeeController {

    @Autowired
    private FeeRecordRepository feeRecordRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private NotificationService notificationService;

    // 1. Get stats for Admin/Staff
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || (!"ADMIN".equalsIgnoreCase(sessionOpt.get().getRole()) && !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized access."));
        }

        List<FeeRecord> records = feeRecordRepository.findAll();
        double totalCollection = records.stream().mapToDouble(FeeRecord::getPaidAmount).sum();
        double pendingFees = records.stream().mapToDouble(r -> Math.max(0.0, r.getTotalFee() - r.getPaidAmount())).sum();
        long overdueAlerts = records.stream().filter(r -> "OVERDUE".equalsIgnoreCase(r.getStatus())).count();

        return ResponseEntity.ok(Map.of(
                "totalCollection", totalCollection,
                "pendingFees", pendingFees,
                "overdueAlerts", overdueAlerts
        ));
    }

    // 2. Get records list for Admin/Staff
    @GetMapping("/records")
    public ResponseEntity<?> getRecords(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || (!"ADMIN".equalsIgnoreCase(sessionOpt.get().getRole()) && !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized access."));
        }

        return ResponseEntity.ok(feeRecordRepository.findAll());
    }

    // 3. Get personal fee status for Students
    @GetMapping("/my-status")
    public ResponseEntity<?> getMyStatus(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || !"STUDENT".equalsIgnoreCase(sessionOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized student access."));
        }

        List<FeeRecord> myRecords = feeRecordRepository.findByStudentId(sessionOpt.get().getId());
        return ResponseEntity.ok(myRecords);
    }

    // 4. Pay fee (Update record)
    @PostMapping("/{id}/pay")
    public ResponseEntity<?> collectFee(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Double> payload) {
        
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || (!"ADMIN".equalsIgnoreCase(sessionOpt.get().getRole()) && !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized access."));
        }

        Optional<FeeRecord> recordOpt = feeRecordRepository.findById(id);
        if (recordOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Fee record not found."));
        }

        FeeRecord record = recordOpt.get();
        double amountToPay = payload.containsKey("amount") ? payload.get("amount") : 0.0;
        
        double newPaid = record.getPaidAmount() + amountToPay;
        if (newPaid > record.getTotalFee()) {
            newPaid = record.getTotalFee();
        }
        record.setPaidAmount(newPaid);
        
        if (newPaid >= record.getTotalFee()) {
            record.setStatus("PAID");
        } else if (LocalDate.now().isAfter(record.getDueDate())) {
            record.setStatus("OVERDUE");
        } else {
            record.setStatus("PENDING");
        }

        FeeRecord saved = feeRecordRepository.save(record);
        
        // Notify student of the transaction
        notificationService.sendNotification("STUDENT", record.getStudent().getId(), "Fee Payment Received", 
                String.format("A payment of $%.2f was recorded for your tuition fees. New paid balance: $%.2f", amountToPay, record.getPaidAmount()));

        return ResponseEntity.ok(Map.of(
                "message", "Payment recorded successfully!",
                "record", saved
        ));
    }

    // 5. Send overdue alert (Notification trigger)
    @PostMapping("/{id}/alert")
    public ResponseEntity<?> sendOverdueAlert(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {

        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty() || (!"ADMIN".equalsIgnoreCase(sessionOpt.get().getRole()) && !"STAFF".equalsIgnoreCase(sessionOpt.get().getRole()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized access."));
        }

        Optional<FeeRecord> recordOpt = feeRecordRepository.findById(id);
        if (recordOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Fee record not found."));
        }

        FeeRecord record = recordOpt.get();
        
        // Update status to OVERDUE if pending and date has passed
        if ("PENDING".equalsIgnoreCase(record.getStatus()) && LocalDate.now().isAfter(record.getDueDate())) {
            record.setStatus("OVERDUE");
            feeRecordRepository.save(record);
        }

        notificationService.sendNotification("STUDENT", record.getStudent().getId(), "URGENT: Tuition Fees Overdue", 
                String.format("Your tuition fee payment of $%.2f is overdue. Please settle this amount immediately to avoid registry lock.", 
                        record.getTotalFee() - record.getPaidAmount()));

        return ResponseEntity.ok(Map.of("message", "Overdue warning notification dispatched to student."));
    }
}
