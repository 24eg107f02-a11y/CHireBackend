package com.campushire.service;

import com.campushire.dto.AuthResponse;
import com.campushire.dto.LoginRequest;
import com.campushire.dto.RegisterRequest;
import com.campushire.dto.UserResponse;
import com.campushire.entity.RecruiterProfile;
import com.campushire.entity.Role;
import com.campushire.entity.StudentProfile;
import com.campushire.entity.User;
import com.campushire.repository.RecruiterProfileRepository;
import com.campushire.repository.StudentProfileRepository;
import com.campushire.repository.UserRepository;
import com.campushire.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            RecruiterProfileRepository recruiterProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.PLACEMENT_OFFICER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Placement officer accounts can only be provisioned by an administrator"
            );
        }
        if (request.role() != Role.STUDENT && request.role() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a supported registration role");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = userRepository.save(new User(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.role()
        ));
        if (user.getRole() == Role.STUDENT) {
            studentProfileRepository.save(new StudentProfile(user));
        } else {
            recruiterProfileRepository.save(new RecruiterProfile(user));
        }
        return createResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return createResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found"));
        return UserResponse.from(user);
    }

    private AuthResponse createResponse(User user) {
        return AuthResponse.from(user, jwtService.createToken(user), jwtService.getExpirationMillis());
    }
}
