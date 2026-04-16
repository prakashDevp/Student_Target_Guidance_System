package com.cgpatracker.dto;

import com.cgpatracker.model.SemesterRecord.ProgressStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

public class SemesterDTO {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {
        @NotNull @Min(1) @Max(8)
        private Integer semesterNumber;

        @NotNull @DecimalMin("0.0") @DecimalMax("10.0")
        private Double gpa;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private Integer semesterNumber;
        private Double gpa;
        private Double cgpaAfterSemester;
        private Double requiredGpaRemaining;
        private ProgressStatus progressStatus;
        private String guidanceMessage;
        private String semesterLabel;
        private LocalDateTime recordedAt;
    }
}
