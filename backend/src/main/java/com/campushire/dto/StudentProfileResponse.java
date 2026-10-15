package com.campushire.dto;

import com.campushire.entity.StudentProfile;
import com.campushire.entity.User;

import java.math.BigDecimal;
import java.time.Instant;

public record StudentProfileResponse(
        Long id,
        Long userId,
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
        Instant updatedAt
) {
    public static StudentProfileResponse from(StudentProfile profile) {
        User user = profile.getUser();
        return new StudentProfileResponse(
                profile.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                profile.getCollegeId(),
                profile.getPhone(),
                profile.getBranch(),
                profile.getQualification(),
                profile.getCgpa(),
                profile.getGraduationYear(),
                profile.getSkills(),
                profile.getInterests(),
                profile.getProjects(),
                profile.getResumeUrl(),
                profile.getUpdatedAt()
        );
    }
}
