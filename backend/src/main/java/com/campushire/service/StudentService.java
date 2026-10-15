package com.campushire.service;

import com.campushire.dto.ApplicationStatusHistoryResponse;
import com.campushire.dto.InterviewResponse;
import com.campushire.dto.JobApplicationResponse;
import com.campushire.dto.JobRecommendationResponse;
import com.campushire.dto.JobResponse;
import com.campushire.dto.PlacementRecordResponse;
import com.campushire.dto.StudentProfileRequest;
import com.campushire.dto.StudentProfileResponse;
import com.campushire.entity.ApplicationStatus;
import com.campushire.entity.ApplicationStatusHistory;
import com.campushire.entity.Job;
import com.campushire.entity.JobApplication;
import com.campushire.entity.JobStatus;
import com.campushire.entity.StudentProfile;
import com.campushire.entity.User;
import com.campushire.repository.ApplicationStatusHistoryRepository;
import com.campushire.repository.InterviewRepository;
import com.campushire.repository.JobApplicationRepository;
import com.campushire.repository.JobRepository;
import com.campushire.repository.PlacementRecordRepository;
import com.campushire.repository.StudentProfileRepository;
import com.campushire.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final InterviewRepository interviewRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final PlacementRecordRepository placementRecordRepository;
    private final NotificationService notificationService;
    private final JobEligibilityService eligibilityService;

    public StudentService(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            JobRepository jobRepository,
            JobApplicationRepository jobApplicationRepository,
            InterviewRepository interviewRepository,
            ApplicationStatusHistoryRepository statusHistoryRepository,
            PlacementRecordRepository placementRecordRepository,
            NotificationService notificationService,
            JobEligibilityService eligibilityService
    ) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.interviewRepository = interviewRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.placementRecordRepository = placementRecordRepository;
        this.notificationService = notificationService;
        this.eligibilityService = eligibilityService;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(Long userId) {
        return StudentProfileResponse.from(requireStudentProfile(userId));
    }

    @Transactional
    public StudentProfileResponse updateProfile(Long userId, StudentProfileRequest request) {
        StudentProfile profile = requireStudentProfile(userId);
        if (request.fullName() != null) {
            if (request.fullName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name cannot be blank");
            }
            profile.getUser().setFullName(request.fullName().trim());
        }
        profile.setCollegeId(clean(request.collegeId()));
        profile.setPhone(clean(request.phone()));
        profile.setBranch(clean(request.branch()));
        profile.setQualification(clean(request.qualification()));
        profile.setCgpa(request.cgpa());
        profile.setGraduationYear(request.graduationYear());
        profile.setSkills(clean(request.skills()));
        profile.setInterests(clean(request.interests()));
        profile.setProjects(clean(request.projects()));
        profile.setResumeUrl(clean(request.resumeUrl()));
        return StudentProfileResponse.from(profile);
    }

    @Transactional
    public JobApplicationResponse apply(Long userId, Long jobId) {
        StudentProfile student = requireStudentProfile(userId);
        Job job = jobRepository.findOpenJobById(jobId, JobStatus.APPROVED, LocalDate.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Open job not found"));

        if (jobApplicationRepository.existsByJobIdAndStudentId(jobId, student.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied for this job");
        }

        var eligibility = eligibilityService.evaluate(student, job);
        if (!eligibility.eligible()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, String.join(" ", eligibility.reasons()));
        }

        try {
            JobApplication application = new JobApplication(job, student);
            application.setStatus(ApplicationStatus.APPLIED);
            application = jobApplicationRepository.saveAndFlush(application);

            // Record Initial Application Status History
            statusHistoryRepository.save(new ApplicationStatusHistory(
                    application,
                    null,
                    ApplicationStatus.APPLIED,
                    student.getUser(),
                    "Application submitted by candidate"
            ));

            // Notify Student
            notificationService.createNotification(
                    student.getUser(),
                    "Application Submitted",
                    "You applied for " + job.getTitle() + " at " + job.getCompany().getName() + ".",
                    "APPLICATION"
            );

            // Notify Recruiter
            User recruiterUser = job.getCompany().getRecruiter().getUser();
            notificationService.createNotification(
                    recruiterUser,
                    "New Application Received",
                    student.getUser().getFullName() + " applied for " + job.getTitle() + ".",
                    "APPLICATION"
            );

            return JobApplicationResponse.from(application);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied for this job", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getApplications(Long userId) {
        StudentProfile student = requireStudentProfile(userId);
        return jobApplicationRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream()
                .map(JobApplicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryResponse> getApplicationHistory(Long userId, Long applicationId) {
        StudentProfile student = requireStudentProfile(userId);
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        if (!application.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view this application");
        }
        return statusHistoryRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(ApplicationStatusHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviews(Long userId) {
        StudentProfile student = requireStudentProfile(userId);
        return interviewRepository.findByApplicationStudentIdOrderByScheduledAtAsc(student.getId())
                .stream()
                .map(InterviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlacementRecordResponse> getPlacements(Long userId) {
        return placementRecordRepository.findByUserId(userId).stream()
                .map(PlacementRecordResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobRecommendationResponse> getRecommendations(Long userId) {
        StudentProfile student = requireStudentProfile(userId);
        return jobRepository.findOpenJobs(JobStatus.APPROVED, LocalDate.now(), null, null, null, null)
                .stream()
                .map(job -> recommendationFor(student, job))
                .sorted(Comparator.comparingInt(JobRecommendationResponse::matchScore).reversed())
                .limit(8)
                .toList();
    }

    /**
     * Intelligent Job Eligibility and Recommendation Engine:
     * Calculates an explainable, deterministic weighted recommendation match score (0-100):
     * - Technical Skills Match (40%): Matched skills / Total required skills
     * - Branch Compatibility (20%): Matching branch or open to all branches
     * - Academic CGPA Fit (20%): Meets or exceeds minimum threshold
     * - Interests & Domain Fit (20%): Overlap between student interests and job keywords
     */
    private JobRecommendationResponse recommendationFor(StudentProfile student, Job job) {
        List<String> matchedSkills = eligibilityService.matchingSkills(student, job);
        List<String> missingSkills = eligibilityService.missingSkills(student, job);
        List<String> reasons = new ArrayList<>();
        int score = 0;

        // 1. Technical Skills Match (Weight: 40%)
        if (job.getRequiredSkills() == null || job.getRequiredSkills().isEmpty()) {
            score += 40;
            reasons.add("No specific skills are required.");
        } else {
            int totalRequired = job.getRequiredSkills().size();
            int matchedCount = matchedSkills.size();
            int skillScore = Math.round(40.0f * matchedCount / totalRequired);
            score += skillScore;
            if (matchedCount == totalRequired) {
                reasons.add("Matching skills: " + String.join(", ", matchedSkills) + ".");
            } else if (matchedCount > 0) {
                reasons.add("Matching skills: " + String.join(", ", matchedSkills) + ".");
                reasons.add("More matching skills improve your fit.");
            } else {
                reasons.add("Missing required skills: " + String.join(", ", missingSkills) + ".");
            }
        }

        // 2. Branch Compatibility (Weight: 30%)
        String eligibleBranch = job.getEligibleBranch();
        String studentBranch = student.getBranch();
        if (eligibleBranch == null || eligibleBranch.isBlank() || eligibleBranch.equalsIgnoreCase("All") || eligibleBranch.equalsIgnoreCase("Any")) {
            score += 30;
            reasons.add("Open to all branches.");
        } else if (studentBranch != null && eligibleBranch.trim().equalsIgnoreCase(studentBranch.trim())) {
            score += 30;
            reasons.add("Your branch matches the role.");
        } else if (studentBranch == null || studentBranch.isBlank()) {
            reasons.add("Add your branch to your profile to personalize this recommendation.");
        } else {
            reasons.add("Role designated for " + eligibleBranch + " branch.");
        }

        // 3. Academic CGPA Compatibility (Weight: 30%)
        BigDecimal minCgpa = job.getMinimumCgpa();
        BigDecimal studentCgpa = student.getCgpa();
        if (minCgpa == null) {
            score += 30;
            reasons.add("No minimum CGPA requirement.");
        } else if (studentCgpa != null) {
            if (studentCgpa.compareTo(minCgpa) >= 0) {
                score += 30;
                reasons.add("Your CGPA meets the minimum requirement.");
            } else {
                int scaled = Math.max(0, Math.round(studentCgpa.divide(minCgpa, 2, RoundingMode.HALF_UP).floatValue() * 18.0f));
                score += scaled;
                reasons.add("Your CGPA is below the minimum requirement.");
            }
        } else {
            reasons.add("Add your CGPA to your profile to personalize this recommendation.");
        }

        // 4. Interests Alignment
        String interests = student.getInterests();
        if (interests != null && !interests.isBlank()) {
            Set<String> interestTokens = Arrays.stream(interests.split("[,;\\s]+"))
                    .map(String::trim)
                    .filter(s -> s.length() > 2)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            String jobText = (job.getTitle() + " " + job.getDescription()).toLowerCase(Locale.ROOT);
            long matchingInterests = interestTokens.stream().filter(jobText::contains).count();
            if (matchingInterests > 0) {
                reasons.add("Role keywords align with your career interests.");
            }
        }

        var eligibility = eligibilityService.evaluate(student, job);
        if (reasons.isEmpty()) {
            reasons.add("Review the role requirements and complete your profile for a more personalized match.");
        }

        return new JobRecommendationResponse(
                JobResponse.from(job),
                eligibility.eligible(),
                Math.min(100, score),
                matchedSkills,
                missingSkills,
                reasons,
                eligibility.reasons()
        );
    }

    private StudentProfile requireStudentProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student account was not found"));
        return studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile was not found"));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
