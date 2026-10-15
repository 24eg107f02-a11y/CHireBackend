package com.campushire.dto;

import java.util.List;

public record JobRecommendationResponse(
        JobResponse job,
        boolean eligible,
        int matchScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> recommendationReasons,
        List<String> eligibilityReasons
) {
    public JobRecommendationResponse {
        matchedSkills = matchedSkills == null ? List.of() : List.copyOf(matchedSkills);
        missingSkills = missingSkills == null ? List.of() : List.copyOf(missingSkills);
        recommendationReasons = recommendationReasons == null ? List.of() : List.copyOf(recommendationReasons);
        eligibilityReasons = eligibilityReasons == null ? List.of() : List.copyOf(eligibilityReasons);
    }
}
