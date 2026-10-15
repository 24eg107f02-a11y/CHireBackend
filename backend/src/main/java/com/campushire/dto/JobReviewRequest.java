package com.campushire.dto;

import jakarta.validation.constraints.Size;

public record JobReviewRequest(@Size(max = 1000) String notes) {
}
