package com.campushire.dto;

import com.campushire.entity.ApplicationStatus;
import com.campushire.entity.JobApplication;

import java.math.BigDecimal;
import java.time.Instant;

public record JobApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String companyName,
        Long studentProfileId,
        String studentName,
        String studentEmail,
        String studentBranch,
        BigDecimal studentCgpa,
        ApplicationStatus status,
        String recruiterNotes,
        Instant appliedAt,
        Instant updatedAt
) {
    public static JobApplicationResponse from(JobApplication application) {
        var student = application.getStudent();
        var user = student != null ? student.getUser() : null;
        return new JobApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getJob().getCompany().getName(),
                student != null ? student.getId() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                student != null ? student.getBranch() : null,
                student != null ? student.getCgpa() : null,
                application.getStatus(),
                application.getRecruiterNotes(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
