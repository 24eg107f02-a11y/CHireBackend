package app.campushire.controller;

import app.campushire.entity.Job;
import app.campushire.entity.JobStatus;
import app.campushire.entity.User;
import app.campushire.exception.BadRequestException;
import app.campushire.exception.ResourceNotFoundException;
import app.campushire.repository.JobRepository;
import app.campushire.repository.UserRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public AdminController(JobRepository jobRepository, UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/jobs/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Job>> getPendingJobs() {
        return ResponseEntity.ok(jobRepository.findByStatus(JobStatus.PENDING));
    }

    @PutMapping("/jobs/{jobId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Job> approveJob(@PathVariable Long jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        job.setStatus(JobStatus.APPROVED);
        return ResponseEntity.ok(jobRepository.save(job));
    }

    @PutMapping("/jobs/{jobId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Job> rejectJob(@PathVariable Long jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        job.setStatus(JobStatus.REJECTED);
        return ResponseEntity.ok(jobRepository.save(job));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/users/{userId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> enableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        return ResponseEntity.ok("User enabled");
    }

    @PutMapping("/users/{userId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> disableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == app.campushire.entity.Role.ADMIN) {
            throw new BadRequestException("Cannot disable an admin user");
        }
        user.setEnabled(false);
        userRepository.save(user);
        return ResponseEntity.ok("User disabled");
    }
}
