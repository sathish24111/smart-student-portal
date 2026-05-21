package com.smartportal.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "attendance_records",
    uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "student_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "marked_at", nullable = false)
    private LocalDateTime markedAt = LocalDateTime.now();

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, length = 20)
    private String status = "PRESENT"; // PRESENT, LATE, ABSENT

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = true;
}
