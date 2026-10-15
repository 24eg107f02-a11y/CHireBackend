package com.campushire.service;

import com.campushire.dto.ApplicationStatusRequest;
import com.campushire.dto.CompanyRequest;
import com.campushire.dto.CompanyResponse;
import com.campushire.dto.InterviewRequest;
import com.campushire.dto.InterviewResponse;
import com.campushire.dto.JobApplicationResponse;
import com.campushire.dto.JobRequest;
import com.campushire.dto.JobResponse;
import com.campushire.dto.RecruiterApplicantResponse;
import com.campushire.entity.ApplicationStatus;
import com.campushire.entity.ApplicationStatusHistory;
import com.campushire.entity.Company;
import com.campushire.entity.Interview;
import com.campushire.entity.Job;
import com.campushire.entity.JobApplication;
import com.campushire.entity.JobStatus;
import com.campushire.entity.PlacementRecord;
import com.campushire.entity.RecruiterProfile;
import com.campushire.entity.Role;
import com.campushire.entity.User;
import com.campushire.repository.ApplicationStatusHistoryRepository;
import com.campushire.repository.CompanyRepository;
import com.campushire.repository.InterviewRepository;
import com.campushire.repository.JobApplicationRepository;
import com.campushire.repository.JobRepository;
import com.campushire.repository.PlacementRecordRepository;
import com.campushire.repository.RecruiterProfileRepository;
import com.campushire.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecruiterService {

    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final InterviewRepository interviewRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final PlacementRecordRepository placementRecordRepository;
    private final NotificationService notificationService;

    public RecruiterService(
            UserRepository userRepository,
            RecruiterProfileRepository recruiterProfileRepository,
            CompanyRepository companyRepository,
            JobRepository jobRepository,
            JobApplicationRepository jobApplicationRepository,
            InterviewRepository interviewRepository,
            ApplicationStatusHistoryRepository statusHistoryRepository,
            PlacementRecordRepository placementRecordRepository,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.interviewRepository = interviewRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.placementRecordRepository = placementRecordRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getCompanies(Long userId) {
        requireRecruiter(userId);
        return companyRepository.findAllForRecruiter(userId).stream()
                .map(CompanyResponse::from)
                .toList();
    }

    @Transactional
    public CompanyResponse createCompany(Long userId, CompanyRequest request) {
        RecruiterProfile recruiter = requireRecruiter(userId);
        Company company = new Company(recruiter, request.name().trim());
        applyCompanyFields(company, request);
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse updateCompany(Long userId, Long companyId, CompanyRequest request) {
        Company company = requireOwnedCompany(userId, companyId);
        company.setName(request.name().trim());
        applyCompanyFields(company, request);
        return CompanyResponse.from(company);
    }

    @Transactional
    public void deleteCompany(Long userId, Long companyId) {
        Company company = requireOwnedCompany(userId, companyId);
        if (jobRepository.existsByCompanyId(companyId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A company with job postings cannot be deleted; close or remove its jobs first"
            );
        }
        companyRepository.delete(company);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getJobs(Long userId) {
        requireRecruiter(userId);
        return jobRepository.findAllForRecruiter(userId).stream()
                .map(JobResponse::from)
                .toList();
    }

    @Transactional
    public JobResponse createJob(Long userId, JobRequest request) {
        Company company = requireOwnedCompany(userId, request.companyId());
        Job job = new Job(company, request.title().trim(), request.description().trim(),
                request.applicationDeadline());
        updateJobFields(job, request);
        job.setStatus(JobStatus.PENDING_APPROVAL);
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public JobResponse updateJob(Long userId, Long jobId, JobRequest request) {
        Job job = requireOwnedJob(userId, jobId);
        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closed jobs cannot be edited");
        }
        Company company = requireOwnedCompany(userId, request.companyId());
        if (job.getStatus() == JobStatus.APPROVED) {
            job.setStatus(JobStatus.PENDING_APPROVAL);
        }
        job.setCompany(company);
        updateJobFields(job, request);
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse closeJob(Long userId, Long jobId) {
        Job job = requireOwnedJob(userId, jobId);
        if (job.getStatus() == JobStatus.CLOSED) {
            return JobResponse.from(job);
        }
        job.setStatus(JobStatus.CLOSED);
        return JobResponse.from(job);
    }

    @Transactional
    public void deleteJob(Long userId, Long jobId) {
        Job job = requireOwnedJob(userId, jobId);
        if (jobApplicationRepository.existsByJobId(jobId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A job with applications cannot be deleted; close the job instead"
            );
        }
        jobRepository.delete(job);
    }

    @Transactional(readOnly = true)
    public List<RecruiterApplicantResponse> getApplicants(Long userId, Long jobId) {
        requireOwnedJob(userId, jobId);
        return jobApplicationRepository.findApplicantsForOwnedJob(jobId, userId).stream()
                .map(RecruiterApplicantResponse::from)
                .toList();
    }

    @Transactional
    public JobApplicationResponse updateApplicationStatus(
            Long userId,
            Long applicationId,
            ApplicationStatusRequest request
    ) {
        User recruiter = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter not found"));
        JobApplication application = requireOwnedApplication(userId, applicationId);
        ApplicationStatus current = application.getStatus();
        ApplicationStatus target = request.status();

        if (target == null || target == ApplicationStatus.APPLIED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target status");
        }

        // Validate state transitions according to lifecycle:
        // APPLIED -> UNDER_REVIEW, SHORTLISTED, REJECTED
        // UNDER_REVIEW -> SHORTLISTED, REJECTED
        // SHORTLISTED -> INTERVIEW_SCHEDULED, SELECTED, REJECTED
        // INTERVIEW_SCHEDULED -> SELECTED, REJECTED
        // SELECTED, REJECTED -> terminal states
        boolean allowed = switch (current) {
            case APPLIED -> target == ApplicationStatus.UNDER_REVIEW
                    || target == ApplicationStatus.SHORTLISTED
                    || target == ApplicationStatus.REJECTED;
            case UNDER_REVIEW -> target == ApplicationStatus.SHORTLISTED
                    || target == ApplicationStatus.REJECTED;
            case SHORTLISTED -> target == ApplicationStatus.INTERVIEW_SCHEDULED
                    || target == ApplicationStatus.SELECTED
                    || target == ApplicationStatus.REJECTED;
            case INTERVIEW_SCHEDULED -> target == ApplicationStatus.SELECTED
                    || target == ApplicationStatus.REJECTED;
            case REJECTED, SELECTED -> false;
        };

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Application status cannot transition from " + current + " to " + target
            );
        }

        application.setStatus(target);
        application.setRecruiterNotes(clean(request.recruiterNotes()));

        // Record Status History
        statusHistoryRepository.save(new ApplicationStatusHistory(
                application,
                current,
                target,
                recruiter,
                request.recruiterNotes() != null && !request.recruiterNotes().isBlank()
                        ? request.recruiterNotes().trim()
                        : "Status changed to " + target
        ));

        // When a candidate is SELECTED, automatically create/update Placement Record
        if (target == ApplicationStatus.SELECTED) {
            var student = application.getStudent();
            var job = application.getJob();
            var company = job.getCompany();

            placementRecordRepository.findByStudentIdAndJobId(student.getId(), job.getId())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setPackageCtc(job.getSalary());
                                existing.setSelectionDate(LocalDate.now());
                                existing.setPlacementStatus("OFFERED");
                            },
                            () -> {
                                PlacementRecord record = new PlacementRecord(
                                        student,
                                        company,
                                        job,
                                        job.getSalary(),
                                        LocalDate.now()
                                );
                                record.setPlacementStatus("OFFERED");
                                placementRecordRepository.save(record);
                            }
                    );
        }

        // Notify Candidate
        notificationService.createNotification(
                application.getStudent().getUser(),
                "Application " + target,
                "Your application for " + application.getJob().getTitle() + " at " + application.getJob().getCompany().getName() + " is now " + target + ".",
                target == ApplicationStatus.SELECTED ? "PLACEMENT" : "APPLICATION"
        );

        return JobApplicationResponse.from(application);
    }

    @Transactional
    public InterviewResponse scheduleInterview(
            Long userId,
            Long applicationId,
            InterviewRequest request
    ) {
        User recruiter = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter account was not found"));
        JobApplication application = requireOwnedApplication(userId, applicationId);

        if (application.getStatus() != ApplicationStatus.SHORTLISTED
                && application.getStatus() != ApplicationStatus.INTERVIEW_SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Candidate must be SHORTLISTED before scheduling an interview");
        }

        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);

        Interview interview = new Interview(application, recruiter, request.scheduledAt());
        interview.setInterviewType(clean(request.interviewType() != null ? request.interviewType() : "TECHNICAL"));
        interview.setInterviewMode(clean(request.interviewMode() != null ? request.interviewMode() : "ONLINE"));
        interview.setRound(clean(request.round() != null ? request.round() : "Interview Round"));
        interview.setLocationOrMeetingUrl(clean(request.locationOrMeetingUrl()));
        interview.setStatus(clean(request.status() != null ? request.status() : "SCHEDULED"));
        interview.setNotes(clean(request.notes()));

        Interview saved = interviewRepository.save(interview);

        // Record Status History
        statusHistoryRepository.save(new ApplicationStatusHistory(
                application,
                previousStatus,
                ApplicationStatus.INTERVIEW_SCHEDULED,
                recruiter,
                "Interview scheduled: " + interview.getRound() + " at " + interview.getScheduledAt()
        ));

        // Notify Candidate
        notificationService.createNotification(
                application.getStudent().getUser(),
                "Interview Scheduled",
                interview.getRound() + " scheduled for " + application.getJob().getTitle() + " at " + application.getJob().getCompany().getName() + " on " + request.scheduledAt() + ".",
                "INTERVIEW"
        );

        return InterviewResponse.from(saved);
    }

    private RecruiterProfile requireRecruiter(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter account was not found"));
        if (user.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recruiter access is required");
        }
        return recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter profile was not found"));
    }

    private Company requireOwnedCompany(Long userId, Long companyId) {
        requireRecruiter(userId);
        return companyRepository.findOwnedCompany(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private Job requireOwnedJob(Long userId, Long jobId) {
        requireRecruiter(userId);
        return jobRepository.findOwnedJob(jobId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    private JobApplication requireOwnedApplication(Long userId, Long applicationId) {
        requireRecruiter(userId);
        return jobApplicationRepository.findOwnedApplication(applicationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private void applyCompanyFields(Company company, CompanyRequest request) {
        company.setDescription(clean(request.description()));
        company.setWebsite(clean(request.website()));
        company.setHeadquarters(clean(request.headquarters()));
    }

    private void updateJobFields(Job job, JobRequest request) {
        job.setTitle(request.title().trim());
        job.setDescription(request.description().trim());
        job.setLocation(clean(request.location()));
        job.setEmploymentType(clean(request.employmentType()));
        job.setSalary(request.salary());
        job.setEligibleBranch(clean(request.eligibleBranch()));
        job.setQualification(clean(request.qualification()));
        job.setMinimumCgpa(request.minimumCgpa());
        job.setRequiredSkills(request.requiredSkills());
        job.setApplicationDeadline(request.applicationDeadline());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
