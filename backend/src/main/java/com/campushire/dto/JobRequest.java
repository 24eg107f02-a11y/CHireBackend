package com.campushire.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record JobRequest(
        @NotNull Long companyId,
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 8000) String description,
        @Size(max = 120) String location,
        @Size(max = 80) String employmentType,
        @DecimalMin("0.00") BigDecimal salary,
        @Size(max = 100) String eligibleBranch,
        @Size(max = 100) String qualification,
        @DecimalMin("0.00") @DecimalMax("10.00") BigDecimal minimumCgpa,
        Set<@NotBlank @Size(max = 80) String> requiredSkills,
        @NotNull @FutureOrPresent LocalDate applicationDeadline
) {
}
