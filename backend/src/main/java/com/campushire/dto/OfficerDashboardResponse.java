package com.campushire.dto;

import java.math.BigDecimal;

public record OfficerDashboardResponse(
        long students,
        long recruiters,
        long placementOfficers,
        long companies,
        long totalJobs,
        long pendingJobs,
        long approvedJobs,
        long rejectedJobs,
        long applications,
        long underReviewApplications,
        long shortlistedApplications,
        long interviewScheduledApplications,
        long selectedApplications,
        long rejectedApplications,
        long scheduledInterviews,
        long placedStudents,
        double placementPercentage,
        BigDecimal highestPackage,
        BigDecimal averagePackage,
        long placementDrives
) {
}
