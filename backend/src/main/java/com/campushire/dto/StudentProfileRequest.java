package com.campushire.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StudentProfileRequest(
        @Size(max = 100) String fullName,
        @Size(max = 50) String collegeId,
        @Size(max = 30) String phone,
        @Size(max = 100) String branch,
        @Size(max = 100) String qualification,
        @DecimalMin("0.00") @DecimalMax("10.00") BigDecimal cgpa,
        @Min(2000) @Max(2100) Integer graduationYear,
        @Size(max = 2000) String skills,
        @Size(max = 2000) String interests,
        @Size(max = 4000) String projects,
        @Size(max = 2048) String resumeUrl
) {
}
