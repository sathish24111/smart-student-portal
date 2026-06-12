package com.smartportal.controllers;

import com.smartportal.models.Notification;
import com.smartportal.services.AuthService;
import com.smartportal.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuthService authService;

    @GetMapping
    public ResponseEntity<?> getMyNotifications(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized session"));
        }

        AuthService.UserSession session = sessionOpt.get();
        List<Notification> list = notificationService.getNotifications(session.getRole(), session.getId());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getMyUnreadNotifications(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized session"));
        }

        AuthService.UserSession session = sessionOpt.get();
        List<Notification> list = notificationService.getUnreadNotifications(session.getRole(), session.getId());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized session"));
        }

        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestHeader("Authorization") String token) {
        Optional<AuthService.UserSession> sessionOpt = authService.validateToken(token);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized session"));
        }

        AuthService.UserSession session = sessionOpt.get();
        notificationService.markAllAsRead(session.getRole(), session.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
