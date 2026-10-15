package com.campushire.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PlacementDriveRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 120) String venue
) {
}
