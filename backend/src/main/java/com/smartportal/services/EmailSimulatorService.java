package com.smartportal.services;

import com.smartportal.models.Student;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EmailSimulatorService {

    private final List<MockEmail> emailLog = new CopyOnWriteArrayList<>();

    public static class MockEmail {
        private final String to;
        private final String subject;
        private final String body;
        private final LocalDateTime timestamp;

        public MockEmail(String to, String subject, String body) {
            this.to = to;
            this.subject = subject;
            this.body = body;
            this.timestamp = LocalDateTime.now();
        }

        public String getTo() { return to; }
        public String getSubject() { return subject; }
        public String getBody() { return body; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public void sendLowAttendanceAlert(Student student, double attendancePercentage) {
        String to = student.getEmail();
        String subject = "🚨 WARNING: Low Attendance Alert - " + student.getName();
        String body = String.format(
                "Dear %s (%s),\n\n" +
                "Your current attendance in the Student Management Portal is **%.1f%%**, which is below the minimum required threshold of **75.0%%**.\n\n" +
                "Please make sure to attend your upcoming lectures and mark your attendance using the Geolocation Portal inside the classroom to avoid disciplinary actions or being barred from final exams.\n\n" +
                "Regards,\n" +
                "Academic Administration Office",
                student.getName(), student.getRollNumber(), attendancePercentage
        );

        System.out.println("=========================================================================");
        System.out.println("[EMAIL SIMULATOR] Outbound email sent successfully!");
        System.out.println("TO: " + to);
        System.out.println("SUBJECT: " + subject);
        System.out.println("BODY:\n" + body);
        System.out.println("=========================================================================");

        emailLog.add(new MockEmail(to, subject, body));
    }

    public void sendWelcomeEmail(String role, String name, String email, String username) {
        String subject = "🎓 Welcome to Smart Student Management Portal!";
        String body = String.format(
                "Welcome %s,\n\n" +
                "Your account has been successfully created as a **%s** on our modern Smart Student Management Portal.\n\n" +
                "Here are your login details:\n" +
                "- Username: %s\n" +
                "- Portal URL: http://localhost:8080\n\n" +
                "Please log in and update your profile settings. Students can mark classroom attendance using their mobile GPS location through this portal.\n\n" +
                "Best Regards,\n" +
                "IT Helpdesk Services",
                name, role, username
        );

        System.out.println("=========================================================================");
        System.out.println("[EMAIL SIMULATOR] Outbound email sent successfully!");
        System.out.println("TO: " + email);
        System.out.println("SUBJECT: " + subject);
        System.out.println("BODY:\n" + body);
        System.out.println("=========================================================================");

        emailLog.add(new MockEmail(email, subject, body));
    }

    public List<MockEmail> getEmailsForUser(String email) {
        List<MockEmail> userEmails = new ArrayList<>();
        for (MockEmail mail : emailLog) {
            if (mail.getTo().equalsIgnoreCase(email)) {
                userEmails.add(mail);
            }
        }
        return userEmails;
    }

    public List<MockEmail> getAllEmails() {
        return emailLog;
    }
}
