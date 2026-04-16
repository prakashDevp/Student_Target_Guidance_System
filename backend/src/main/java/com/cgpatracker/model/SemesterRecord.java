package com.cgpatracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "semester_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull(message = "Semester number is required")
    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 8, message = "Semester cannot exceed 8")
    @Column(name = "semester_number", nullable = false)
    private Integer semesterNumber;

    @NotNull(message = "GPA is required")
    @DecimalMin(value = "0.0", message = "GPA cannot be negative")
    @DecimalMax(value = "10.0", message = "GPA cannot exceed 10.0")
    @Column(name = "gpa", nullable = false)
    private Double gpa;

    @Column(name = "cgpa_after_semester")
    private Double cgpaAfterSemester;

    @Column(name = "required_gpa_remaining")
    private Double requiredGpaRemaining;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_status")
    private ProgressStatus progressStatus;

    @Column(name = "guidance_message", columnDefinition = "TEXT")
    private String guidanceMessage;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }

    public enum ProgressStatus {
        ON_TRACK,
        AHEAD,
        BEHIND,
        CRITICAL,
        ACHIEVED
    }
}
