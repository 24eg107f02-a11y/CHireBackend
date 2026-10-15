package com.campushire.service;

import com.campushire.dto.EligibilityResult;
import com.campushire.entity.Job;
import com.campushire.entity.JobStatus;
import com.campushire.entity.StudentProfile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobEligibilityService {

    public EligibilityResult evaluate(StudentProfile student, Job job) {
        List<String> reasons = new ArrayList<>();

        // Check Job Status
        if (job.getStatus() != JobStatus.APPROVED) {
            reasons.add("Job status is " + job.getStatus() + "; applications are only open for APPROVED jobs.");
        }

        // Check Application Deadline
        if (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(LocalDate.now())) {
            reasons.add("Application deadline passed on " + job.getApplicationDeadline() + ".");
        }

        // Check Eligible Branch
        String requiredBranch = normalize(job.getEligibleBranch());
        String studentBranch = normalize(student.getBranch());
        if (requiredBranch != null && !requiredBranch.equalsIgnoreCase("all") && !requiredBranch.equalsIgnoreCase("any")) {
            if (studentBranch == null) {
                reasons.add("Add your branch to your profile to check this job requirement (" + job.getEligibleBranch() + ").");
            } else if (!requiredBranch.equalsIgnoreCase(studentBranch)) {
                reasons.add("This job is limited to the " + job.getEligibleBranch() + " branch.");
            }
        }

        // Check Qualification / Degree
        String requiredQualification = normalize(job.getQualification());
        String studentQualification = normalize(student.getQualification());
        if (requiredQualification != null && !requiredQualification.equalsIgnoreCase("all") && !requiredQualification.equalsIgnoreCase("any")) {
            if (studentQualification == null) {
                reasons.add("Add your qualification (" + job.getQualification() + ") to your profile to check this requirement.");
            } else if (!requiredQualification.equalsIgnoreCase(studentQualification) && !studentQualification.contains(requiredQualification)) {
                reasons.add("This job requires " + job.getQualification() + " qualification.");
            }
        }

        // Check Minimum CGPA
        if (job.getMinimumCgpa() != null) {
            if (student.getCgpa() == null) {
                reasons.add("Add your CGPA to your profile to check this job requirement (minimum " + job.getMinimumCgpa() + ").");
            } else if (student.getCgpa().compareTo(job.getMinimumCgpa()) < 0) {
                reasons.add("This job requires a minimum CGPA of at least " + job.getMinimumCgpa() + ".");
            }
        }

        // Check Mandatory Technical Skills
        List<String> missing = missingSkills(student, job);
        if (!missing.isEmpty()) {
            reasons.add("Add these required skills to your profile: " + String.join(", ", missing) + ".");
        }

        return new EligibilityResult(reasons.isEmpty(), reasons);
    }

    public List<String> matchingSkills(StudentProfile student, Job job) {
        Set<String> studentSkills = parseSkills(student.getSkills());
        if (job.getRequiredSkills() == null || job.getRequiredSkills().isEmpty()) {
            return List.of();
        }
        return job.getRequiredSkills().stream()
                .filter(skill -> studentSkills.contains(normalize(skill)))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> missingSkills(StudentProfile student, Job job) {
        Set<String> studentSkills = parseSkills(student.getSkills());
        if (job.getRequiredSkills() == null || job.getRequiredSkills().isEmpty()) {
            return List.of();
        }
        return job.getRequiredSkills().stream()
                .filter(skill -> !studentSkills.contains(normalize(skill)))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public Set<String> parseSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(skills.split("[,;\\r\\n]+"))
                .map(this::normalize)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
