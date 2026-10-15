package com.campushire.controller;

import com.campushire.dto.InterviewResponse;
import com.campushire.dto.JobApplicationResponse;
import com.campushire.dto.JobDriveAssignmentRequest;
import com.campushire.dto.JobReviewRequest;
import com.campushire.dto.JobReviewResponse;
import com.campushire.dto.OfficerDashboardResponse;
import com.campushire.dto.PlacementDriveRequest;
import com.campushire.dto.PlacementDriveResponse;
import com.campushire.dto.PlacementRecordResponse;
import com.campushire.dto.PlacementReportResponse;
import com.campushire.dto.UserAdminResponse;
import com.campushire.dto.UserStatusRequest;
import com.campushire.entity.JobStatus;
import com.campushire.entity.Role;
import com.campushire.security.CampusHireUserDetails;
import com.campushire.service.PlacementOfficerService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/officer")
public class PlacementOfficerController {

    private final PlacementOfficerService placementOfficerService;

    public PlacementOfficerController(PlacementOfficerService placementOfficerService) {
        this.placementOfficerService = placementOfficerService;
    }

    @GetMapping("/dashboard")
    public OfficerDashboardResponse dashboard() {
        return placementOfficerService.getDashboard();
    }

    @GetMapping("/users")
    public List<UserAdminResponse> users(@RequestParam(required = false) Role role) {
        return placementOfficerService.getUsers(role);
    }

    @PatchMapping("/users/{userId}/status")
    public UserAdminResponse updateUserStatus(
            @AuthenticationPrincipal CampusHireUserDetails officer,
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusRequest request
    ) {
        return placementOfficerService.updateUserStatus(officer.getUser().getId(), userId, request);
    }

    @GetMapping("/jobs")
    public List<JobReviewResponse> jobs(@RequestParam(required = false) JobStatus status) {
        return placementOfficerService.getJobs(status);
    }

    @PostMapping("/jobs/{jobId}/approve")
    public JobReviewResponse approveJob(
            @AuthenticationPrincipal CampusHireUserDetails officer,
            @PathVariable Long jobId,
            @Valid @RequestBody(required = false) JobReviewRequest request
    ) {
        return placementOfficerService.reviewJob(officer.getUser().getId(), jobId, true, safeRequest(request));
    }

    @PostMapping("/jobs/{jobId}/reject")
    public JobReviewResponse rejectJob(
            @AuthenticationPrincipal CampusHireUserDetails officer,
            @PathVariable Long jobId,
            @Valid @RequestBody(required = false) JobReviewRequest request
    ) {
        return placementOfficerService.reviewJob(officer.getUser().getId(), jobId, false, safeRequest(request));
    }

    @PutMapping("/jobs/{jobId}/placement-drive")
    public JobReviewResponse assignJobToDrive(
            @AuthenticationPrincipal CampusHireUserDetails officer,
            @PathVariable Long jobId,
            @RequestBody JobDriveAssignmentRequest request
    ) {
        return placementOfficerService.assignJobToDrive(officer.getUser().getId(), jobId, request);
    }

    @GetMapping("/drives")
    public List<PlacementDriveResponse> placementDrives() {
        return placementOfficerService.getPlacementDrives();
    }

    @PostMapping("/drives")
    @ResponseStatus(HttpStatus.CREATED)
    public PlacementDriveResponse createPlacementDrive(
            @AuthenticationPrincipal CampusHireUserDetails officer,
            @Valid @RequestBody PlacementDriveRequest request
    ) {
        return placementOfficerService.createPlacementDrive(officer.getUser().getId(), request);
    }

    @PutMapping("/drives/{driveId}")
    public PlacementDriveResponse updatePlacementDrive(
            @AuthenticationPrincipal CampusHireUserDetails officer,
            @PathVariable Long driveId,
            @Valid @RequestBody PlacementDriveRequest request
    ) {
        return placementOfficerService.updatePlacementDrive(officer.getUser().getId(), driveId, request);
    }

    @DeleteMapping("/drives/{driveId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlacementDrive(
            @AuthenticationPrincipal CampusHireUserDetails officer,
            @PathVariable Long driveId
    ) {
        placementOfficerService.deletePlacementDrive(officer.getUser().getId(), driveId);
    }

    @GetMapping("/reports/placements")
    public PlacementReportResponse placementReport() {
        return placementOfficerService.getPlacementReport();
    }

    @GetMapping("/placements")
    public List<PlacementRecordResponse> placements() {
        return placementOfficerService.getAllPlacements();
    }

    @GetMapping("/analytics")
    public Map<String, Object> analytics() {
        return placementOfficerService.getPlacementAnalytics();
    }

    @GetMapping("/applications")
    public List<JobApplicationResponse> applications() {
        return placementOfficerService.getAllApplications();
    }

    @GetMapping("/interviews")
    public List<InterviewResponse> interviews() {
        return placementOfficerService.getAllInterviews();
    }

    private JobReviewRequest safeRequest(JobReviewRequest request) {
        return request == null ? new JobReviewRequest(null) : request;
    }
}
