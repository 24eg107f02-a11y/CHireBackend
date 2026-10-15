package com.campushire.dto;

import com.campushire.entity.Interview;

import java.time.Instant;

public record InterviewResponse(
        Long id,
        Long applicationId,
        Long jobId,
        String jobTitle,
        String companyName,
        String studentName,
        Instant scheduledAt,
        String interviewType,
        String interviewMode,
        String round,
        String locationOrMeetingUrl,
        String status,
        String notes
) {
    public static InterviewResponse from(Interview interview) {
        var app = interview.getApplication();
        var job = app.getJob();
        var student = app.getStudent();
        return new InterviewResponse(
                interview.getId(),
                app.getId(),
                job.getId(),
                job.getTitle(),
                job.getCompany().getName(),
                student.getUser().getFullName(),
                interview.getScheduledAt(),
                interview.getInterviewType(),
                interview.getInterviewMode(),
                interview.getRound(),
                interview.getLocationOrMeetingUrl(),
                interview.getStatus(),
                interview.getNotes()
        );
    }
}
