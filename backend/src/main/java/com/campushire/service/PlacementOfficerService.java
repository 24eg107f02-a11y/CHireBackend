package com.campushire.service;

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
import com.campushire.entity.ApplicationStatus;
import com.campushire.entity.Job;
import com.campushire.entity.JobStatus;
import com.campushire.entity.PlacementDrive;
import com.campushire.entity.Role;
import com.campushire.entity.User;
import com.campushire.repository.CompanyRepository;
import com.campushire.repository.InterviewRepository;
import com.campushire.repository.JobApplicationRepository;
import com.campushire.repository.JobRepository;
import com.campushire.repository.PlacementDriveRepository;
import com.campushire.repository.PlacementRecordRepository;
import com.campushire.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlacementOfficerService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final InterviewRepository interviewRepository;
    private final PlacementRecordRepository placementRecordRepository;
    private final NotificationService notificationService;

    public PlacementOfficerService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            JobRepository jobRepository,
            JobApplicationRepository jobApplicationRepository,
            PlacementDriveRepository placementDriveRepository,
            InterviewRepository interviewRepository,
            PlacementRecordRepository placementRecordRepository,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.interviewRepository = interviewRepository;
        this.placementRecordRepository = placementRecordRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public OfficerDashboardResponse getDashboard() {
        long studentCount = userRepository.countByRole(Role.STUDENT);
        long placedCount = placementRecordRepository.countDistinctPlacedStudents();
        double placementPercentage = studentCount > 0
                ? BigDecimal.valueOf(placedCount * 100.0 / studentCount)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return new OfficerDashboardResponse(
                studentCount,
                userRepository.countByRole(Role.RECRUITER),
                userRepository.countByRole(Role.PLACEMENT_OFFICER),
                companyRepository.count(),
                jobRepository.count(),
                jobRepository.countByStatus(JobStatus.PENDING_APPROVAL),
                jobRepository.countByStatus(JobStatus.APPROVED),
                jobRepository.countByStatus(JobStatus.REJECTED),
                jobApplicationRepository.count(),
                jobApplicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW),
                jobApplicationRepository.countByStatus(ApplicationStatus.SHORTLISTED),
                jobApplicationRepository.countByStatus(ApplicationStatus.INTERVIEW_SCHEDULED),
                jobApplicationRepository.countByStatus(ApplicationStatus.SELECTED),
                jobApplicationRepository.countByStatus(ApplicationStatus.REJECTED),
                interviewRepository.count(),
                placedCount,
                placementPercentage,
                placementRecordRepository.findHighestPackage(),
                placementRecordRepository.findAveragePackage(),
                placementDriveRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> getUsers(Role role) {
        List<User> users = role == null
                ? userRepository.findAllByOrderByCreatedAtDesc()
                : userRepository.findByRoleOrderByCreatedAtDesc(role);
        return users.stream().map(UserAdminResponse::from).toList();
    }

    @Transactional
    public UserAdminResponse updateUserStatus(Long officerId, Long targetId, UserStatusRequest request) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found"));
        if (!request.enabled() && targetId.equals(officerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot disable your own account");
        }
        if (!request.enabled() && target.getRole() == Role.PLACEMENT_OFFICER
                && target.isEnabled()
                && userRepository.countByRoleAndEnabledTrueAndIdNot(Role.PLACEMENT_OFFICER, targetId) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one placement officer must remain enabled");
        }
        target.setEnabled(request.enabled());
        return UserAdminResponse.from(target);
    }

    @Transactional(readOnly = true)
    public List<JobReviewResponse> getJobs(JobStatus status) {
        List<Job> jobs = status == null
                ? jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.PENDING_APPROVAL)
                : jobRepository.findByStatusOrderByCreatedAtAsc(status);
        return jobs.stream().map(JobReviewResponse::from).toList();
    }

    @Transactional
    public JobReviewResponse reviewJob(Long officerId, Long jobId, boolean approve, JobReviewRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (job.getStatus() != JobStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending jobs can be reviewed");
        }
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Officer account was not found"));
        job.setStatus(approve ? JobStatus.APPROVED : JobStatus.REJECTED);
        job.setReviewedBy(officer);
        job.setReviewedAt(Instant.now());
        job.setReviewNotes(clean(request.notes()));

        // Notify Recruiter
        User recruiter = job.getCompany().getRecruiter().getUser();
        notificationService.createNotification(
                recruiter,
                "Job Posting " + (approve ? "Approved" : "Rejected"),
                "Your job posting '" + job.getTitle() + "' was " + (approve ? "approved" : "rejected") + " by the Placement Office." + (request.notes() != null && !request.notes().isBlank() ? " Notes: " + request.notes() : ""),
                "JOB"
        );

        return JobReviewResponse.from(job);
    }

    @Transactional(readOnly = true)
    public List<PlacementDriveResponse> getPlacementDrives() {
        return placementDriveRepository.findAllByOrderByStartDateAsc().stream()
                .map(PlacementDriveResponse::from)
                .toList();
    }

    @Transactional
    public PlacementDriveResponse createPlacementDrive(Long officerId, PlacementDriveRequest request) {
        User officer = requireOfficer(officerId);
        validateDriveDates(request);
        PlacementDrive drive = new PlacementDrive(officer, request.name().trim(), request.startDate(),
                request.endDate());
        applyDriveFields(drive, request);
        return PlacementDriveResponse.from(placementDriveRepository.save(drive));
    }

    @Transactional
    public PlacementDriveResponse updatePlacementDrive(Long officerId, Long driveId,
                                                        PlacementDriveRequest request) {
        requireOfficer(officerId);
        validateDriveDates(request);
        PlacementDrive drive = requireDrive(driveId);
        drive.setName(request.name().trim());
        drive.setStartDate(request.startDate());
        drive.setEndDate(request.endDate());
        applyDriveFields(drive, request);
        return PlacementDriveResponse.from(drive);
    }

    @Transactional
    public void deletePlacementDrive(Long officerId, Long driveId) {
        requireOfficer(officerId);
        PlacementDrive drive = requireDrive(driveId);
        if (jobRepository.existsByPlacementDriveId(driveId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Remove the drive from its jobs before deleting it");
        }
        placementDriveRepository.delete(drive);
    }

    @Transactional
    public JobReviewResponse assignJobToDrive(Long officerId, Long jobId, JobDriveAssignmentRequest request) {
        requireOfficer(officerId);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (request.placementDriveId() == null) {
            job.setPlacementDrive(null);
        } else {
            job.setPlacementDrive(requireDrive(request.placementDriveId()));
        }
        return JobReviewResponse.from(job);
    }

    @Transactional(readOnly = true)
    public PlacementReportResponse getPlacementReport() {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        Arrays.stream(ApplicationStatus.values())
                .forEach(status -> counts.put(status, jobApplicationRepository.countByStatus(status)));
        return new PlacementReportResponse(
                jobApplicationRepository.countByStatus(ApplicationStatus.SELECTED),
                counts,
                jobApplicationRepository.findPlacementCountsByCompany(ApplicationStatus.SELECTED)
        );
    }

    @Transactional(readOnly = true)
    public List<PlacementRecordResponse> getAllPlacements() {
        return placementRecordRepository.findAllWithDetails().stream()
                .map(PlacementRecordResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPlacementAnalytics() {
        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("totalOffers", placementRecordRepository.count());
        analytics.put("placedStudents", placementRecordRepository.countDistinctPlacedStudents());
        analytics.put("highestPackage", placementRecordRepository.findHighestPackage());
        analytics.put("averagePackage", placementRecordRepository.findAveragePackage());

        Map<String, Long> byCompany = new LinkedHashMap<>();
        for (Object[] row : placementRecordRepository.findPlacementsByCompany()) {
            byCompany.put((String) row[0], ((Number) row[1]).longValue());
        }
        analytics.put("byCompany", byCompany);

        Map<String, Long> byBranch = new LinkedHashMap<>();
        for (Object[] row : placementRecordRepository.findPlacementsByBranch()) {
            byBranch.put((String) row[0], ((Number) row[1]).longValue());
        }
        analytics.put("byBranch", byBranch);

        return analytics;
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getAllApplications() {
        return jobApplicationRepository.findAllWithDetails().stream()
                .map(JobApplicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getAllInterviews() {
        return interviewRepository.findAllByOrderByScheduledAtDesc().stream()
                .map(InterviewResponse::from)
                .toList();
    }

    private User requireOfficer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Officer account was not found"));
        if (user.getRole() != Role.PLACEMENT_OFFICER || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Placement officer access is required");
        }
        return user;
    }

    private PlacementDrive requireDrive(Long id) {
        return placementDriveRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Placement drive not found"));
    }

    private void validateDriveDates(PlacementDriveRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Drive end date must be on or after its start date");
        }
    }

    private void applyDriveFields(PlacementDrive drive, PlacementDriveRequest request) {
        drive.setDescription(clean(request.description()));
        drive.setVenue(clean(request.venue()));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
