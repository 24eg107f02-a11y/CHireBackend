package com.campushire.dto;

import com.campushire.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationStatusRequest(
        @NotNull ApplicationStatus status,
        @Size(max = 2000) String recruiterNotes
) {
}
