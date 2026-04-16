package com.cgpatracker.dto;

import com.cgpatracker.model.SemesterRecord.ProgressStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

// ── Student DTOs ──────────────────────────────────────────
public class StudentDTO {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {
        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Email is required")
        @Email
        private String email;

        @NotBlank(message = "Register number is required")
        private String registerNumber;

        @NotBlank(message = "Branch is required")
        private String branch;

        @NotNull @DecimalMin("0.0") @DecimalMax("10.0")
        private Double targetCgpa;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String name;
        private String email;
        private String registerNumber;
        private String branch;
        private Double targetCgpa;
        private Double currentCgpa;
        private Integer currentSemester;
        private Integer semestersCompleted;
        private Integer semestersRemaining;
        private Double requiredGpaPerSemester;
        private String overallStatus;
        private String motivationalMessage;
        private LocalDateTime createdAt;
        private List<SemesterDTO.Response> semesterRecords;
    }
}

// ── Semester DTOs ─────────────────────────────────────────
class SemesterDTOHolder {} // grouping marker
