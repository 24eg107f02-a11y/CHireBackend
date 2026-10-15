package com.campushire.dto;

import com.campushire.entity.ApplicationStatus;

import java.util.List;
import java.util.Map;

public record PlacementReportResponse(
        long selectedStudents,
        Map<ApplicationStatus, Long> applicationsByStatus,
        List<PlacementReportRow> placementsByCompany
) {
    public PlacementReportResponse {
        applicationsByStatus = Map.copyOf(applicationsByStatus);
        placementsByCompany = List.copyOf(placementsByCompany);
    }
}
