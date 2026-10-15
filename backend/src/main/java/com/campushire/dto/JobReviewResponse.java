package com.campushire.dto;

import com.campushire.entity.Job;

import java.time.Instant;

public record JobReviewResponse(
        JobResponse job,
        Long reviewedByUserId,
        Instant reviewedAt,
        String reviewNotes
) {
    public static JobReviewResponse from(Job job) {
        return new JobReviewResponse(
                JobResponse.from(job),
                job.getReviewedBy() == null ? null : job.getReviewedBy().getId(),
                job.getReviewedAt(),
                job.getReviewNotes()
        );
    }
}
