package com.campushire.controller;

import com.campushire.dto.ApplicationStatusHistoryResponse;
import com.campushire.dto.InterviewResponse;
import com.campushire.dto.JobApplicationResponse;
import com.campushire.dto.JobRecommendationResponse;
import com.campushire.dto.PlacementRecordResponse;
import com.campushire.dto.StudentProfileRequest;
import com.campushire.dto.StudentProfileResponse;
import com.campushire.security.CampusHireUserDetails;
import com.campushire.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/profile")
    public StudentProfileResponse getProfile(@AuthenticationPrincipal CampusHireUserDetails user) {
        return studentService.getProfile(user.getUser().getId());
    }

    @PutMapping("/profile")
    public StudentProfileResponse updateProfile(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @Valid @RequestBody StudentProfileRequest request
    ) {
        return studentService.updateProfile(user.getUser().getId(), request);
    }

    @PostMapping("/jobs/{jobId}/applications")
    public JobApplicationResponse apply(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @PathVariable Long jobId
    ) {
        return studentService.apply(user.getUser().getId(), jobId);
    }

    @GetMapping("/applications")
    public List<JobApplicationResponse> getApplications(@AuthenticationPrincipal CampusHireUserDetails user) {
        return studentService.getApplications(user.getUser().getId());
    }

    @GetMapping("/applications/{applicationId}/history")
    public List<ApplicationStatusHistoryResponse> getApplicationHistory(
            @AuthenticationPrincipal CampusHireUserDetails user,
            @PathVariable Long applicationId
    ) {
        return studentService.getApplicationHistory(user.getUser().getId(), applicationId);
    }

    @GetMapping("/interviews")
    public List<InterviewResponse> getInterviews(@AuthenticationPrincipal CampusHireUserDetails user) {
        return studentService.getInterviews(user.getUser().getId());
    }

    @GetMapping("/placements")
    public List<PlacementRecordResponse> getPlacements(@AuthenticationPrincipal CampusHireUserDetails user) {
        return studentService.getPlacements(user.getUser().getId());
    }

    @GetMapping("/recommendations")
    public List<JobRecommendationResponse> getRecommendations(@AuthenticationPrincipal CampusHireUserDetails user) {
        return studentService.getRecommendations(user.getUser().getId());
    }
}
