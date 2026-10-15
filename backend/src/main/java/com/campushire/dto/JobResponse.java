package com.campushire.dto;

import com.campushire.entity.Job;
import com.campushire.entity.JobStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record JobResponse(
        Long id,
        Long companyId,
        String companyName,
        String title,
        String description,
        String location,
        String employmentType,
        BigDecimal salary,
        String eligibleBranch,
        String qualification,
        BigDecimal minimumCgpa,
        Set<String> requiredSkills,
        LocalDate applicationDeadline,
        JobStatus status
) {
    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getCompany().getId(),
                job.getCompany().getName(),
                job.getTitle(),
                job.getDescription(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getSalary(),
                job.getEligibleBranch(),
                job.getQualification(),
                job.getMinimumCgpa(),
                Set.copyOf(job.getRequiredSkills()),
                job.getApplicationDeadline(),
                job.getStatus()
        );
    }
}
