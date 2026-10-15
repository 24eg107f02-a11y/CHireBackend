package com.campushire.dto;

import com.campushire.entity.ApplicationStatus;
import com.campushire.entity.ApplicationStatusHistory;

import java.time.Instant;

public record ApplicationStatusHistoryResponse(
        Long id,
        Long applicationId,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        String changedByName,
        String comments,
        Instant changedAt
) {
    public static ApplicationStatusHistoryResponse from(ApplicationStatusHistory history) {
        return new ApplicationStatusHistoryResponse(
                history.getId(),
                history.getApplication().getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy() != null ? history.getChangedBy().getFullName() : "System",
                history.getComments(),
                history.getCreatedAt()
        );
    }
}
