package app.campushire.service;

import app.campushire.entity.Job;
import app.campushire.entity.StudentProfile;
import app.campushire.entity.User;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EligibilityEngine {

    public boolean isEligible(StudentProfile profile, Job job) {
        if (profile == null || job == null) {
            return false;
        }

        if (profile.getCgpa() < job.getMinimumCgpa()) {
            return false;
        }

        if (job.getApplicationDeadline() != null && LocalDate.now().isAfter(job.getApplicationDeadline())) {
            return false;
        }

        String eligibleBranches = normalize(job.getEligibleBranches());
        if (!eligibleBranches.isEmpty() && !containsAny(profile.getBranch(), eligibleBranches)) {
            return false;
        }

        String requiredSkills = normalize(job.getRequiredSkills());
        if (!requiredSkills.isEmpty() && !hasRequiredSkills(profile, requiredSkills)) {
            return false;
        }

        return true;
    }

    public String getEligibilitySummary(StudentProfile profile, Job job) {
        if (profile == null || job == null) {
            return "Missing student or job details";
        }
        if (profile.getCgpa() < job.getMinimumCgpa()) {
            return "CGPA below required threshold";
        }
        if (job.getApplicationDeadline() != null && LocalDate.now().isAfter(job.getApplicationDeadline())) {
            return "Application deadline has passed";
        }
        String eligibleBranches = normalize(job.getEligibleBranches());
        if (!eligibleBranches.isEmpty() && !containsAny(profile.getBranch(), eligibleBranches)) {
            return "Branch not eligible for this role";
        }
        String requiredSkills = normalize(job.getRequiredSkills());
        if (!requiredSkills.isEmpty() && !hasRequiredSkills(profile, requiredSkills)) {
            return "Missing one or more required skills";
        }
        return "Eligible";
    }

    private boolean hasRequiredSkills(StudentProfile profile, String requiredSkills) {
        Set<String> profileSkills = tokenize(profile.getSkills());
        for (String skill : tokenize(requiredSkills)) {
            if (!profileSkills.contains(skill.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    private boolean containsAny(String target, String candidates) {
        Set<String> targetSet = tokenize(target);
        Set<String> candidateSet = tokenize(candidates);
        for (String candidate : candidateSet) {
            if (targetSet.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> tokenize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(value.toLowerCase().split("[,;\\s]+"))
            .filter(token -> !token.isBlank())
            .collect(java.util.stream.Collectors.toSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
