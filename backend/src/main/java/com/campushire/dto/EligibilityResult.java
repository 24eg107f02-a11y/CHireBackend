package com.campushire.dto;

import java.util.List;

public record EligibilityResult(boolean eligible, List<String> reasons) {
    public EligibilityResult {
        reasons = List.copyOf(reasons);
    }
}
