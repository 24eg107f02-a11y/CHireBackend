package com.campushire.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record InterviewRequest(
        @NotNull @Future Instant scheduledAt,
        @Size(max = 50) String interviewType,
        @Size(max = 50) String interviewMode,
        @Size(max = 100) String round,
        @Size(max = 500) String locationOrMeetingUrl,
        @Size(max = 30) String status,
        @Size(max = 2000) String notes
) {
}
