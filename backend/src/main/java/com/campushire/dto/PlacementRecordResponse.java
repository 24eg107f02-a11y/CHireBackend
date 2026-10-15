package com.campushire.dto;

import com.campushire.entity.PlacementRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record PlacementRecordResponse(
        Long id,
        Long studentId,
        String studentName,
        String studentEmail,
        String studentBranch,
        Long companyId,
        String companyName,
        Long jobId,
        String jobTitle,
        BigDecimal packageCtc,
        LocalDate selectionDate,
        String placementStatus,
        Instant createdAt
) {
    public static PlacementRecordResponse from(PlacementRecord record) {
        var student = record.getStudent();
        var user = student.getUser();
        var company = record.getCompany();
        var job = record.getJob();

        return new PlacementRecordResponse(
                record.getId(),
                student.getId(),
                user.getFullName(),
                user.getEmail(),
                student.getBranch(),
                company.getId(),
                company.getName(),
                job.getId(),
                job.getTitle(),
                record.getPackageCtc(),
                record.getSelectionDate(),
                record.getPlacementStatus(),
                record.getCreatedAt()
        );
    }
}
