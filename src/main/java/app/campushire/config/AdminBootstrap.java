package app.campushire.config;

import app.campushire.entity.Role;
import app.campushire.entity.User;
import app.campushire.repository.UserRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-email:admin@campushire.com}")
    private String adminEmail;

    @Value("${app.admin.default-password:Admin@123}")
    private String adminPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(normalizedEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(normalizedEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullName("CampusHire Admin");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        }
    }
}
