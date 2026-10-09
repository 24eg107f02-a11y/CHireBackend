package app.campushire.controller;

import app.campushire.entity.Application;
import app.campushire.entity.ApplicationStatus;
import app.campushire.entity.Job;
import app.campushire.entity.StudentProfile;
import app.campushire.entity.User;
import app.campushire.exception.BadRequestException;
import app.campushire.exception.ResourceNotFoundException;
import app.campushire.repository.ApplicationRepository;
import app.campushire.repository.JobRepository;
import app.campushire.repository.StudentProfileRepository;
import app.campushire.repository.UserRepository;
import app.campushire.service.EligibilityEngine;
import app.campushire.service.RecommendationEngine;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final EligibilityEngine eligibilityEngine;
    private final RecommendationEngine recommendationEngine;

    public ApplicationController(
        ApplicationRepository applicationRepository,
        JobRepository jobRepository,
        UserRepository userRepository,
        StudentProfileRepository studentProfileRepository,
        EligibilityEngine eligibilityEngine,
        RecommendationEngine recommendationEngine
    ) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.eligibilityEngine = eligibilityEngine;
        this.recommendationEngine = recommendationEngine;
    }

    @PostMapping("/students/jobs/{jobId}/apply")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Application> applyToJob(Principal principal, @PathVariable Long jobId, @Valid @RequestBody Application application) {
        User student = getCurrentUser(principal);
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getStatus().equals(app.campushire.entity.JobStatus.APPROVED)) {
            throw new BadRequestException("Job is not approved yet");
        }

        if (applicationRepository.findByStudentAndJob(student, job).isPresent()) {
            throw new BadRequestException("Student already applied to this job");
        }

        StudentProfile profile = studentProfileRepository.findByUser(student)
            .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        if (!eligibilityEngine.isEligible(profile, job)) {
            throw new BadRequestException("Student is not eligible for this job: " + eligibilityEngine.getEligibilitySummary(profile, job));
        }

        application.setStudent(student);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
        return ResponseEntity.ok(applicationRepository.save(application));
    }

    @GetMapping("/students/me/applications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Application>> getMyApplications(Principal principal) {
        User student = getCurrentUser(principal);
        return ResponseEntity.ok(applicationRepository.findByStudentId(student.getId()));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getApprovedJobs() {
        return ResponseEntity.ok(jobRepository.findByStatus(app.campushire.entity.JobStatus.APPROVED));
    }

    @GetMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<List<Application>> getJobApplications(@PathVariable Long jobId) {
        return ResponseEntity.ok(applicationRepository.findByJobId(jobId));
    }

    @GetMapping("/students/{studentId}/recommendations")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<List<RecommendationEngine.RecommendationResult>> getRecommendations(@PathVariable Long studentId) {
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        StudentProfile profile = studentProfileRepository.findByUser(student)
            .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        List<Job> jobs = jobRepository.findByStatus(app.campushire.entity.JobStatus.APPROVED);
        List<RecommendationEngine.RecommendationResult> results = new java.util.ArrayList<>();
        for (Job job : jobs) {
            results.add(recommendationEngine.score(profile, job));
        }
        return ResponseEntity.ok(results);
    }

    @PutMapping("/applications/{applicationId}/status")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<Application> updateStatus(@PathVariable Long applicationId, @RequestBody ApplicationStatus status) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        application.setStatus(status);
        return ResponseEntity.ok(applicationRepository.save(application));
    }

    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
