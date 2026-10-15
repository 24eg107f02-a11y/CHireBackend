package com.campushire.dto;

import com.campushire.entity.Company;

public record CompanyResponse(
        Long id,
        Long recruiterProfileId,
        String name,
        String description,
        String website,
        String headquarters
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getRecruiter().getId(),
                company.getName(),
                company.getDescription(),
                company.getWebsite(),
                company.getHeadquarters()
        );
    }
}
