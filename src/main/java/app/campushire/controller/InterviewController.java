package app.campushire.controller;

import app.campushire.entity.Application;
import app.campushire.entity.Interview;
import app.campushire.entity.InterviewStatus;
import app.campushire.entity.User;
import app.campushire.exception.BadRequestException;
import app.campushire.exception.ResourceNotFoundException;
import app.campushire.repository.ApplicationRepository;
import app.campushire.repository.InterviewRepository;
import app.campushire.repository.UserRepository;
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
public class InterviewController {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public InterviewController(
        InterviewRepository interviewRepository,
        ApplicationRepository applicationRepository,
        UserRepository userRepository
    ) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/applications/{applicationId}/interviews")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<Interview> scheduleInterview(@PathVariable Long applicationId, @RequestBody Interview interview, Principal principal) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        User interviewer = getCurrentUser(principal);
        interview.setApplication(application);
        interview.setInterviewer(interviewer);
        if (interview.getStatus() == null) {
            interview.setStatus(InterviewStatus.SCHEDULED);
        }
        return ResponseEntity.ok(interviewRepository.save(interview));
    }

    @GetMapping("/applications/{applicationId}/interviews")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<List<Interview>> getInterviews(@PathVariable Long applicationId) {
        return ResponseEntity.ok(interviewRepository.findByApplicationId(applicationId));
    }

    @PutMapping("/interviews/{interviewId}/status")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<Interview> updateInterviewStatus(@PathVariable Long interviewId, @RequestBody InterviewStatus status) {
        Interview interview = interviewRepository.findById(interviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        interview.setStatus(status);
        return ResponseEntity.ok(interviewRepository.save(interview));
    }

    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
