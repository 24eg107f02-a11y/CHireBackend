package app.campushire.service;

import app.campushire.entity.Job;
import app.campushire.entity.StudentProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RecommendationEngine {

    public RecommendationResult score(StudentProfile student, Job job) {
        if (student == null || job == null) {
            throw new IllegalArgumentException("Student and job are required");
        }

        List<String> reasons = new ArrayList<>();
        int score = 0;

        double cgpaDifference = student.getCgpa() - job.getMinimumCgpa();
        if (cgpaDifference >= 0) {
            score += 30;
            reasons.add("CGPA meets minimum requirement");
        } else {
            score += Math.max(0, (int) ((student.getCgpa() / job.getMinimumCgpa()) * 25));
            reasons.add("CGPA is below the recommended threshold");
        }

        Set<String> studentSkills = extractTokens(student.getSkills());
        Set<String> requiredSkills = extractTokens(job.getRequiredSkills());
        int matchedSkills = 0;
        for (String skill : requiredSkills) {
            if (studentSkills.contains(skill.toLowerCase(Locale.ROOT))) {
                matchedSkills++;
            }
        }
        if (!requiredSkills.isEmpty()) {
            score += Math.min(35, (matchedSkills * 35) / Math.max(1, requiredSkills.size()));
            reasons.add("Matched " + matchedSkills + " of " + requiredSkills.size() + " required skills");
        }

        if (job.getEligibleBranches() != null && !job.getEligibleBranches().isBlank()) {
            Set<String> branches = extractTokens(job.getEligibleBranches());
            if (branches.contains(student.getBranch().toLowerCase(Locale.ROOT))) {
                score += 20;
                reasons.add("Branch matches job eligibility");
            } else {
                reasons.add("Branch is not among eligible branches");
            }
        }

        if (student.getAreasOfInterest() != null && !student.getAreasOfInterest().isBlank()) {
            Set<String> interests = extractTokens(student.getAreasOfInterest());
            Set<String> jobWords = extractTokens(job.getDescription() + " " + job.getRequirements());
            int matchCount = 0;
            for (String word : jobWords) {
                if (interests.contains(word)) {
                    matchCount++;
                }
            }
            if (matchCount > 0) {
                score += Math.min(15, matchCount * 5);
                reasons.add("Student interests align with role");
            }
        }

        score = Math.max(0, Math.min(score, 100));
        return new RecommendationResult(score, reasons);
    }

    private Set<String> extractTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return new HashSet<>();
        }
        return Arrays.stream(raw.toLowerCase(Locale.ROOT).split("[,;\\s]+"))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(java.util.stream.Collectors.toSet());
    }

    public static class RecommendationResult {
        private final int score;
        private final List<String> reasons;

        public RecommendationResult(int score, List<String> reasons) {
            this.score = score;
            this.reasons = reasons;
        }

        public int getScore() { return score; }
        public List<String> getReasons() { return reasons; }
    }
}
