package app.campushire.controller;

import app.campushire.entity.Company;
import app.campushire.entity.Job;
import app.campushire.entity.JobStatus;
import app.campushire.entity.RecruiterProfile;
import app.campushire.entity.User;
import app.campushire.exception.BadRequestException;
import app.campushire.exception.ResourceNotFoundException;
import app.campushire.repository.CompanyRepository;
import app.campushire.repository.JobRepository;
import app.campushire.repository.RecruiterProfileRepository;
import app.campushire.repository.UserRepository;
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
@RequestMapping("/api/recruiters")
public class RecruiterController {

    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;

    public RecruiterController(
        UserRepository userRepository,
        RecruiterProfileRepository recruiterProfileRepository,
        CompanyRepository companyRepository,
        JobRepository jobRepository
    ) {
        this.userRepository = userRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<RecruiterProfile> getMyProfile(Principal principal) {
        User user = getCurrentUser(principal);
        return ResponseEntity.ok(recruiterProfileRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found")));
    }

    @PostMapping("/profile")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<RecruiterProfile> createProfile(Principal principal, @Valid @RequestBody RecruiterProfile profile) {
        User user = getCurrentUser(principal);
        if (recruiterProfileRepository.findByUser(user).isPresent()) {
            throw new BadRequestException("Recruiter profile already exists");
        }
        profile.setUser(user);
        return ResponseEntity.ok(recruiterProfileRepository.save(profile));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<RecruiterProfile> updateProfile(Principal principal, @Valid @RequestBody RecruiterProfile profile) {
        User user = getCurrentUser(principal);
        RecruiterProfile existing = recruiterProfileRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));
        profile.setId(existing.getId());
        profile.setUser(user);
        return ResponseEntity.ok(recruiterProfileRepository.save(profile));
    }

    @PostMapping("/jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Job> createJob(Principal principal, @Valid @RequestBody Job job) {
        User recruiter = getCurrentUser(principal);
        if (job.getCompany() == null || job.getCompany().getId() == null) {
            throw new BadRequestException("Company is required");
        }
        Company company = companyRepository.findById(job.getCompany().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        job.setCompany(company);
        job.setRecruiter(recruiter);
        job.setStatus(JobStatus.PENDING);
        return ResponseEntity.ok(jobRepository.save(job));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<List<Job>> getJobs(Principal principal) {
        User recruiter = getCurrentUser(principal);
        return ResponseEntity.ok(jobRepository.findByRecruiter(recruiter));
    }

    @PutMapping("/jobs/{jobId}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Job> updateJobStatus(Principal principal, @PathVariable Long jobId, @RequestBody JobStatus status) {
        User recruiter = getCurrentUser(principal);
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new BadRequestException("You cannot update another recruiter job");
        }
        job.setStatus(status);
        return ResponseEntity.ok(jobRepository.save(job));
    }

    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
