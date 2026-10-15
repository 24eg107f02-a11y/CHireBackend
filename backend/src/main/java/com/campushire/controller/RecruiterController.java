package com.campushire.controller;

import com.campushire.dto.ApplicationStatusRequest;
import com.campushire.dto.CompanyRequest;
import com.campushire.dto.CompanyResponse;
import com.campushire.dto.InterviewRequest;
import com.campushire.dto.InterviewResponse;
import com.campushire.dto.JobApplicationResponse;
import com.campushire.dto.JobRequest;
import com.campushire.dto.JobResponse;
import com.campushire.dto.RecruiterApplicantResponse;
import com.campushire.security.CampusHireUserDetails;
import com.campushire.service.RecruiterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recruiters")
public class RecruiterController {

    private final RecruiterService recruiterService;

    public RecruiterController(RecruiterService recruiterService) {
        this.recruiterService = recruiterService;
    }

    @GetMapping("/companies")
    public List<CompanyResponse> getCompanies(@AuthenticationPrincipal CampusHireUserDetails user) {
        return recruiterService.getCompanies(user.getUser().getId());
    }

    @PostMapping("/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse createCompany(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @Valid @RequestBody CompanyRequest request
    ) {
        return recruiterService.createCompany(user.getUser().getId(), request);
    }

    @PutMapping("/companies/{companyId}")
    public CompanyResponse updateCompany(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @PathVariable Long companyId,
            @Valid @RequestBody CompanyRequest request
    ) {
        return recruiterService.updateCompany(user.getUser().getId(), companyId, request);
    }

    @DeleteMapping("/companies/{companyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompany(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @PathVariable Long companyId
    ) {
        recruiterService.deleteCompany(user.getUser().getId(), companyId);
    }

    @GetMapping("/jobs")
    public List<JobResponse> getJobs(@AuthenticationPrincipal CampusHireUserDetails user) {
        return recruiterService.getJobs(user.getUser().getId());
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse createJob(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @Valid @RequestBody JobRequest request
    ) {
        return recruiterService.createJob(user.getUser().getId(), request);
    }

    @PutMapping("/jobs/{jobId}")
    public JobResponse updateJob(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @PathVariable Long jobId,
            @Valid @RequestBody JobRequest request
    ) {
        return recruiterService.updateJob(user.getUser().getId(), jobId, request);
    }

    @PostMapping("/jobs/{jobId}/close")
    public JobResponse closeJob(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @PathVariable Long jobId
    ) {
        return recruiterService.closeJob(user.getUser().getId(), jobId);
    }

    @DeleteMapping("/jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @PathVariable Long jobId
    ) {
        recruiterService.deleteJob(user.getUser().getId(), jobId);
    }

    @GetMapping("/jobs/{jobId}/applications")
    public List<RecruiterApplicantResponse> getApplicants(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @PathVariable Long jobId
    ) {
        return recruiterService.getApplicants(user.getUser().getId(), jobId);
    }

    @PatchMapping("/applications/{applicationId}/status")
    public JobApplicationResponse updateApplicationStatus(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationStatusRequest request
    ) {
        return recruiterService.updateApplicationStatus(user.getUser().getId(), applicationId, request);
    }

    @PostMapping("/applications/{applicationId}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewResponse scheduleInterview(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @PathVariable Long applicationId,
            @Valid @RequestBody InterviewRequest request
    ) {
        return recruiterService.scheduleInterview(user.getUser().getId(), applicationId, request);
    }
}
