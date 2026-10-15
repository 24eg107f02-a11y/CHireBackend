package com.campushire.dto;

import com.campushire.entity.PlacementDrive;

import java.time.LocalDate;

public record PlacementDriveResponse(
        Long id,
        Long createdByUserId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String venue
) {
    public static PlacementDriveResponse from(PlacementDrive drive) {
        return new PlacementDriveResponse(
                drive.getId(),
                drive.getCreatedBy().getId(),
                drive.getName(),
                drive.getDescription(),
                drive.getStartDate(),
                drive.getEndDate(),
                drive.getVenue()
        );
    }
}
