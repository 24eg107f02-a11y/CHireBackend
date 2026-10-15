package com.campushire.dto;

import com.campushire.entity.JobApplication;

import java.math.BigDecimal;
import java.time.Instant;

public record RecruiterApplicantResponse(
        Long applicationId,
        Long studentProfileId,
        String fullName,
        String email,
        String collegeId,
        String phone,
        String branch,
        String qualification,
        BigDecimal cgpa,
        Integer graduationYear,
        String skills,
        String interests,
        String projects,
        String resumeUrl,
        String status,
        String recruiterNotes,
        Instant appliedAt
) {
    public static RecruiterApplicantResponse from(JobApplication application) {
        var student = application.getStudent();
        var user = student.getUser();
        return new RecruiterApplicantResponse(
                application.getId(),
                student.getId(),
                user.getFullName(),
                user.getEmail(),
                student.getCollegeId(),
                student.getPhone(),
                student.getBranch(),
                student.getQualification(),
                student.getCgpa(),
                student.getGraduationYear(),
                student.getSkills(),
                student.getInterests(),
                student.getProjects(),
                student.getResumeUrl(),
                application.getStatus().name(),
                application.getRecruiterNotes(),
                application.getCreatedAt()
        );
    }
}
