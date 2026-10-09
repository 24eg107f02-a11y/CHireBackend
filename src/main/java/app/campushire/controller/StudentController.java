package app.campushire.controller;

import app.campushire.entity.StudentProfile;
import app.campushire.entity.User;
import app.campushire.exception.BadRequestException;
import app.campushire.exception.ResourceNotFoundException;
import app.campushire.repository.StudentProfileRepository;
import app.campushire.repository.UserRepository;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    public StudentController(UserRepository userRepository, StudentProfileRepository studentProfileRepository) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentProfile> getMyProfile(Principal principal) {
        User user = getCurrentUser(principal);
        StudentProfile profile = studentProfileRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentProfile> createProfile(Principal principal, @Valid @RequestBody StudentProfile profile) {
        User user = getCurrentUser(principal);
        if (studentProfileRepository.findByUser(user).isPresent()) {
            throw new BadRequestException("Student profile already exists");
        }
        profile.setUser(user);
        return ResponseEntity.ok(studentProfileRepository.save(profile));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentProfile> updateProfile(Principal principal, @Valid @RequestBody StudentProfile profile) {
        User user = getCurrentUser(principal);
        StudentProfile existing = studentProfileRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        profile.setId(existing.getId());
        profile.setUser(user);
        return ResponseEntity.ok(studentProfileRepository.save(profile));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StudentProfile>> getAllProfiles() {
        return ResponseEntity.ok(studentProfileRepository.findAll());
    }

    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
