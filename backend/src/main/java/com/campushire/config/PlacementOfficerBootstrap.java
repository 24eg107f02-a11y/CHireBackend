package com.campushire.config;

import com.campushire.entity.Role;
import com.campushire.entity.User;
import com.campushire.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class PlacementOfficerBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public PlacementOfficerBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.officer-email:}") String email,
            @Value("${app.bootstrap.officer-password:}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Set both OFFICER_EMAIL and OFFICER_PASSWORD to provision the officer");
        }
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("OFFICER_PASSWORD must be at least 12 characters and at most 72 UTF-8 bytes");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresentOrElse(user -> {
            if (user.getRole() != Role.PLACEMENT_OFFICER) {
                throw new IllegalStateException("Officer bootstrap email is already assigned to a non-officer account");
            }
        }, () -> userRepository.save(new User(
                "Placement Officer",
                normalizedEmail,
                passwordEncoder.encode(password),
                Role.PLACEMENT_OFFICER
        )));
    }
}
